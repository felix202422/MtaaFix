/// Typed failure raised by [ApiClient] for every non-successful outcome.
///
/// * `statusCode == 0` — the server could not be reached (network/timeout).
/// * Otherwise mirrors the HTTP status with the backend's `code` and `message`
///   from the `ApiError` envelope (`{status, code, message, path, errors}`).
class ApiException implements Exception {
  const ApiException({
    required this.statusCode,
    required this.message,
    this.code,
  });

  final int statusCode;
  final String? code;
  final String message;

  bool get isNetwork => statusCode == 0;

  bool get isUnauthorized => statusCode == 401;

  bool get isForbidden => statusCode == 403;

  /// Message safe to show in the UI (backend messages are already friendly).
  String get userMessage => message;

  @override
  String toString() =>
      'ApiException(statusCode: $statusCode, code: $code, message: $message)';
}
