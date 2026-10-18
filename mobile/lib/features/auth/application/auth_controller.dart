import 'package:flutter/foundation.dart';

import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/features/auth/domain/auth_state.dart';
import 'package:mtaafix_mobile/features/auth/domain/user.dart';

/// Owns the authentication lifecycle: session restore, login, register and
/// logout. Listeners (guards, screens) rebuild via [ChangeNotifier].
class AuthController extends ChangeNotifier {
  AuthController(this._api);

  final ApiClient _api;

  AuthState _state = const AuthState();

  /// The authenticated HTTP client (screens build feature APIs from it).
  ApiClient get api => _api;

  AuthState get state => _state;

  bool get isAuthenticated => _state.status == AuthStatus.authenticated;

  bool get isLoading => _state.status == AuthStatus.loading;

  String? get errorMessage => _state.message;

  User? get user => _state.user;

  /// Restores a persisted session: load tokens, then `GET /auth/me`.
  ///
  /// Network failures keep the stored tokens (the user is shown the
  /// connectivity error on the sign-in surface instead of being signed out);
  /// rejected credentials clear the session.
  Future<void> init() async {
    try {
      await _api.init();
      if (!_api.hasSession) {
        _emit(const AuthState(status: AuthStatus.unauthenticated));
        return;
      }
      final me = await _api.get('/auth/me');
      _emit(
        AuthState(status: AuthStatus.authenticated, user: User.fromJson(me)),
      );
    } on ApiException catch (e) {
      if (e.isNetwork) {
        _emit(
          AuthState(
            status: AuthStatus.unauthenticated,
            message: e.userMessage,
          ),
        );
      } else {
        await _api.clearSession();
        _emit(const AuthState(status: AuthStatus.unauthenticated));
      }
    }
  }

  Future<bool> login(String email, String password) async {
    _emit(const AuthState(status: AuthStatus.loading));
    try {
      final response = await _api.post('/auth/login', {
        'email': email,
        'password': password,
      });
      final access = response['accessToken'] as String?;
      final refresh = response['refreshToken'] as String?;
      if (access == null || refresh == null) {
        throw const ApiException(
          statusCode: 500,
          code: 'INVALID_RESPONSE',
          message: 'The sign-in response was incomplete. Please try again.',
        );
      }
      await _api.setSession(accessToken: access, refreshToken: refresh);
      final userJson = response['user'];
      final user = userJson is Map<String, dynamic>
          ? User.fromJson(userJson)
          : User.fromJson(await _api.get('/auth/me'));
      _emit(AuthState(status: AuthStatus.authenticated, user: user));
      return true;
    } on ApiException catch (e) {
      _emit(
        AuthState(status: AuthStatus.error, message: e.userMessage),
      );
      return false;
    }
  }

  Future<bool> register({
    required String fullName,
    required String email,
    required String password,
    String? mobile,
  }) async {
    _emit(const AuthState(status: AuthStatus.loading));
    try {
      await _api.post('/auth/register', {
        'fullName': fullName,
        'email': email,
        'password': password,
        if (mobile != null && mobile.isNotEmpty) 'mobile': mobile,
      });
      // Registration succeeded — establish the session immediately.
      return await login(email, password);
    } on ApiException catch (e) {
      _emit(AuthState(status: AuthStatus.error, message: e.userMessage));
      return false;
    }
  }

  Future<void> logout() async {
    await _api.logout();
    _emit(const AuthState(status: AuthStatus.unauthenticated));
  }

  /// Updates the signed-in user's profile (full name / mobile only, FR-1.x)
  /// and refreshes the in-memory session user.
  Future<bool> updateProfile({String? fullName, String? mobile}) async {
    try {
      final json = await _api.put('/auth/me', {
        if (fullName != null) 'fullName': fullName,
        if (mobile != null) 'mobile': mobile,
      });
      _emit(AuthState(status: AuthStatus.authenticated, user: User.fromJson(json)));
      return true;
    } on ApiException catch (e) {
      _emit(
        AuthState(
          status: AuthStatus.authenticated,
          user: _state.user,
          message: e.userMessage,
        ),
      );
      return false;
    }
  }

  void _emit(AuthState next) {
    _state = next;
    notifyListeners();
  }
}
