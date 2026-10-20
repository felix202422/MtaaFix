/// Per-user notification channel preferences (FR-6.4).
class NotificationPreference {
  const NotificationPreference({
    required this.emailEnabled,
    required this.pushEnabled,
    required this.pushAvailable,
  });

  final bool emailEnabled;
  final bool pushEnabled;

  /// Whether a push provider is configured on this deployment — when false
  /// the push toggle is read-only for the user.
  final bool pushAvailable;

  factory NotificationPreference.fromJson(Map<String, dynamic> json) =>
      NotificationPreference(
        emailEnabled: json['emailEnabled'] == true,
        pushEnabled: json['pushEnabled'] == true,
        pushAvailable: json['pushAvailable'] == true,
      );
}
