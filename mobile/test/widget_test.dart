import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:mtaafix_mobile/app.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';

void main() {
  testWidgets(
      'boots to sign-in when signed out, then signs in to My Reports',
      (tester) async {
    final store = InMemoryTokenStore();
    final api = ApiClient(
      baseUrl: 'http://widget.test/api/v1',
      client: MockClient((request) async {
        if (request.url.path == '/api/v1/auth/login') {
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
            headers: {'content-type': 'application/json'},
          );
        }
        fail('Unexpected request: ${request.method} ${request.url}');
      }),
      tokenStore: store,
    );

    await tester.pumpWidget(MtaaFixMobileApp(apiClient: api));
    await tester.pumpAndSettle();

    // Signed out with no stored session → sign-in screen.
    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('Welcome Back'), findsOneWidget);

    await tester.enterText(
      find.byType(TextFormField).first,
      'citizen@example.com',
    );
    await tester.enterText(
      find.byType(TextFormField).at(1),
      'Password1!',
    );
    await tester.tap(find.text('Sign In'));
    await tester.pumpAndSettle();

    expect(find.byType(ReportListScreen), findsOneWidget);
    expect(find.text('My Reports'), findsOneWidget);
    expect(store.accessToken, 'access-1');
  });
}
