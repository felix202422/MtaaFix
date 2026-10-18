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
import 'package:mtaafix_mobile/features/map/presentation/map_screen.dart';

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  AuthController controller(MockClient client) => AuthController(
        ApiClient(
          baseUrl: 'http://map.test/api/v1',
          client: client,
          tokenStore: InMemoryTokenStore(),
        ),
      );

  MockClient api({
    http.Response Function(Uri url)? onPublicFeed,
  }) =>
      MockClient((request) async {
        final path = request.url.path;
        if (path == '/api/v1/categories/public') {
          return http.Response(
            jsonEncode([
              {'id': 'c1', 'name': 'Roads', 'active': true},
            ]),
            200,
            headers: jsonHeaders(),
          );
        }
        if (path == '/api/v1/reports/public') {
          if (onPublicFeed != null) return onPublicFeed(request.url);
          return http.Response(
            jsonEncode({
              'content': [
                {
                  'code': 'MTF-0001',
                  'title': 'Pothole near the market',
                  'status': 'SUBMITTED',
                  'categoryName': 'Roads',
                  'latitude': -1.29,
                  'longitude': 36.82,
                  'reportedAt': '2026-10-01T09:00:00Z',
                },
              ],
              'totalElements': 1,
              'totalPages': 1,
              'page': 0,
              'size': 50,
            }),
            200,
            headers: jsonHeaders(),
          );
        }
        return http.Response(jsonEncode({'message': 'not mocked: $path'}),
            404,
            headers: jsonHeaders());
      });

  Future<void> pumpMap(
    WidgetTester tester,
    AuthController auth,
  ) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          home: const MapScreen(),
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets('renders the map with filters and report markers',
      (tester) async {
    await pumpMap(tester, controller(api()));

    expect(find.text('Community Map'), findsOneWidget);
    expect(find.text('Status'), findsOneWidget);
    expect(find.text('Category'), findsOneWidget);
    expect(find.byIcon(Icons.location_pin), findsOneWidget);
    expect(find.byTooltip('My location'), findsOneWidget);
    expect(find.byTooltip('Reports on screen'), findsOneWidget);
  });

  testWidgets('passes the bbox of the visible viewport', (tester) async {
    Uri? seen;
    await pumpMap(
      tester,
      controller(api(onPublicFeed: (url) {
        seen = url;
        return http.Response(
          jsonEncode({
            'content': <dynamic>[],
            'totalElements': 0,
            'totalPages': 0,
            'page': 0,
            'size': 50,
          }),
          200,
          headers: jsonHeaders(),
        );
      })),
    );

    expect(seen, isNotNull);
    expect(seen!.queryParameters['bbox'], isNotNull);
    final parts = seen!.queryParameters['bbox']!.split(',');
    expect(parts, hasLength(4));
    final west = double.parse(parts[0]);
    final south = double.parse(parts[1]);
    final east = double.parse(parts[2]);
    final north = double.parse(parts[3]);
    expect(west, lessThan(east));
    expect(south, lessThan(north));
  });

  testWidgets('shows a retryable error when the public feed fails',
      (tester) async {
    final failing = controller(MockClient((request) async {
      if (request.url.path == '/api/v1/categories/public') {
        return http.Response(jsonEncode(<dynamic>[]), 200,
            headers: jsonHeaders());
      }
      return http.Response(jsonEncode({'message': 'down'}), 500,
          headers: jsonHeaders());
    }));

    await pumpMap(tester, failing);

    expect(find.text('Could not load the map feed.'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });
}
