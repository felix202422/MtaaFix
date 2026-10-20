import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/notifications/application/notifications_api.dart';
import 'package:mtaafix_mobile/features/notifications/domain/notification_item.dart';

void main() {
  const base = 'http://notifications.test/api/v1';

  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  NotificationsApi apiWith(MockClient mock) => NotificationsApi(
        ApiClient(baseUrl: base, client: mock, tokenStore: InMemoryTokenStore()),
      );

  test('list requests the first page with the default size', () async {
    late Uri seen;
    final api = apiWith(MockClient((request) async {
      seen = request.url;
      expect(request.method, 'GET');
      return http.Response(
        jsonEncode({
          'content': [
            {
              'id': 'n1',
              'title': 'Report assigned',
              'body': 'Moderator picked up MTF-0001',
              'type': 'ASSIGNMENT',
              'isRead': false,
              'timestamp': '2026-10-01T10:00:00',
              'reportId': 'r1',
            },
          ],
          'totalElements': 1,
          'totalPages': 1,
          'page': 0,
          'size': 20,
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final page = await api.list();

    expect(seen.path, '/api/v1/notifications');
    expect(seen.queryParameters['page'], '0');
    expect(seen.queryParameters['size'], '20');

    final item = page.content.single;
    expect(item.id, 'n1');
    expect(item.isRead, isFalse);
    expect(item.reportId, 'r1');
    expect(item.type, 'ASSIGNMENT');
  });

  test('list parses Spring Page shape (number field)', () async {
    final api = apiWith(MockClient((request) async {
      return http.Response(
        jsonEncode({
          'content': [
            {
              'id': 'n2',
              'title': 'Status update',
              'body': 'Now in progress',
              'isRead': true,
              'timestamp': '2026-10-02T10:00:00',
            },
          ],
          'totalElements': 1,
          'totalPages': 1,
          'number': 0,
          'size': 20,
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final page = await api.list(page: 1);

    expect(page.page, 0);
    expect(page.content.single.id, 'n2');
    expect(page.content.single.isRead, isTrue);
  });

  test('markRead posts to /notifications/{id}/read', () async {
    final hits = <String>[];
    final api = apiWith(MockClient((request) async {
      hits.add('${request.method} ${request.url.path}');
      return http.Response('{}', 200, headers: jsonHeaders());
    }));

    await api.markRead('n1');

    expect(hits, ['POST /api/v1/notifications/n1/read']);
  });

  test('markAllRead posts to /notifications/read-all', () async {
    final hits = <String>[];
    final api = apiWith(MockClient((request) async {
      hits.add('${request.method} ${request.url.path}');
      return http.Response('{}', 200, headers: jsonHeaders());
    }));

    await api.markAllRead();

    expect(hits, ['POST /api/v1/notifications/read-all']);
  });

  test('NotificationItem tolerates missing optional fields', () {
    final item = NotificationItem.fromJson({
      'id': 'n9',
      'title': 'T',
      'body': 'B',
    });

    expect(item.isRead, isFalse);
    expect(item.timestamp, isNull);
    expect(item.reportId, isNull);
    expect(item.type, isNull);
  });

  test('unreadCount requests GET /notifications/unread-count', () async {
    late Uri seen;
    final api = apiWith(MockClient((request) async {
      seen = request.url;
      expect(request.method, 'GET');
      return http.Response(jsonEncode({'count': 7}), 200,
          headers: jsonHeaders());
    }));

    expect(await api.unreadCount(), 7);
    expect(seen.path, '/api/v1/notifications/unread-count');
  });

  test('unreadCount falls back to 0 without a count field', () async {
    final api = apiWith(MockClient((request) async {
      return http.Response(jsonEncode(<String, dynamic>{}), 200,
          headers: jsonHeaders());
    }));

    expect(await api.unreadCount(), 0);
  });

  test('getPreferences reads the channel flags', () async {
    late Uri seen;
    final api = apiWith(MockClient((request) async {
      seen = request.url;
      expect(request.method, 'GET');
      return http.Response(
        jsonEncode({
          'emailEnabled': false,
          'pushEnabled': true,
          'pushAvailable': true,
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final prefs = await api.getPreferences();

    expect(seen.path, '/api/v1/notifications/preferences');
    expect(prefs.emailEnabled, isFalse);
    expect(prefs.pushEnabled, isTrue);
    expect(prefs.pushAvailable, isTrue);
  });

  test('updatePreferences sends a JSON PUT with only the changed flag',
      () async {
    late http.Request seen;
    final api = apiWith(MockClient((request) async {
      seen = request;
      return http.Response(
        jsonEncode({
          'emailEnabled': false,
          'pushEnabled': false,
          'pushAvailable': false,
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final prefs = await api.updatePreferences(emailEnabled: false);

    expect(seen.method, 'PUT');
    expect(seen.url.path, '/api/v1/notifications/preferences');
    final body = jsonDecode(seen.body) as Map<String, dynamic>;
    expect(body['emailEnabled'], isFalse);
    expect(body.containsKey('pushEnabled'), isFalse);
    expect(prefs.emailEnabled, isFalse);
    expect(prefs.pushAvailable, isFalse);
  });
}
