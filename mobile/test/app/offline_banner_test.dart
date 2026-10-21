import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/offline/application/connectivity_service.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_controller.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_scope.dart';
import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';
import 'package:mtaafix_mobile/features/offline/presentation/pending_submissions_screen.dart';

class FakeConnectivity implements ConnectivitySource {
  FakeConnectivity({this.online = true});

  bool online;
  final _events = StreamController<bool>.broadcast();

  @override
  Future<bool> isOnline() async => online;

  @override
  Stream<bool> get onOnlineChange => _events.stream;

  void setOnline(bool value) {
    if (online == value) return;
    online = value;
    _events.add(value);
  }
}

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

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

  Future<AuthController> signedInAuth() async {
    final controller = AuthController(
      ApiClient(
        baseUrl: 'http://banner.test/api/v1',
        client: MockClient((request) async {
          final path = request.url.path;
          if (path == '/api/v1/auth/login') {
            return http.Response(
              jsonEncode({
                'accessToken': 'access-1',
                'refreshToken': 'refresh-1',
                'user': {
                  'id': 'u1',
                  'email': 'citizen@example.com',
                  'fullName': 'Asha Mrema',
                  'role': 'CITIZEN',
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
            return http.Response(jsonEncode(<dynamic>[]), 200,
                headers: jsonHeaders());
          }
          if (path == '/api/v1/notifications/unread-count') {
            return http.Response(jsonEncode({'count': 0}), 200,
                headers: jsonHeaders());
          }
          if (path == '/api/v1/dashboard/me') {
            return http.Response(jsonEncode({'stats': {}}), 200,
                headers: jsonHeaders());
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

  late Directory tempDir;
  late FakeConnectivity fake;
  late OfflineController offline;

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
    tempDir = Directory.systemTemp.createTempSync('mtaafix_banner_test_');
    fake = FakeConnectivity();
    offline = OfflineController(
      storage: OfflineStorage(baseDir: tempDir),
      connectivity: ConnectivityService(source: fake),
    );
  });

  tearDown(() {
    offline.dispose();
    tempDir.deleteSync(recursive: true);
  });

  Future<void> pumpShell(WidgetTester tester, AuthController auth) async {
    await tester.pumpWidget(
      OfflineScope(
        controller: offline,
        child: AuthScope(
          controller: auth,
          child: MaterialApp(
            home: const MainShellScreen(),
            onGenerateRoute: (settings) => generateRoute(settings, auth: auth),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('shows the offline strip only while offline', (tester) async {
    fake.online = false;
    await tester.runAsync(offline.start);
    final auth = await signedInAuth();
    await pumpShell(tester, auth);

    expect(
      find.text(
          'Offline — new reports are saved and sent when you reconnect.'),
      findsOneWidget,
    );

    fake.setOnline(true);
    await tester.pumpAndSettle();
    expect(
      find.text(
          'Offline — new reports are saved and sent when you reconnect.'),
      findsNothing,
    );
  });

  testWidgets('reports tab banners unsent work and opens the queue',
      (tester) async {
    await tester.runAsync(offline.start);
    final now = DateTime.now();
    await tester.runAsync(() => offline.enqueue(PendingSubmission(
      localId: 'local-1',
      idempotencyKey: 'idem-key-0000000000000001',
      categoryId: 'c1',
      categoryName: 'Roads',
      title: 'Broken streetlight',
      description: null,
      latitude: -6.16,
      longitude: 35.75,
      photoPaths: const [],
      state: PendingSubmissionState.failed,
      lastError: 'Rejected by the server',
      createdAt: now,
      updatedAt: now,
    )));

    final auth = await signedInAuth();
    await pumpShell(tester, auth);

    await tester.tap(find.text('Reports'));
    await tester.pumpAndSettle();

    expect(find.text('My Reports'), findsOneWidget);
    expect(
      find.text('1 report not yet submitted — tap to view'),
      findsOneWidget,
    );
    // Unsent work never masquerades as a filed report in the list.
    expect(find.text('Broken streetlight'), findsNothing);

    await tester.tap(find.text('1 report not yet submitted — tap to view'));
    await tester.pumpAndSettle();

    expect(find.byType(PendingSubmissionsScreen), findsOneWidget);
    expect(find.text('Broken streetlight'), findsOneWidget);
    expect(find.text('Rejected by the server'), findsOneWidget);
  });

  testWidgets('no banner appears when the queue is empty', (tester) async {
    await tester.runAsync(offline.start);
    final auth = await signedInAuth();
    await pumpShell(tester, auth);

    await tester.tap(find.text('Reports'));
    await tester.pumpAndSettle();

    expect(find.textContaining('not yet submitted'), findsNothing);
  });
}
