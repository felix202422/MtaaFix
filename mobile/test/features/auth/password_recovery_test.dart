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
import 'package:mtaafix_mobile/features/auth/presentation/forgot_password_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/reset_password_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  Future<AuthController> authWith(
    List<http.BaseRequest> requests, {
    bool failForgot = false,
  }) async {
    final controller = AuthController(
      ApiClient(
        baseUrl: 'http://recovery.test/api/v1',
        client: MockClient((request) async {
          requests.add(request);
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
          if (path == '/api/v1/auth/password/forgot') {
            if (failForgot) {
              return http.Response(jsonEncode({'message': 'Mail is down.'}),
                  500,
                  headers: jsonHeaders());
            }
            return http.Response(
              jsonEncode({'success': true, 'message': 'Accepted.'}),
              200,
              headers: jsonHeaders(),
            );
          }
          if (path == '/api/v1/auth/password/reset') {
            return http.Response(
              jsonEncode({'success': true, 'message': 'Password reset.'}),
              200,
              headers: jsonHeaders(),
            );
          }
          return http.Response(jsonEncode({'message': 'not mocked: $path'}),
              404,
              headers: jsonHeaders());
        }),
        tokenStore: InMemoryTokenStore(),
      ),
    );
    return controller;
  }

  Future<void> pumpForgot(
    WidgetTester tester,
    AuthController auth,
  ) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const ForgotPasswordScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> pumpReset(
    WidgetTester tester,
    AuthController auth,
  ) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const ResetPasswordScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('forgot password posts the email and always confirms',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await authWith(requests);
    await pumpForgot(tester, auth);

    await tester.enterText(find.byType(TextFormField), 'asha@example.com');
    await tester.tap(find.widgetWithText(ElevatedButton, 'Send reset link'));
    await tester.pumpAndSettle();

    expect(find.text('Check your inbox'), findsOneWidget);
    expect(
      find.textContaining('If an account exists for asha@example.com'),
      findsOneWidget,
    );
    expect(
      find.textContaining('expires in 30 minutes'),
      findsOneWidget,
    );

    final posts = requests
        .where((r) =>
            r.method == 'POST' &&
            r.url.path == '/api/v1/auth/password/forgot')
        .toList();
    expect(posts, hasLength(1));
    final body = jsonDecode((posts.single as http.Request).body)
        as Map<String, dynamic>;
    expect(body['email'], 'asha@example.com');

    await tester.tap(find.widgetWithText(ElevatedButton, 'I have a reset token'));
    await tester.pumpAndSettle();
    expect(find.byType(ResetPasswordScreen), findsOneWidget);
    expect(find.text('Set a new password'), findsOneWidget);
  });

  testWidgets('forgot password validates the email before calling the API',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await authWith(requests);
    await pumpForgot(tester, auth);

    await tester.enterText(find.byType(TextFormField), 'not-an-email');
    await tester.tap(find.widgetWithText(ElevatedButton, 'Send reset link'));
    await tester.pumpAndSettle();

    expect(find.text('Please enter a valid email'), findsOneWidget);
    expect(find.text('Check your inbox'), findsNothing);
    expect(
      requests.where((r) => r.url.path == '/api/v1/auth/password/forgot'),
      isEmpty,
    );
  });

  testWidgets('forgot password failure keeps the form and surfaces an error',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await authWith(requests, failForgot: true);
    await pumpForgot(tester, auth);

    await tester.enterText(find.byType(TextFormField), 'asha@example.com');
    await tester.tap(find.widgetWithText(ElevatedButton, 'Send reset link'));
    await tester.pumpAndSettle();

    expect(find.text('Check your inbox'), findsNothing);
    expect(find.text('Send reset link'), findsOneWidget);
    expect(find.byType(SnackBar), findsOneWidget);
    expect(auth.isAuthenticated, isFalse);
  });

  testWidgets('reset password requires matching fields before posting',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await authWith(requests);
    await pumpReset(tester, auth);

    final fields = find.descendant(
      of: find.byType(Form),
      matching: find.byType(TextFormField),
    );
    expect(fields, findsNWidgets(3));
    await tester.enterText(fields.at(0), 'tok-123');
    await tester.enterText(fields.at(1), 'NewSecret1');
    await tester.enterText(fields.at(2), 'Different1');

    await tester.tap(find.widgetWithText(ElevatedButton, 'Set new password'));
    await tester.pumpAndSettle();

    expect(find.text('Passwords do not match'), findsOneWidget);
    expect(
      requests.where((r) => r.url.path == '/api/v1/auth/password/reset'),
      isEmpty,
    );
  });

  testWidgets('a valid reset posts the token and lands on sign-in',
      (tester) async {
    final requests = <http.BaseRequest>[];
    final auth = await authWith(requests);
    await pumpReset(tester, auth);

    final fields = find.descendant(
      of: find.byType(Form),
      matching: find.byType(TextFormField),
    );
    await tester.enterText(fields.at(0), 'tok-123');
    await tester.enterText(fields.at(1), 'NewSecret1');
    await tester.enterText(fields.at(2), 'NewSecret1');

    await tester.tap(find.widgetWithText(ElevatedButton, 'Set new password'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.byType(ResetPasswordScreen), findsNothing);
    expect(
      find.text('Password has been reset. Sign in with it now.'),
      findsOneWidget,
    );

    final posts = requests
        .where((r) =>
            r.method == 'POST' && r.url.path == '/api/v1/auth/password/reset')
        .toList();
    expect(posts, hasLength(1));
    final body = jsonDecode((posts.single as http.Request).body)
        as Map<String, dynamic>;
    expect(body['token'], 'tok-123');
    expect(body['password'], 'NewSecret1');
  });
}
