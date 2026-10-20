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

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  Map<String, dynamic> reportJson({
    String status = 'SUBMITTED',
    List<Map<String, dynamic>> statusHistory = const [],
    List<Map<String, dynamic>> comments = const [],
    List<Map<String, dynamic>> media = const [],
    String? description,
  }) =>
      {
        'id': 'r1',
        'code': 'MTF-0001',
        'title': 'Pothole near the market',
        'description': description ?? 'Deep pothole by the bus stop',
        'status': status,
        'location': {'latitude': -1.29, 'longitude': 36.82},
        'reportedAt': '2026-10-01T09:00:00Z',
        'categoryId': 'cat-1',
        'statusHistory': statusHistory,
        'media': media,
        'comments': comments,
      };

  /// Serves GET/POST /reports/r1 and its sub-resources from [currentJson].
  MockClient api(Map<String, dynamic> Function() currentJson) =>
      MockClient((request) async {
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
        if (request.method == 'GET' && path == '/api/v1/reports/r1') {
          return http.Response(jsonEncode(currentJson()), 200,
              headers: jsonHeaders());
        }
        if (request.method == 'POST' &&
            (path == '/api/v1/reports/r1/cancel' ||
                path == '/api/v1/reports/r1/confirm' ||
                path == '/api/v1/reports/r1/dispute' ||
                path == '/api/v1/reports/r1/resubmit' ||
                path == '/api/v1/reports/r1/comments')) {
          return http.Response(jsonEncode(currentJson()), 200,
              headers: jsonHeaders());
        }
        return http.Response(
          jsonEncode({'message': 'not mocked: $path'}),
          404,
          headers: jsonHeaders(),
        );
      });

  Future<AuthController> signedIn(MockClient client) async {
    final auth = AuthController(
      ApiClient(
        baseUrl: 'http://detail.test/api/v1',
        client: client,
        tokenStore: InMemoryTokenStore(),
      ),
    );
    final ok = await auth.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);
    return auth;
  }

  Future<void> pumpDetail(WidgetTester tester, AuthController auth) async {
    await tester.pumpWidget(
      AuthScope(
        controller: auth,
        child: MaterialApp(
          onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
          home: _PushDetailLauncher(),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
  });

  testWidgets('shows the summary, timeline, location and comments',
      (tester) async {
    final auth = await signedIn(api(() => reportJson(
          statusHistory: [
            {
              'fromStatus': 'SUBMITTED',
              'toStatus': 'UNDER_REVIEW',
              'timestamp': '2026-10-01T10:00:00Z',
              'comment': 'Moderator picked this up',
            },
            {
              'fromStatus': 'UNDER_REVIEW',
              'toStatus': 'SUBMITTED',
              'timestamp': '2026-10-01T11:00:00Z',
            },
          ],
          comments: [
            {
              'id': 'c1',
              'content': 'Any update on this?',
              'timestamp': '2026-10-01T12:00:00Z',
            },
          ],
        )));

    await pumpDetail(tester, auth);

    expect(find.text('MTF-0001'), findsOneWidget);
    expect(find.text('Pothole near the market'), findsOneWidget);
    expect(find.text('Deep pothole by the bus stop'), findsOneWidget);
    expect(find.text('Submitted'), findsWidgets);
    expect(find.text('Status timeline'), findsOneWidget);
    expect(find.text('Under review'), findsWidgets);
    expect(find.text('Moderator picked this up'), findsOneWidget);
    expect(find.text('Comments (1)'), findsOneWidget);
    expect(find.text('Any update on this?'), findsOneWidget);
    expect(find.text('Lat -1.29000, Lng 36.82000'), findsOneWidget);
  });

  testWidgets('SUBMITTED reports can be cancelled and edited',
      (tester) async {
    final auth = await signedIn(api(() => reportJson()));

    await pumpDetail(tester, auth);

    expect(find.text('Cancel report'), findsOneWidget);
    expect(find.byTooltip('Edit report'), findsOneWidget);
    expect(find.text('Confirm resolution'), findsNothing);
    expect(find.text('Dispute resolution'), findsNothing);
    expect(find.text('Resubmit report'), findsNothing);
  });

  testWidgets('RESOLVED reports offer confirm and dispute, not cancel',
      (tester) async {
    final auth = await signedIn(api(() => reportJson(status: 'RESOLVED')));

    await pumpDetail(tester, auth);

    expect(find.text('Confirm resolution'), findsOneWidget);
    expect(find.text('Dispute resolution'), findsOneWidget);
    expect(find.text('Cancel report'), findsNothing);
    expect(find.text('Resubmit report'), findsNothing);
  });

  testWidgets('NEEDS_INFORMATION reports offer resubmit', (tester) async {
    final auth = await signedIn(api(() => reportJson(status: 'NEEDS_INFORMATION')));

    await pumpDetail(tester, auth);

    expect(find.text('Resubmit report'), findsOneWidget);
    expect(find.text('Cancel report'), findsNothing);
    expect(find.text('Confirm resolution'), findsNothing);
  });

  testWidgets('posting a comment refreshes the comment list', (tester) async {
    var commentCount = 1;
    Map<String, dynamic> payload() => reportJson(
          comments: [
            {
              'id': 'c1',
              'content': 'Any update on this?',
              'timestamp': '2026-10-01T12:00:00Z',
            },
            if (commentCount > 1)
              {
                'id': 'c2',
                'content': 'Contractor is on the way',
                'timestamp': '2026-10-01T13:00:00Z',
              },
          ],
        );

    final auth = await signedIn(MockClient((request) async {
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
      if (request.method == 'GET' && path == '/api/v1/reports/r1') {
        return http.Response(jsonEncode(payload()), 200,
            headers: jsonHeaders());
      }
      if (request.method == 'POST' && path == '/api/v1/reports/r1/comments') {
        commentCount = 2;
        return http.Response(jsonEncode(payload()), 200,
            headers: jsonHeaders());
      }
      return http.Response(jsonEncode({'message': 'not mocked: $path'}),
          404,
          headers: jsonHeaders());
    }));

    await pumpDetail(tester, auth);

    expect(find.text('Comments (1)'), findsOneWidget);

    await tester.enterText(
      find.byType(TextField),
      'Contractor is on the way',
    );
    final send = find.byIcon(Icons.send_rounded);
    await tester.ensureVisible(send);
    await tester.tap(send);
    await tester.pumpAndSettle();

    expect(find.text('Comments (2)'), findsOneWidget);
    expect(find.text('Contractor is on the way'), findsOneWidget);
  });

  testWidgets('cancel requires confirmation before calling the API',
      (tester) async {
    final auth = await signedIn(api(() => reportJson()));

    await pumpDetail(tester, auth);

    final cancel = find.widgetWithText(OutlinedButton, 'Cancel report');
    await tester.ensureVisible(cancel);
    await tester.tap(cancel);
    await tester.pumpAndSettle();

    expect(find.text('Cancel this report?'), findsOneWidget);
    expect(find.text('Keep report'), findsOneWidget);

    await tester.tap(find.text('Keep report'));
    await tester.pumpAndSettle();

    expect(find.text('Cancel this report?'), findsNothing);
    expect(find.text('Cancel report'), findsOneWidget);
  });

  testWidgets('confirms a resolution with optional feedback', (tester) async {
    final auth = await signedIn(api(() => reportJson(status: 'RESOLVED')));

    await pumpDetail(tester, auth);

    final confirm = find.widgetWithText(ElevatedButton, 'Confirm resolution');
    await tester.ensureVisible(confirm);
    await tester.tap(confirm);
    await tester.pumpAndSettle();

    expect(find.text('Confirm resolution'), findsWidgets);
    expect(find.text('Skip'), findsOneWidget);

    await tester.tap(find.text('Skip'));
    await tester.pumpAndSettle();

    expect(find.text('Thanks — resolution confirmed.'), findsOneWidget);
  });

  testWidgets('dispute requires a reason', (tester) async {
    final auth = await signedIn(api(() => reportJson(status: 'RESOLVED')));

    await pumpDetail(tester, auth);

    final dispute = find.widgetWithText(OutlinedButton, 'Dispute resolution');
    await tester.ensureVisible(dispute);
    await tester.tap(dispute);
    await tester.pumpAndSettle();

    expect(find.text('Dispute resolution'), findsWidgets);
    expect(find.text('A dispute reason is required.'), findsNothing);

    await tester.tap(find.text('Submit dispute'));
    await tester.pumpAndSettle();

    expect(find.text('A dispute reason is required.'), findsOneWidget);

    await tester.enterText(
      find.descendant(
        of: find.byType(AlertDialog),
        matching: find.byType(TextField),
      ),
      'The road is still flooded',
    );
    await tester.tap(find.text('Submit dispute'));
    await tester.pumpAndSettle();

    expect(find.text('Dispute submitted.'), findsOneWidget);
  });

  testWidgets('shows a friendly error when the report cannot load',
      (tester) async {
    final auth = await signedIn(MockClient((request) async {
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
          headers: jsonHeaders(),
        );
      }
      return http.Response(jsonEncode({'message': 'gone'}), 404,
          headers: jsonHeaders());
    }));

    await pumpDetail(tester, auth);

    expect(find.text('Report unavailable'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });

  testWidgets('active reports show the evidence section with an add button',
      (tester) async {
    final auth = await signedIn(api(() => reportJson()));

    await pumpDetail(tester, auth);

    expect(find.text('Evidence (0)'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Add photo'), findsOneWidget);
  });

  testWidgets('closed reports hide the evidence section entirely',
      (tester) async {
    final auth = await signedIn(api(() => reportJson(status: 'RESOLVED')));

    await pumpDetail(tester, auth);

    expect(find.text('Evidence (0)'), findsNothing);
    expect(find.widgetWithText(OutlinedButton, 'Add photo'), findsNothing);
  });

  testWidgets('removing a photo confirms, calls DELETE and reloads',
      (tester) async {
    var withMedia = true;
    Map<String, dynamic> payload() => reportJson(
          media: withMedia
              ? <Map<String, dynamic>>[
                  {
                    'id': 'm1',
                    'fileName': 'pothole.jpg',
                    'contentType': 'image/jpeg',
                    'url': '/api/v1/media/m1/content',
                  },
                ]
              : <Map<String, dynamic>>[],
        );
    final hits = <String>[];

    final auth = await signedIn(MockClient((request) async {
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
      if (request.method == 'GET' && path == '/api/v1/reports/r1') {
        return http.Response(jsonEncode(payload()), 200,
            headers: jsonHeaders());
      }
      if (request.method == 'DELETE' && path == '/api/v1/media/m1') {
        hits.add('DELETE $path');
        withMedia = false;
        return http.Response('{}', 200, headers: jsonHeaders());
      }
      return http.Response(jsonEncode({'message': 'not mocked: $path'}),
          404,
          headers: jsonHeaders());
    }));

    await pumpDetail(tester, auth);

    expect(find.text('Evidence (1)'), findsOneWidget);
    final remove = find.byTooltip('Remove photo');
    expect(remove, findsOneWidget);

    await tester.ensureVisible(remove);
    await tester.pumpAndSettle();
    await tester.tap(remove);
    await tester.pumpAndSettle();

    expect(find.text('Remove this photo?'), findsOneWidget);
    expect(find.text('The photo is deleted permanently. The report itself is not affected.'),
        findsOneWidget);

    await tester.tap(find.widgetWithText(TextButton, 'Remove'));
    await tester.pumpAndSettle();

    expect(hits, ['DELETE /api/v1/media/m1']);
    expect(find.text('Photo removed.'), findsOneWidget);
    expect(find.text('Evidence (0)'), findsOneWidget);
    expect(find.text('Evidence (1)'), findsNothing);
  });
}

/// Pushes the report detail route with a fixed id after the first frame.
class _PushDetailLauncher extends StatefulWidget {
  @override
  State<_PushDetailLauncher> createState() => _PushDetailLauncherState();
}

class _PushDetailLauncherState extends State<_PushDetailLauncher> {
  bool _pushed = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_pushed) return;
    _pushed = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      Navigator.of(context)
          .pushNamed(Routes.reportDetail, arguments: 'r1');
    });
  }

  @override
  Widget build(BuildContext context) => const SizedBox.shrink();
}
