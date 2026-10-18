import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/home/presentation/home_screen.dart';
import 'package:mtaafix_mobile/features/map/presentation/map_screen.dart';
import 'package:mtaafix_mobile/features/notifications/presentation/notifications_screen.dart';
import 'package:mtaafix_mobile/features/profile/presentation/profile_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

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

  Future<AuthController> signedInAuth() async {
    final controller = AuthController(
      ApiClient(
        baseUrl: 'http://shell.test/api/v1',
        client: MockClient((request) async {
          final path = request.url.path;
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
          if (path == '/api/v1/reports' ||
              path == '/api/v1/notifications' ||
              path.startsWith('/api/v1/reports/public')) {
            return emptyPage();
          }
          if (path == '/api/v1/categories/public') {
            return http.Response(jsonEncode({'content': <dynamic>[]}), 200,
                headers: jsonHeaders());
          }
          if (path == '/api/v1/dashboard/me') {
            return http.Response(jsonEncode({'stats': {}}), 200,
                headers: jsonHeaders());
          }
          return http.Response(jsonEncode({'message': 'not mocked: $path'}),
              404,
              headers: jsonHeaders());
        }),
        tokenStore: InMemoryTokenStore(),
      ),
    );
    final ok = await controller.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);
    return controller;
  }

  Future<void> pumpShell(WidgetTester tester, AuthController auth) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const MainShellScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets('starts on the Home tab with all five destinations',
      (tester) async {
    final auth = await signedInAuth();
    await pumpShell(tester, auth);

    expect(find.byType(HomeScreen), findsOneWidget);
    expect(find.byType(ReportListScreen), findsNothing);
    expect(find.byType(MapScreen), findsNothing);
    expect(find.byType(NotificationsScreen), findsNothing);
    expect(find.byType(ProfileScreen), findsNothing);

    for (final label in ['Home', 'Reports', 'Map', 'Alerts', 'Profile']) {
      expect(find.text(label), findsOneWidget);
    }
    expect(find.text('Karibu, Asha'), findsOneWidget);
  });

  testWidgets('switches tabs and keeps each screen reachable',
      (tester) async {
    final auth = await signedInAuth();
    await pumpShell(tester, auth);

    await tester.tap(find.text('Reports'));
    await tester.pumpAndSettle();
    expect(find.byType(ReportListScreen), findsOneWidget);
    expect(find.text('My Reports'), findsOneWidget);

    await tester.tap(find.text('Map'));
    await tester.pumpAndSettle();
    expect(find.byType(MapScreen), findsOneWidget);
    expect(find.text('Community Map'), findsOneWidget);

    await tester.tap(find.text('Alerts'));
    await tester.pumpAndSettle();
    expect(find.byType(NotificationsScreen), findsOneWidget);
    expect(find.text('No notifications'), findsOneWidget);

    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();
    expect(find.byType(ProfileScreen), findsOneWidget);
    expect(find.text('citizen@example.com'), findsWidgets);

    await tester.tap(find.text('Home'));
    await tester.pumpAndSettle();
    expect(find.byType(HomeScreen), findsOneWidget);
    expect(find.byType(ReportListScreen), findsNothing);
  });
}
