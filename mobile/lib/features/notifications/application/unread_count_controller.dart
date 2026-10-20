import 'package:flutter/foundation.dart';

import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/features/notifications/application/notifications_api.dart';

/// Keeps the authoritative unread-notification count for the Alerts tab badge
/// (FR-6.1). Failures are swallowed — a badge refresh is never fatal.
class UnreadCountController extends ChangeNotifier {
  UnreadCountController(this._api);

  final ApiClient _api;

  int _count = 0;

  int get count => _count;

  Future<void> refresh() async {
    try {
      final count = await NotificationsApi(_api).unreadCount();
      if (count != _count) {
        _count = count;
        notifyListeners();
      }
    } catch (_) {
      // Keep the last known count.
    }
  }
}
