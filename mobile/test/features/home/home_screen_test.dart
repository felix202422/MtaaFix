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
import 'package:mtaafix_mobile/features/home/presentation/home_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  const validUser = <String, dynamic>{
    'id': 'u1',
    'email': 'citizen@example.com',
    'fullName': 'Asha Mrema',
    'mobile': null,
    'role': 'CITIZEN',
    'organisationId': null,
    'active': true,
  };

  http.Response emptyPage() => http.Response(
        jsonEncode({
          'content': <dynamic>[],
          'totalElements': 0,
          'totalPages': 0,
          'page': 0,
          'size': 20,
        }),
        200,
        headers: jsonHeaders(),
      );

  Future<AuthController> signedInAuth(MockClient client) async {
    final controller = AuthController(
      ApiClient(
        baseUrl: 'http://home.test/api/v1',
        client: client,
        tokenStore: InMemoryTokenStore(),
      ),
    );
    final ok = await controller.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);
    return controller;
  }

  Future<void> pumpHome(WidgetTester tester, AuthController auth) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const HomeScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets('greets the signed-in citizen and shows the primary action',
      (tester) async {
    final auth = await signedInAuth(MockClient((request) async {
      final path = request.url.path;
      if (path == '/api/v1/auth/login') {
        return http.Response(
          jsonEncode({
            'accessToken': 'access-1',
            'refreshToken': 'refresh-1',
            'accessTokenExpiresIn': 3600,
            'user': validUser,
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      if (path == '/api/v1/dashboard/me') {
        return http.Response(
          jsonEncode({
            'organisationId': 'org-1',
            'stats': {'SUBMITTED': 3, 'RESOLVED': 1},
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      if (path == '/api/v1/reports') {
        return http.Response(
          jsonEncode({
            'content': [
              {
                'id': 'r1',
                'code': 'MTF-0001',
                'title': 'Pothole near the market',
                'status': 'SUBMITTED',
                'reportedAt': '2026-10-01T09:00:00Z',
              },
            ],
            'totalElements': 1,
            'totalPages': 1,
            'page': 0,
            'size': 5,
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      if (path == '/api/v1/notifications') {
        return http.Response(
          jsonEncode({
            'content': [
              {
                'id': 'n1',
                'title': 'Report received',
                'body': 'We are reviewing MTF-0001',
                'isRead': false,
                'timestamp': '2026-10-01T10:00:00',
              },
            ],
            'totalElements': 1,
            'totalPages': 1,
            'page': 0,
            'size': 3,
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      return http.Response(jsonEncode({'message': 'not mocked'}), 404,
          headers: jsonHeaders());
    }));

    await pumpHome(tester, auth);

    expect(find.text('Karibu, Asha'), findsOneWidget);
    expect(
      find.widgetWithText(ElevatedButton, 'Report an Issue'),
      findsOneWidget,
    );
    expect(find.text('reports submitted'), findsOneWidget);
    expect(find.text('Pothole near the market'), findsOneWidget);
    expect(find.text('Report received'), findsOneWidget);
    expect(find.text('See all'), findsNWidgets(2));
  });

  testWidgets('shows the empty state before any report activity exists',
      (tester) async {
    final auth = await signedInAuth(MockClient((request) async {
      final path = request.url.path;
      if (path == '/api/v1/auth/login') {
        return http.Response(
          jsonEncode({
            'accessToken': 'access-1',
            'refreshToken': 'refresh-1',
            'accessTokenExpiresIn': 3600,
            'user': validUser,
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      if (path == '/api/v1/dashboard/me') {
        return http.Response(jsonEncode({'stats': {}}), 200,
            headers: jsonHeaders());
      }
      if (path == '/api/v1/reports' || path == '/api/v1/notifications') {
        return emptyPage();
      }
      return http.Response(jsonEncode({'message': 'not mocked'}), 404,
          headers: jsonHeaders());
    }));

    await pumpHome(tester, auth);

    expect(find.text('No report activity yet.'), findsOneWidget);
    expect(
      find.text('No reports yet — your submissions will show up here.'),
      findsOneWidget,
    );
    expect(find.text('You are all caught up.'), findsOneWidget);
  });

  testWidgets('shows a friendly error when the home feed fails',
      (tester) async {
    final auth = await signedInAuth(MockClient((request) async {
      final path = request.url.path;
      if (path == '/api/v1/auth/login') {
        return http.Response(
          jsonEncode({
            'accessToken': 'access-1',
            'refreshToken': 'refresh-1',
            'accessTokenExpiresIn': 3600,
            'user': validUser,
          }),
          200,
          headers: jsonHeaders(),
        );
      }
      return http.Response(jsonEncode({'message': 'boom'}), 500,
          headers: jsonHeaders());
    }));

    await pumpHome(tester, auth);

    expect(find.text('Home unavailable'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });
}
