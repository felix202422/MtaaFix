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
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/profile/presentation/profile_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  const validUser = <String, dynamic>{
    'id': 'u1',
    'email': 'citizen@example.com',
    'fullName': 'Asha Mrema',
    'mobile': '+255712345678',
    'role': 'CITIZEN',
    'organisationId': 'org-1',
    'active': true,
  };

  Future<AuthController> signedInAuth({
    List<http.BaseRequest>? requests,
  }) async {
    final log = requests ?? <http.BaseRequest>[];
    final controller = AuthController(
      ApiClient(
        baseUrl: 'http://profile.test/api/v1',
        client: MockClient((request) async {
          log.add(request);
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
          if (path == '/api/v1/auth/me' && request.method == 'PUT') {
            final body = jsonDecode(request.body) as Map<String, dynamic>;
            return http.Response(
              jsonEncode({
                'id': 'u1',
                'email': 'citizen@example.com',
                'fullName': body['fullName'] ?? validUser['fullName'],
                'mobile': body['mobile'] ?? validUser['mobile'],
                'role': 'CITIZEN',
                'organisationId': 'org-1',
                'active': true,
              }),
              200,
              headers: jsonHeaders(),
            );
          }
          if (path == '/api/v1/auth/logout') {
            return http.Response('{}', 200, headers: jsonHeaders());
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

  Future<void> pumpProfile(
    WidgetTester tester,
    AuthController auth,
  ) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const ProfileScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets('shows the account details, privacy and about cards',
      (tester) async {
    final auth = await signedInAuth();
    await pumpProfile(tester, auth);

    expect(find.text('Profile'), findsOneWidget);
    expect(find.text('Asha Mrema'), findsOneWidget);
    expect(find.text('citizen@example.com'), findsWidgets);
    expect(find.text('+255712345678'), findsWidgets);
    expect(find.text('Citizen'), findsOneWidget);
    expect(find.text('org-1'), findsOneWidget);
    expect(find.text('Privacy'), findsOneWidget);
    expect(
      find.textContaining('anonymised form on the public map'),
      findsOneWidget,
    );
    expect(find.text('Notification preferences'), findsOneWidget);
    expect(
      find.textContaining('arrive with push'),
      findsOneWidget,
    );
    expect(find.text('About MtaaFix'), findsOneWidget);
    expect(find.text('Sign Out'), findsOneWidget);
  });

  testWidgets('edit profile saves the name through PUT /auth/me',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await signedInAuth(requests: requests);
    await pumpProfile(tester, auth);

    await tester.tap(find.widgetWithText(OutlinedButton, 'Edit profile'));
    await tester.pumpAndSettle();
    expect(find.text('Edit profile'), findsWidgets);

    final nameField = find.descendant(
      of: find.byType(AlertDialog),
      matching: find.byType(TextField),
    );
    await tester.enterText(nameField.first, 'Asha J. Mrema');
    await tester.tap(find.widgetWithText(FilledButton, 'Save'));
    await tester.pumpAndSettle();

    expect(find.text('Profile updated.'), findsOneWidget);
    expect(find.text('Asha J. Mrema'), findsOneWidget);

    final put = requests.where((r) => r.method == 'PUT' && r.url.path == '/api/v1/auth/me');
    expect(put, hasLength(1));
    final body = jsonDecode((put.single as http.Request).body)
        as Map<String, dynamic>;
    expect(body['fullName'], 'Asha J. Mrema');
    expect(auth.user?.fullName, 'Asha J. Mrema');
  });

  testWidgets('edit profile surfaces a failure without changing the user',
      (tester) async {
    final controller = AuthController(
      ApiClient(
        baseUrl: 'http://profile.test/api/v1',
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
          return http.Response(jsonEncode({'message': 'save failed'}), 500,
              headers: jsonHeaders());
        }),
        tokenStore: InMemoryTokenStore(),
      ),
    );
    final ok = await controller.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);

    await pumpProfile(tester, controller);

    await tester.tap(find.widgetWithText(OutlinedButton, 'Edit profile'));
    await tester.pumpAndSettle();
    await tester.enterText(
      find
          .descendant(
            of: find.byType(AlertDialog),
            matching: find.byType(TextField),
          )
          .first,
      'Someone Else',
    );
    await tester.tap(find.widgetWithText(FilledButton, 'Save'));
    await tester.pumpAndSettle();

    expect(find.text('Could not update profile.'), findsOneWidget);
    expect(controller.user?.fullName, 'Asha Mrema');
  });

  testWidgets('sign out asks for confirmation and returns to sign-in',
      (tester) async {
    final auth = await signedInAuth();
    await pumpProfile(tester, auth);

    final signOut = find.text('Sign Out');
    await tester.ensureVisible(signOut);
    await tester.pumpAndSettle();
    await tester.tap(signOut);
    await tester.pumpAndSettle();
    expect(find.text('Sign out?'), findsOneWidget);

    await tester.tap(find.text('Sign Out').last);
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);
  });
}
