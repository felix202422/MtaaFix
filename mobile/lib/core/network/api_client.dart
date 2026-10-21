import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';

/// Thin JSON client for the MtaaFix backend (`/api/v1`).
///
/// Configuration:
/// * Base URL defaults to the Android emulator loopback and can be overridden
///   at build/run time:
///   `flutter run --dart-define=MTAAFIX_API_BASE=http://10.0.2.2:8080/api/v1`
///
/// Behaviour:
/// * Loads the persisted token pair on [init] and attaches
///   `Authorization: Bearer` to every authenticated request.
/// * On HTTP 401 with a refresh token it performs **one** refresh-and-retry
///   (the backend rotates refresh tokens; the new pair is persisted).
/// * Every failure surfaces as [ApiException]; `statusCode == 0` means the
///   server was unreachable.
class ApiClient {
  ApiClient({
    String? baseUrl,
    http.Client? client,
    TokenStore? tokenStore,
    this.requestTimeout = const Duration(seconds: 15),
  })  : baseUrl = baseUrl ??
            const String.fromEnvironment(
              'MTAAFIX_API_BASE',
              defaultValue: 'http://10.0.2.2:8080/api/v1',
            ),
        _http = client ?? http.Client(),
        _tokens = tokenStore ?? const SecureTokenStore();

  final String baseUrl;
  final http.Client _http;
  final TokenStore _tokens;
  final Duration requestTimeout;

  String? _accessToken;
  String? _refreshToken;

  bool get hasSession => _accessToken != null || _refreshToken != null;

  String? get accessToken => _accessToken;

  /// Reads persisted tokens into memory. Call once during app bootstrap.
  Future<void> init() async {
    _accessToken = await _tokens.readAccessToken();
    _refreshToken = await _tokens.readRefreshToken();
  }

  Future<void> setSession({
    required String accessToken,
    required String refreshToken,
  }) async {
    _accessToken = accessToken;
    _refreshToken = refreshToken;
    await _tokens.writeTokens(
      accessToken: accessToken,
      refreshToken: refreshToken,
    );
  }

  Future<void> clearSession() async {
    _accessToken = null;
    _refreshToken = null;
    await _tokens.clear();
  }

  Future<Map<String, dynamic>> get(String path) => _send('GET', path);

  Future<Map<String, dynamic>> post(String path,
          [Object? body, String? idempotencyKey]) =>
      _send('POST', path, body: body, idempotencyKey: idempotencyKey);

  Future<Map<String, dynamic>> put(String path, [Object? body]) =>
      _send('PUT', path, body: body);

  Future<Map<String, dynamic>> delete(String path) => _send('DELETE', path);

  /// Best-effort server-side logout (POST `/auth/logout` with the refresh
  /// token) followed by local session removal. Never throws — a failed network
  /// call must not trap the user in a signed-in state.
  Future<void> logout() async {
    final refresh = _refreshToken;
    if (refresh != null) {
      try {
        await _send(
          'POST',
          '/auth/logout',
          body: {'refreshToken': refresh},
          allowRefresh: false,
        );
      } on ApiException {
        // Ignore — local sign-out proceeds regardless.
      }
    }
    await clearSession();
  }

