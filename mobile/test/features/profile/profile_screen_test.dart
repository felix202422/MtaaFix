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
          if (path == '/api/v1/notifications/preferences') {
            if (request.method == 'PUT') {
              final body = jsonDecode(request.body) as Map<String, dynamic>;
              return http.Response(jsonEncode(body), 200,
                  headers: jsonHeaders());
            }
            return http.Response(
              jsonEncode({
                'emailEnabled': true,
                'pushEnabled': false,
                'pushAvailable': false,
              }),
              200,
              headers: jsonHeaders(),
            );
          }
          if (path == '/api/v1/auth/password/change') {
            return http.Response(
              jsonEncode({'success': true, 'message': 'Password changed.'}),
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
      find.textContaining('Choose which channels you use'),
      findsOneWidget,
    );
    expect(find.text('Change password'), findsOneWidget);
    expect(
      find.textContaining('Update your password while signed in.'),
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

  testWidgets('the notification preferences row opens the preferences screen',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await signedInAuth(requests: requests);
    await pumpProfile(tester, auth);

    final row = find.text('Notification preferences');
    await tester.ensureVisible(row);
    await tester.pumpAndSettle();
    await tester.tap(row);
    await tester.pumpAndSettle();

    expect(find.text('In-app notifications'), findsOneWidget);
    expect(find.text('Email notifications'), findsOneWidget);
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

    final toggle = find.text('Email notifications');
    await tester.ensureVisible(toggle);
    await tester.pumpAndSettle();
    await tester.tap(toggle);
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
  });

  testWidgets('the change password row submits POST /auth/password/change',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await signedInAuth(requests: requests);
    await pumpProfile(tester, auth);

    final row = find.text('Change password');
    await tester.ensureVisible(row);
    await tester.pumpAndSettle();
    await tester.tap(row);
    await tester.pumpAndSettle();

    expect(find.text('Update your password'), findsOneWidget);

    final fields = find.descendant(
      of: find.byType(Form),
      matching: find.byType(TextFormField),
    );
    expect(fields, findsNWidgets(3));
    await tester.enterText(fields.at(0), 'Password1!');
    await tester.enterText(fields.at(1), 'NewPassword1');
    await tester.enterText(fields.at(2), 'NewPassword1');

    final submit = find.widgetWithText(ElevatedButton, 'Change password');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pumpAndSettle();

    expect(find.text('Password changed.'), findsOneWidget);
    expect(find.text('Update your password'), findsNothing);
    expect(auth.isAuthenticated, isTrue);

    final posts = requests
        .where((r) =>
            r.method == 'POST' &&
            r.url.path == '/api/v1/auth/password/change')
        .toList();
    expect(posts, hasLength(1));
    final body = jsonDecode((posts.single as http.Request).body)
        as Map<String, dynamic>;
    expect(body['currentPassword'], 'Password1!');
    expect(body['newPassword'], 'NewPassword1');
  });
}
