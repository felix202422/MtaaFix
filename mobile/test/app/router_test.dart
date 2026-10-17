import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/register_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';
import 'package:mtaafix_mobile/features/splash/presentation/splash_screen.dart';

void main() {
  const base = 'http://router.test/api/v1';

  const validUser = <String, dynamic>{
    'id': 'u1',
    'email': 'citizen@example.com',
    'fullName': 'Asha Mrema',
    'mobile': null,
    'role': 'CITIZEN',
    'organisationId': null,
    'active': true,
  };

  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  AuthController signedOut() => AuthController(
        ApiClient(
          baseUrl: base,
          client: MockClient((_) async => fail('No HTTP call expected')),
          tokenStore: InMemoryTokenStore(),
        ),
      );

  AuthController signedIn() => AuthController(
        ApiClient(
          baseUrl: base,
          client: MockClient((request) async {
            if (request.url.path == '/api/v1/auth/login') {
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
            fail('Unexpected request: ${request.method} ${request.url}');
          }),
          tokenStore: InMemoryTokenStore(),
        ),
      );

  Future<void> pumpRoute(
    WidgetTester tester,
    String name, {
    required AuthController auth,
  }) async {
    final route = generateRoute(RouteSettings(name: name), auth: auth);
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: Builder(builder: (context) => route.builder(context)),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  test('route table documents every named route', () {
    expect(Routes.splash, '/');
    expect(Routes.login, '/login');
    expect(Routes.register, '/register');
    expect(Routes.reports, '/reports');
    expect(Routes.reportNew, '/reports/new');
    expect(Routes.notifications, '/notifications');
    expect(Routes.profile, '/profile');
    expect(
      Routes.protected,
      containsAll(<String>{Routes.reports, Routes.reportNew, Routes.profile}),
    );
  });

  testWidgets('protected routes resolve to sign-in when signed out',
      (tester) async {
    await pumpRoute(tester, Routes.profile, auth: signedOut());
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets('/reports/new is also guarded', (tester) async {
    await pumpRoute(tester, Routes.reportNew, auth: signedOut());
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets('splash renders first, then routes to sign-in when signed out',
      (tester) async {
    final auth = signedOut();
    final route = generateRoute(
      const RouteSettings(name: Routes.splash),
      auth: auth,
    );
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: Builder(builder: (context) => route.builder(context)),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pump();
    expect(find.byType(SplashScreen), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets('unknown routes resolve to the 404 screen', (tester) async {
    await pumpRoute(tester, '/definitely/not/a/route', auth: signedOut());
    expect(find.text('Page not found'), findsOneWidget);
  });

  testWidgets('a signed-in user lands on the report list for /reports',
      (tester) async {
    final auth = signedIn();
    final ok = await auth.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);
    await pumpRoute(tester, Routes.reports, auth: auth);
    expect(find.byType(ReportListScreen), findsOneWidget);
    expect(find.text('My Reports'), findsOneWidget);
  });

  testWidgets('a signed-in user is redirected away from /login',
      (tester) async {
    final auth = signedIn();
    await auth.login('citizen@example.com', 'Password1!');
    await pumpRoute(tester, Routes.login, auth: auth);
    expect(find.byType(ReportListScreen), findsOneWidget);
    expect(find.byType(LoginScreen), findsNothing);
  });

  testWidgets('the register route resolves to the register screen',
      (tester) async {
    await pumpRoute(tester, Routes.register, auth: signedOut());
    expect(find.byType(RegisterScreen), findsOneWidget);
  });
}
