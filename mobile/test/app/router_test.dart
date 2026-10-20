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
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/register_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/forgot_password_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/reset_password_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/change_password_screen.dart';
import 'package:mtaafix_mobile/features/onboarding/presentation/onboarding_screen.dart';
import 'package:mtaafix_mobile/features/profile/presentation/notification_preferences_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_detail_screen.dart';
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
            return http.Response(
              jsonEncode({'message': 'not mocked: $path'}),
              404,
              headers: jsonHeaders(),
            );
          }),
          tokenStore: InMemoryTokenStore(),
        ),
      );

  Future<void> pumpRoute(
    WidgetTester tester,
    String name, {
    required AuthController auth,
    Object? arguments,
  }) async {
    final route = generateRoute(
      RouteSettings(name: name, arguments: arguments),
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
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  test('route table documents every named route', () {
    expect(Routes.splash, '/');
    expect(Routes.onboarding, '/onboarding');
    expect(Routes.home, '/home');
    expect(Routes.login, '/login');
    expect(Routes.register, '/register');
    expect(Routes.forgotPassword, '/forgot-password');
    expect(Routes.resetPassword, '/reset-password');
    expect(Routes.changePassword, '/change-password');
    expect(Routes.reports, '/reports');
    expect(Routes.reportNew, '/reports/new');
    expect(Routes.reportDetail, '/report-details');
    expect(Routes.map, '/map');
    expect(Routes.notifications, '/notifications');
    expect(Routes.profile, '/profile');
    expect(Routes.notificationPreferences, '/notification-preferences');
    expect(
      Routes.protected,
      containsAll(<String>{
        Routes.home,
        Routes.reports,
        Routes.reportNew,
        Routes.reportDetail,
        Routes.map,
        Routes.notifications,
        Routes.profile,
        Routes.changePassword,
        Routes.notificationPreferences,
      }),
    );
    expect(Routes.protected.contains(Routes.onboarding), isFalse);
    expect(Routes.protected.contains(Routes.login), isFalse);
    expect(Routes.protected.contains(Routes.forgotPassword), isFalse);
    expect(Routes.protected.contains(Routes.resetPassword), isFalse);
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

  testWidgets('/home, /map and /report-details are guarded too',
      (tester) async {
    await pumpRoute(tester, Routes.home, auth: signedOut());
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets('/map resolves to sign-in when signed out', (tester) async {
    await pumpRoute(tester, Routes.map, auth: signedOut());
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets('/report-details resolves to sign-in when signed out',
      (tester) async {
    await pumpRoute(tester, Routes.reportDetail, auth: signedOut());
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

  testWidgets('first launch routes from splash to onboarding',
      (tester) async {
    SharedPreferences.setMockInitialValues(const {});
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
    await tester.pumpAndSettle();
    expect(find.byType(OnboardingScreen), findsOneWidget);
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

  testWidgets('a signed-in user is redirected away from /login to /home',
      (tester) async {
    final auth = signedIn();
    await auth.login('citizen@example.com', 'Password1!');
    await pumpRoute(tester, Routes.login, auth: auth);
    expect(find.byType(MainShellScreen), findsOneWidget);
    expect(find.byType(LoginScreen), findsNothing);
  });

  testWidgets('a signed-in user hitting /onboarding lands on the shell',
      (tester) async {
    final auth = signedIn();
    await auth.login('citizen@example.com', 'Password1!');
    await pumpRoute(tester, Routes.onboarding, auth: auth);
    expect(find.byType(MainShellScreen), findsOneWidget);
    expect(find.byType(OnboardingScreen), findsNothing);
  });

  testWidgets('/report-details builds the detail screen with an id argument',
      (tester) async {
    final auth = signedIn();
    await auth.login('citizen@example.com', 'Password1!');
    // GET /reports/{id} is not mocked above → 404 → the screen shows its
    // error state, which still proves the route resolves to the screen with
    // the id argument flowing through.
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
          home: const _PushNamedLauncher(
            routeName: Routes.reportDetail,
            arguments: 'report-1',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(ReportDetailScreen), findsOneWidget);
    expect(find.text('Report unavailable'), findsOneWidget);
  });

  testWidgets('the register route resolves to the register screen',
      (tester) async {
    await pumpRoute(tester, Routes.register, auth: signedOut());
    expect(find.byType(RegisterScreen), findsOneWidget);
  });

  testWidgets('the forgot and reset password routes resolve to their screens',
      (tester) async {
    await pumpRoute(tester, Routes.forgotPassword, auth: signedOut());
    expect(find.byType(ForgotPasswordScreen), findsOneWidget);

    await pumpRoute(tester, Routes.resetPassword, auth: signedOut());
    expect(find.byType(ResetPasswordScreen), findsOneWidget);
  });

  testWidgets('the notification preferences route resolves to its screen',
      (tester) async {
    final auth = signedIn();
    await auth.login('citizen@example.com', 'Password1!');
    // GET /notifications/preferences is not mocked above → 404 → the screen
    // shows its error state, which still proves the route resolves to it.
    await pumpRoute(tester, Routes.notificationPreferences, auth: auth);
    expect(find.byType(NotificationPreferencesScreen), findsOneWidget);
    expect(find.text('Preferences unavailable'), findsOneWidget);
  });

  testWidgets('change password is guarded when signed out and open when '
      'signed in', (tester) async {
    await pumpRoute(tester, Routes.changePassword, auth: signedOut());
    expect(find.byType(LoginScreen), findsOneWidget);

    final auth = signedIn();
    await auth.login('citizen@example.com', 'Password1!');
    await pumpRoute(tester, Routes.changePassword, auth: auth);
    expect(find.byType(ChangePasswordScreen), findsOneWidget);

    await pumpRoute(tester, Routes.notificationPreferences, auth: signedOut());
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets('a signed-in user is redirected away from /forgot-password',
      (tester) async {
    final auth = signedIn();
    await auth.login('citizen@example.com', 'Password1!');
    await pumpRoute(tester, Routes.forgotPassword, auth: auth);
    expect(find.byType(MainShellScreen), findsOneWidget);
    expect(find.byType(ForgotPasswordScreen), findsNothing);
  });
}

/// Pushes [routeName] once after the first frame so named-route tests can be
/// driven from a plain [MaterialApp.home].
class _PushNamedLauncher extends StatefulWidget {
  const _PushNamedLauncher({required this.routeName, this.arguments});

  final String routeName;
  final Object? arguments;

  @override
  State<_PushNamedLauncher> createState() => _PushNamedLauncherState();
}

class _PushNamedLauncherState extends State<_PushNamedLauncher> {
  bool _pushed = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_pushed) return;
    _pushed = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      Navigator.of(context).pushNamed(widget.routeName, arguments: widget.arguments);
    });
  }

  @override
  Widget build(BuildContext context) => const SizedBox.shrink();
}
