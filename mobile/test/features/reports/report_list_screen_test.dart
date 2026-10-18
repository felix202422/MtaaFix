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
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  http.Response pageOf(
    List<Map<String, dynamic>> reports, {
    int totalElements = 0,
    int totalPages = 0,
    int page = 0,
  }) {
    return http.Response(
      jsonEncode({
        'content': reports,
        'totalElements': totalElements,
        'totalPages': totalPages,
        'page': page,
        'size': 20,
      }),
      200,
      headers: jsonHeaders(),
    );
  }

  Map<String, dynamic> report(
    String id, {
    String code = 'MTF-0001',
    String title = 'Pothole near the market',
    String status = 'SUBMITTED',
  }) =>
      {
        'id': id,
        'code': code,
        'title': title,
        'status': status,
        'reportedAt': '2026-10-01T09:00:00Z',
      };

  MockClient api(http.Response Function(Uri url) respond) => MockClient(
        (request) async {
          if (request.url.path == '/api/v1/categories/public') {
            return http.Response(
              jsonEncode([
                {'id': 'c1', 'name': 'Roads', 'active': true},
                {'id': 'c2', 'name': 'Water', 'active': true},
              ]),
              200,
              headers: jsonHeaders(),
            );
          }
          if (request.url.path == '/api/v1/reports') {
            return respond(request.url);
          }
          return http.Response(jsonEncode({'message': 'not mocked'}), 404,
              headers: jsonHeaders());
        },
      );

  AuthController controller(MockClient client) => AuthController(
        ApiClient(
          baseUrl: 'http://list.test/api/v1',
          client: client,
          tokenStore: InMemoryTokenStore(),
        ),
      );

  Future<void> pumpList(
    WidgetTester tester,
    AuthController auth,
  ) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const ReportListScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets('renders report cards with code, title, date and status',
      (tester) async {
    final auth = controller(api((_) => pageOf(
          [
            report('r1'),
            report('r2',
                code: 'MTF-0002',
                title: 'Blocked drain',
                status: 'IN_PROGRESS'),
          ],
          totalElements: 2,
          totalPages: 1,
        )));

    await pumpList(tester, auth);

    expect(find.text('MTF-0001'), findsOneWidget);
    expect(find.text('Pothole near the market'), findsOneWidget);
    expect(find.text('MTF-0002'), findsOneWidget);
    expect(find.text('Blocked drain'), findsOneWidget);
    expect(find.text('2 reports'), findsOneWidget);
    expect(find.text('Submitted'), findsWidgets);
    expect(find.text('In progress'), findsWidgets);
    expect(find.text('1 Oct 2026'), findsWidgets);
  });

  testWidgets('shows the empty state when the citizen has no reports',
      (tester) async {
    final auth = controller(api((_) => pageOf([])));

    await pumpList(tester, auth);

    expect(find.text('No reports yet'), findsOneWidget);
    expect(find.text('Report an Issue'), findsOneWidget);
  });

  testWidgets('shows a retryable error when the list call fails',
      (tester) async {
    final auth = controller(MockClient((request) async {
      if (request.url.path == '/api/v1/categories/public') {
        return http.Response(jsonEncode(<dynamic>[]), 200,
            headers: jsonHeaders());
      }
      return http.Response(jsonEncode({'message': 'down'}), 500,
          headers: jsonHeaders());
    }));

    await pumpList(tester, auth);

    expect(find.text('Reports unavailable'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });

  testWidgets('status filter re-queries the API with the status parameter',
      (tester) async {
    final statusQueries = <String?>[];
    final auth = controller(api((url) {
      statusQueries.add(url.queryParameters['status']);
      return pageOf([]);
    }));

    await pumpList(tester, auth);
    expect(statusQueries, [null]);

    await tester.tap(find.byType(DropdownButtonFormField<String?>).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Under review'));
    await tester.pumpAndSettle();

    expect(statusQueries.last, 'UNDER_REVIEW');
  });

  testWidgets('search submits the q parameter', (tester) async {
    final queries = <String?>[];
    final auth = controller(api((url) {
      queries.add(url.queryParameters['q']);
      return pageOf([]);
    }));

    await pumpList(tester, auth);

    await tester.enterText(find.byType(TextField).first, 'market');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await tester.pumpAndSettle();

    expect(queries.last, 'market');
  });

  testWidgets('category filter re-queries the API with the categoryId',
      (tester) async {
    final categoryQueries = <String?>[];
    final auth = controller(api((url) {
      categoryQueries.add(url.queryParameters['categoryId']);
      return pageOf([]);
    }));

    await pumpList(tester, auth);
    expect(categoryQueries, [null]);

    final dropdowns = find.byType(DropdownButtonFormField<String?>);
    expect(dropdowns, findsNWidgets(2));
    await tester.tap(dropdowns.at(1));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Water'));
    await tester.pumpAndSettle();

    expect(categoryQueries.last, 'c2');
  });

  testWidgets('Load more fetches the next page and appends it',
      (tester) async {
    final auth = controller(api((url) {
      final page = int.parse(url.queryParameters['page'] ?? '0');
      if (page == 0) {
        return pageOf([report('r1')], totalElements: 2, totalPages: 2);
      }
      return pageOf(
        [report('r2', code: 'MTF-0002', title: 'Broken lamp')],
        totalElements: 2,
        totalPages: 2,
        page: 1,
      );
    }));

    await pumpList(tester, auth);
    expect(find.text('Pothole near the market'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Load more'), findsOneWidget);

    await tester.tap(find.widgetWithText(OutlinedButton, 'Load more'));
    await tester.pumpAndSettle();

    expect(find.text('Broken lamp'), findsOneWidget);
    expect(find.text('Pothole near the market'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Load more'), findsNothing);
  });
}
