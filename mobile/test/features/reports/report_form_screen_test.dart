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
import 'package:mtaafix_mobile/features/reports/domain/report.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_form_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  http.Response categoriesResponse() => http.Response(
        jsonEncode([
          {'id': 'c1', 'name': 'Roads', 'active': true},
          {'id': 'c2', 'name': 'Water', 'active': true},
        ]),
        200,
        headers: jsonHeaders(),
      );

  MockClient api() => MockClient((request) async {
        final path = request.url.path;
        if (path == '/api/v1/categories/public') {
          return categoriesResponse();
        }
        if (request.method == 'POST' && path == '/api/v1/reports') {
          return http.Response(
            jsonEncode({
              'id': 'r1',
              'code': 'MTF-0001',
              'title': 'x',
              'status': 'SUBMITTED',
            }),
            201,
            headers: jsonHeaders(),
          );
        }
        return http.Response(
          jsonEncode({'message': 'not mocked: $path'}),
          404,
          headers: jsonHeaders(),
        );
      });

  AuthController controller(MockClient client) => AuthController(
        ApiClient(
          baseUrl: 'http://form.test/api/v1',
          client: client,
          tokenStore: InMemoryTokenStore(),
        ),
      );

  Future<void> pumpForm(WidgetTester tester, AuthController auth) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const ReportFormScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  Future<void> pickCategory(WidgetTester tester, String name) async {
    await tester.tap(find.byType(DropdownButtonFormField<Category>));
    await tester.pumpAndSettle();
    await tester.tap(find.text(name));
    await tester.pumpAndSettle();
  }

  Future<void> submitForm(WidgetTester tester) async {
    final submit = find.widgetWithText(ElevatedButton, 'Submit report');
    await tester.ensureVisible(submit);
    await tester.pump();
    await tester.tap(submit);
    await tester.pumpAndSettle();
  }

  testWidgets('shows the creation sections and field validation errors',
      (tester) async {
    await pumpForm(tester, controller(api()));

    expect(find.text('Category *'), findsOneWidget);
    expect(find.text('Title *'), findsOneWidget);
    expect(find.text('Review'), findsOneWidget);
    expect(find.text('Location *'), findsOneWidget);
    expect(find.text('Photos (0/5)'), findsOneWidget);

    await submitForm(tester);

    expect(find.text('Choose a category'), findsOneWidget);
    expect(find.text('A title is required'), findsOneWidget);
  });

  testWidgets('category dropdown lists the public categories', (tester) async {
    await pumpForm(tester, controller(api()));

    await tester.tap(find.byType(DropdownButtonFormField<Category>));
    await tester.pumpAndSettle();

    expect(find.text('Roads'), findsOneWidget);
    expect(find.text('Water'), findsOneWidget);
  });

  testWidgets('empty title fails validation even with a category',
      (tester) async {
    await pumpForm(tester, controller(api()));

    await pickCategory(tester, 'Roads');
    await submitForm(tester);

    expect(find.text('A title is required'), findsOneWidget);
    expect(find.text('Choose a category'), findsNothing);
  });

  testWidgets('valid fields still require a location pin', (tester) async {
    await pumpForm(tester, controller(api()));

    await pickCategory(tester, 'Roads');
    await tester.enterText(
      find.byType(TextFormField).first,
      'Pothole near market',
    );
    await tester.pump();

    await submitForm(tester);

    expect(
      find.text('Add the issue location (current location or map pin).'),
      findsOneWidget,
    );
    expect(find.text('A title is required'), findsNothing);
  });
}
