import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/onboarding/presentation/onboarding_screen.dart';

void main() {
  AuthController signedOut() => AuthController(
        ApiClient(
          baseUrl: 'http://onboarding.test/api/v1',
          client: MockClient((_) async => fail('No HTTP call expected')),
          tokenStore: InMemoryTokenStore(),
        ),
      );

  Future<void> pumpOnboarding(
    WidgetTester tester,
    AuthController auth,
  ) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const OnboardingScreen(),
          onGenerateRoute: (settings) =>
              generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues(const {});
  });

  testWidgets('shows the first slide with navigation controls',
      (tester) async {
    await pumpOnboarding(tester, signedOut());

    expect(find.text('Report issues in seconds'), findsOneWidget);
    expect(find.text('Skip'), findsOneWidget);
    expect(find.text('Next'), findsOneWidget);
    expect(find.text('Get started'), findsNothing);
  });

  testWidgets('Next advances through all three slides', (tester) async {
    await pumpOnboarding(tester, signedOut());

    await tester.tap(find.text('Next'));
    await tester.pumpAndSettle();
    expect(find.text('Track every report'), findsOneWidget);

    await tester.tap(find.text('Next'));
    await tester.pumpAndSettle();
    expect(find.text('Improve your community'), findsOneWidget);
    expect(find.text('Get started'), findsOneWidget);
    expect(find.text('Next'), findsNothing);
  });

  testWidgets('Skip marks onboarding as seen and continues to sign-in',
      (tester) async {
    await pumpOnboarding(tester, signedOut());

    await tester.tap(find.text('Skip'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getBool(OnboardingScreen.seenKey), isTrue);
  });

  testWidgets('Get started finishes onboarding and routes to sign-in',
      (tester) async {
    await pumpOnboarding(tester, signedOut());

    await tester.tap(find.text('Next'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Next'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Get started'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getBool(OnboardingScreen.seenKey), isTrue);
  });

  testWidgets('does not persist the seen flag before finishing',
      (tester) async {
    await pumpOnboarding(tester, signedOut());

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getBool(OnboardingScreen.seenKey), isNull);
  });
}
