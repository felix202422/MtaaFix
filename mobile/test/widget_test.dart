import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mtaafix_mobile/app.dart';
import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';

Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

/// Benign payloads for the read APIs the home shell fires on first build.
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

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets(
      'boots to sign-in when signed out, then signs in to the home shell',
      (tester) async {
    final store = InMemoryTokenStore();
    final api = ApiClient(
      baseUrl: 'http://widget.test/api/v1',
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
        return http.Response(
          jsonEncode({'message': 'not mocked: $path'}),
          404,
          headers: jsonHeaders(),
        );
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

    // Signed in → the five-tab home shell.
    expect(find.byType(MainShellScreen), findsOneWidget);
    expect(store.accessToken, 'access-1');

    // The Reports tab shows My Reports.
    await tester.tap(find.text('Reports'));
    await tester.pumpAndSettle();
    expect(find.byType(ReportListScreen), findsOneWidget);
    expect(find.text('My Reports'), findsOneWidget);
  });
}
