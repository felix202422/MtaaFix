import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/notifications/presentation/notifications_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  Map<String, dynamic> notification(
    String id, {
    String title = 'Report received',
    String body = 'We are reviewing MTF-0001',
    bool isRead = false,
    String type = 'STATUS_UPDATE',
    String? reportId = 'r1',
  }) =>
      {
        'id': id,
        'title': title,
        'body': body,
        'isRead': isRead,
        'type': type,
        'timestamp': '2026-10-01T10:00:00',
        'reportId': reportId,
      };

  /// [calls] records `METHOD path` for every backend interaction.
  MockClient api(List<String> calls, List<Map<String, dynamic>> items,
      {int totalPages = 1}) {
    return MockClient((request) async {
      final path = request.url.path;
      calls.add('${request.method} $path');
      if (path == '/api/v1/auth/login') {
        return http.Response(
          jsonEncode({
            'accessToken': 'access-1',
            'refreshToken': 'refresh-1',
            'accessTokenExpiresIn': 3600,
            'user': {
              'id': 'u1',
              'email': 'citizen@example.com',
              'fullName': 'Asha Mrema',
              'mobile': null,
              'role': 'CITIZEN',
              'organisationId': null,
              'active': true,
            },
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      if (request.method == 'GET' && path == '/api/v1/notifications') {
        return http.Response(
          jsonEncode({
            'content': items,
            'totalElements': items.length,
            'totalPages': totalPages,
            'page': int.tryParse(request.url.queryParameters['page'] ?? '0') ?? 0,
            'size': 20,
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      if (path.startsWith('/api/v1/notifications')) {
        return http.Response('{}', 200, headers: jsonHeaders());
      }
      if (path == '/api/v1/reports/r1') {
        return http.Response(
          jsonEncode({
            'id': 'r1',
            'code': 'MTF-0001',
            'title': 'Pothole near the market',
            'status': 'SUBMITTED',
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      return http.Response(jsonEncode({'message': 'not mocked: $path'}),
          404,
          headers: jsonHeaders());
    });
  }

  AuthController controller(MockClient client) => AuthController(
        ApiClient(
          baseUrl: 'http://notif.test/api/v1',
          client: client,
          tokenStore: InMemoryTokenStore(),
        ),
      );

  Future<AuthController> signedIn(MockClient client) async {
    final auth = controller(client);
    final ok = await auth.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);
    return auth;
  }

  Future<void> pumpNotifications(
    WidgetTester tester,
    AuthController auth,
  ) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const NotificationsScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets('renders the real feed with unread count', (tester) async {
    final calls = <String>[];
    final auth = controller(api(calls, [
      notification('n1', title: 'Report received'),
      notification('n2',
          title: 'Now in progress', isRead: true, reportId: null),
    ]));

    await pumpNotifications(tester, auth);

    expect(find.text('Notifications'), findsOneWidget);
    expect(find.text('Report received'), findsOneWidget);
    expect(find.text('Now in progress'), findsOneWidget);
    expect(find.text('1 unread'), findsOneWidget);
    expect(calls.first, 'GET /api/v1/notifications');
  });

  testWidgets('shows the empty state when there is nothing to show',
      (tester) async {
    final auth = controller(api(<String>[], []));

    await pumpNotifications(tester, auth);

    expect(find.text('No notifications'), findsOneWidget);
    expect(
      find.text('Updates about your reports will appear here.'),
      findsOneWidget,
    );
  });

  testWidgets('tapping an unread notification marks it read', (tester) async {
    final calls = <String>[];
    final auth = await signedIn(api(calls, [
      notification('n1', title: 'Report received'),
    ]));

    await pumpNotifications(tester, auth);

    await tester.tap(find.text('Report received'));
    await tester.pumpAndSettle();

    expect(calls, contains('POST /api/v1/notifications/n1/read'));
    expect(find.text('1 unread'), findsNothing);
    expect(find.text('Pothole near the market'), findsOneWidget);
  });

  testWidgets('mark-all-read marks the whole feed', (tester) async {
    final calls = <String>[];
    final auth = controller(api(calls, [
      notification('n1', title: 'Report received'),
      notification('n2', title: 'Status changed'),
    ]));

    await pumpNotifications(tester, auth);
    expect(find.text('2 unread'), findsOneWidget);

    await tester.tap(find.byTooltip('Mark all read'));
    await tester.pumpAndSettle();

    expect(calls, contains('POST /api/v1/notifications/read-all'));
    expect(find.text('unread'), findsNothing);
  });

  testWidgets('shows a retryable error when the feed fails', (tester) async {
    final failing = AuthController(
      ApiClient(
        baseUrl: 'http://notif.test/api/v1',
        client: MockClient((_) async =>
            http.Response(jsonEncode({'message': 'down'}), 500,
                headers: jsonHeaders())),
        tokenStore: InMemoryTokenStore(),
      ),
    );

    await pumpNotifications(tester, failing);

    expect(find.text('Notifications unavailable'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });
}
