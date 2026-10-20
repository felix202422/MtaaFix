import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/profile/presentation/notification_preferences_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  const defaultPrefs = <String, dynamic>{
    'emailEnabled': true,
    'pushEnabled': false,
    'pushAvailable': false,
  };

  AuthController authWith(List<http.BaseRequest> requests,
      {bool failGet = false}) {
    return AuthController(
      ApiClient(
        baseUrl: 'http://prefs.test/api/v1',
        client: MockClient((request) async {
          requests.add(request);
          final path = request.url.path;
          if (path == '/api/v1/notifications/preferences') {
            if (request.method == 'PUT') {
              final body = jsonDecode(request.body) as Map<String, dynamic>;
              return http.Response(
                jsonEncode({...defaultPrefs, ...body}),
                200,
                headers: jsonHeaders(),
              );
            }
            if (failGet) {
              return http.Response(jsonEncode({'message': 'boom'}), 500,
                  headers: jsonHeaders());
            }
            return http.Response(jsonEncode(defaultPrefs), 200,
                headers: jsonHeaders());
          }
          return http.Response(jsonEncode({'message': 'not mocked: $path'}),
              404,
              headers: jsonHeaders());
        }),
        tokenStore: InMemoryTokenStore(),
      ),
    );
  }

  Future<void> pumpPrefs(WidgetTester tester, AuthController auth) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: const MaterialApp(
          home: NotificationPreferencesScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('loads the preferences and reflects the server flags',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = authWith(requests);
    await pumpPrefs(tester, auth);

    expect(find.text('In-app notifications'), findsOneWidget);
    final email = tester.widget<SwitchListTile>(
      find.widgetWithText(SwitchListTile, 'Email notifications'),
    );
    expect(email.value, isTrue);
    expect(email.onChanged, isNotNull);

    final push = tester.widget<SwitchListTile>(
      find.widgetWithText(SwitchListTile, 'Push notifications'),
    );
    expect(push.value, isFalse);
    expect(push.onChanged, isNull);
    expect(
      find.textContaining('No push provider is configured'),
      findsOneWidget,
    );

    final gets = requests
        .where((r) =>
            r.method == 'GET' &&
            r.url.path == '/api/v1/notifications/preferences')
        .toList();
    expect(gets, hasLength(1));
  });

  testWidgets('toggling email PUTs the preference and confirms',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = authWith(requests);
    await pumpPrefs(tester, auth);

    await tester.tap(find.text('Email notifications'));
    await tester.pumpAndSettle();

    expect(find.text('Notification preferences saved.'), findsOneWidget);
    final puts = requests
        .where((r) =>
            r.method == 'PUT' &&
            r.url.path == '/api/v1/notifications/preferences')
        .toList();
    expect(puts, hasLength(1));
    final body = jsonDecode((puts.single as http.Request).body)
        as Map<String, dynamic>;
    expect(body['emailEnabled'], isFalse);

    final email = tester.widget<SwitchListTile>(
      find.widgetWithText(SwitchListTile, 'Email notifications'),
    );
    expect(email.value, isFalse);
  });

  testWidgets('shows an error state when loading fails and retry recovers',
      (tester) async {
    var failGet = true;
    final requests = <http.BaseRequest>[];
    final auth = AuthController(
      ApiClient(
        baseUrl: 'http://prefs.test/api/v1',
        client: MockClient((request) async {
          requests.add(request);
          final path = request.url.path;
          if (path == '/api/v1/notifications/preferences') {
            if (request.method == 'PUT') {
              return http.Response(jsonEncode(defaultPrefs), 200,
                  headers: jsonHeaders());
            }
            if (failGet) {
              return http.Response(jsonEncode({'message': 'boom'}), 500,
                  headers: jsonHeaders());
            }
            return http.Response(jsonEncode(defaultPrefs), 200,
                headers: jsonHeaders());
          }
          return http.Response(jsonEncode({'message': 'not mocked: $path'}),
              404,
              headers: jsonHeaders());
        }),
        tokenStore: InMemoryTokenStore(),
      ),
    );
    await pumpPrefs(tester, auth);

    expect(find.text('Preferences unavailable'), findsOneWidget);
    expect(find.widgetWithText(ElevatedButton, 'Retry'), findsOneWidget);

    failGet = false;
    await tester.tap(find.widgetWithText(ElevatedButton, 'Retry'));
    await tester.pumpAndSettle();

    expect(find.text('In-app notifications'), findsOneWidget);
    expect(find.text('Preferences unavailable'), findsNothing);
    final gets = requests
        .where((r) =>
            r.method == 'GET' &&
            r.url.path == '/api/v1/notifications/preferences')
        .toList();
    expect(gets, hasLength(2));
  });
}
