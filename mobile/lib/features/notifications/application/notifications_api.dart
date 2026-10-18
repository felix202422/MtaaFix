import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/network/paged.dart';
import 'package:mtaafix_mobile/features/notifications/domain/notification_item.dart';

class NotificationsApi {
  NotificationsApi(this._api);

  final ApiClient _api;

  Future<Paged<NotificationItem>> list({int page = 0, int size = 20}) async {
    final json = await _api.get('/notifications?page=$page&size=$size');
    return Paged.parse(json, NotificationItem.fromJson);
  }

  Future<void> markRead(String id) =>
      _api.post('/notifications/${Uri.encodeComponent(id)}/read');

  Future<void> markAllRead() => _api.post('/notifications/read-all');
}