  /// Multipart upload used for report evidence photos.
  ///
  /// A 401 here is surfaced to the caller (re-uploading after a silent refresh
  /// is handled by the caller re-triggering the action).
  Future<Map<String, dynamic>> uploadMultipart(
    String path, {
    required String filePath,
    String field = 'file',
    Map<String, String>? fields,
  }) async {
    try {
      final request = http.MultipartRequest('POST', Uri.parse('$baseUrl$path'))
        ..headers.addAll(await _authHeaders());
      if (fields != null) {
        request.fields.addAll(fields);
      }
      request.files.add(await http.MultipartFile.fromPath(field, filePath));
      final streamed = await _http.send(request).timeout(requestTimeout);
      final response = await http.Response.fromStream(streamed);
      return _decode(response);
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        statusCode: 0,
        code: 'NETWORK_ERROR',
        message: 'The upload timed out. Check your connection and try again.',
      );
    } catch (e) {
      throw ApiException(
        statusCode: 0,
        code: 'NETWORK_ERROR',
        message: 'Upload failed: $e',
      );
    }
  }

  Future<Map<String, dynamic>> _send(
    String method,
    String path, {
    Object? body,
    bool allowRefresh = true,
    String? idempotencyKey,
  }) async {
    final response =
        await _execute(method, path, body: body, idempotencyKey: idempotencyKey);
    if (response.statusCode == 401 && allowRefresh && _refreshToken != null) {
      if (await _tryRefresh()) {
        return _send(method, path,
            body: body, allowRefresh: false, idempotencyKey: idempotencyKey);
      }
    }
    return _decode(response);
  }

  Future<http.Response> _execute(
    String method,
    String path, {
    Object? body,
    String? idempotencyKey,
  }) async {
    final headers = <String, String>{
      'Accept': 'application/json',
      if (body != null) 'Content-Type': 'application/json',
      if (idempotencyKey != null && idempotencyKey.isNotEmpty)
        'Idempotency-Key': idempotencyKey,
      ...await _authHeaders(),
    };
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers.addAll(headers);
    if (body != null) {
      request.body = jsonEncode(body);
    }
    try {
      final streamed = await _http.send(request).timeout(requestTimeout);
      return await http.Response.fromStream(streamed);
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        statusCode: 0,
        code: 'NETWORK_ERROR',
        message: 'The request timed out. Check your connection and try again.',
      );
    } catch (_) {
      throw const ApiException(
        statusCode: 0,
        code: 'NETWORK_ERROR',
        message: 'Could not reach the server. Check your connection.',
      );
    }
  }

  Future<Map<String, String>> _authHeaders() async {
    final token = _accessToken;
    if (token == null) return const {};
    return {'Authorization': 'Bearer $token'};
  }

  /// Attempts a single refresh-token exchange. Returns `true` when a new
  /// token pair was obtained. A rejected refresh clears the session; a network
  /// failure during refresh keeps the tokens for a later retry.
  Future<bool> _tryRefresh() async {
    final refresh = _refreshToken;
    if (refresh == null) return false;
    try {
      final response = await _execute(
        'POST',
        '/auth/refresh',
        body: {'refreshToken': refresh},
      );
      if (response.statusCode >= 200 && response.statusCode < 300) {
        final data = _tryDecodeMap(response.body);
        final access = data?['accessToken'] as String?;
        final rotated = data?['refreshToken'] as String?;
        if (access != null && rotated != null) {
          await setSession(accessToken: access, refreshToken: rotated);
          return true;
        }
      }
      await clearSession();
      return false;
    } on ApiException catch (e) {
      if (e.isNetwork) return false;
      await clearSession();
      return false;
    } catch (_) {
      return false;
    }
  }

  Map<String, dynamic> _decode(http.Response response) {
    final status = response.statusCode;
    if (status >= 200 && status < 300) {
      final data = _tryDecodeMap(response.body);
      if (data != null) return data;
      // Top-level JSON arrays (e.g. GET /categories/public) are wrapped so
      // every caller receives a map; list consumers read ['content'].
      final list = _tryDecodeList(response.body);
      if (list != null) return {'content': list};
      return <String, dynamic>{};
    }
    final data = _tryDecodeMap(response.body);
    throw ApiException(
      statusCode: status,
      code: data?['code'] as String?,
      message: (data?['message'] as String?) ?? _fallbackMessage(status),
    );
  }

  Map<String, dynamic>? _tryDecodeMap(String body) {
    if (body.trim().isEmpty) return null;
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) return decoded;
      return null;
    } catch (_) {
      return null;
    }
  }

  List<dynamic>? _tryDecodeList(String body) {
    if (body.trim().isEmpty) return null;
    try {
      final decoded = jsonDecode(body);
      if (decoded is List) return decoded;
      return null;
    } catch (_) {
      return null;
    }
  }

  String _fallbackMessage(int status) {
    if (status == 401) return 'Your session has expired. Please sign in again.';
    if (status == 403) return 'You do not have permission to do that.';
    if (status == 404) return 'The requested resource was not found.';
    if (status >= 500) return 'The server had a problem. Please try again.';
    return 'Request failed (HTTP $status).';
  }
}
