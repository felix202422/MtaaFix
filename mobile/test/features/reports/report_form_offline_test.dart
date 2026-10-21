import 'dart:convert';
import 'dart:io';

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
import 'package:mtaafix_mobile/features/offline/application/connectivity_service.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_controller.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_scope.dart';
import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';
import 'package:mtaafix_mobile/features/offline/domain/report_draft.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_form_screen.dart';

class FakeConnectivity implements ConnectivitySource {
  FakeConnectivity({this.online = true});

  bool online;

  @override
  Future<bool> isOnline() async => online;

  @override
  Stream<bool> get onOnlineChange => const Stream<bool>.empty();
}

void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  /// Lets real dart:io work started by widget code (drafts, outbox writes)
  /// finish: [runAsync] turns let I/O complete, [pump] flushes the callbacks
  /// queued in the test's fake-async zone.
  Future<void> settleReal(WidgetTester tester, {int rounds = 8}) async {
    for (var i = 0; i < rounds; i++) {
      await tester.runAsync(
          () => Future<void>.delayed(const Duration(milliseconds: 40)));
      await tester.pump();
    }
    await tester.pumpAndSettle();
  }

  late Directory tempDir;
  late FakeConnectivity fake;
  late OfflineController offline;

  /// [categoriesOk] controls the categories endpoint (500 → cache path).
  MockClient api({bool categoriesOk = true}) => MockClient((request) async {
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
        if (path == '/api/v1/categories/public') {
          if (!categoriesOk) {
            return http.Response(
              jsonEncode({'message': 'boom'}),
              500,
              headers: jsonHeaders(),
            );
          }
          return http.Response(
            jsonEncode([
              {'id': 'c1', 'name': 'Roads', 'active': true},
              {'id': 'c2', 'name': 'Water', 'active': true},
            ]),
            200,
            headers: jsonHeaders(),
          );
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
        if (path == '/api/v1/reports/r1') {
          return http.Response(
            jsonEncode({
              'id': 'r1',
              'code': 'MTF-0001',
              'title': 'x',
              'status': 'SUBMITTED',
            }),
            200,
            headers: jsonHeaders(),
          );
        }
        return http.Response(
          jsonEncode({'message': 'not mocked: $path'}),
          404,
          headers: jsonHeaders(),
        );
      });

  AuthController auth(MockClient client) => AuthController(
        ApiClient(
          baseUrl: 'http://formoffline.test/api/v1',
          client: client,
          tokenStore: InMemoryTokenStore(),
        ),
      );

  /// Signed-in controller so the guarded `/reports/new` route resolves.
  Future<AuthController> signedInAuth() async {
    final controller = auth(api());
    final ok = await controller.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);
    return controller;
  }

  Future<void> pumpHost(
    WidgetTester tester,
    AuthController authController, {
    Widget? home,
  }) async {
    await tester.pumpWidget(
      OfflineScope(
        controller: offline,
        child: AuthScope(
          controller: authController,
          child: MaterialApp(
            // A Scaffold like the real shell, so SnackBars raised on the
            // popped form have somewhere to re-render after the pop.
            home: home ?? const Scaffold(body: SizedBox.shrink()),
            onGenerateRoute: (settings) =>
                generateRoute(settings, auth: authController),
          ),
        ),
      ),
    );
    await tester.pump();
    await settleReal(tester);
  }

  Future<void> openForm(
    WidgetTester tester,
    AuthController authController, {
    PendingSubmission? pending,
  }) async {
    final navigator = tester.state<NavigatorState>(find.byType(Navigator));
    navigator.pushNamed(Routes.reportNew, arguments: pending);
    await tester.pump();
    await settleReal(tester);
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
    tempDir = Directory.systemTemp.createTempSync('mtaafix_form_test_');
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

  testWidgets('offers to restore a saved draft and restores it',
      (tester) async {
    await tester.runAsync(offline.start);
    await tester.runAsync(() => offline.saveDraft(ReportDraft(
      categoryId: 'c1',
      categoryName: 'Roads',
      title: 'Drafted title',
      description: 'Drafted description',
      latitude: -6.16,
      longitude: 35.75,
      photoPaths: const [],
      updatedAt: DateTime.now(),
    )));

    await pumpHost(tester, auth(api()), home: const ReportFormScreen());

    expect(find.text('Restore your previous draft?'), findsOneWidget);
    expect(find.text('A draft from your last session was found.'), findsOneWidget);

    await tester.tap(find.widgetWithText(TextButton, 'Restore'));
    await tester.pumpAndSettle();

    expect(find.text('Restore your previous draft?'), findsNothing);
    // The Review section echoes these values, so scope to the input fields.
    expect(
      find.widgetWithText(TextFormField, 'Drafted title'), findsOneWidget);
    expect(
      find.widgetWithText(TextFormField, 'Drafted description'),
      findsOneWidget);
    expect(find.textContaining('-6.16000, 35.75000'), findsWidgets);
  });

  testWidgets('discarding the restore offer deletes the draft',
      (tester) async {
    await tester.runAsync(offline.start);
    await tester.runAsync(() => offline.saveDraft(ReportDraft(
      categoryId: 'c1',
      title: 'Drafted title',
      description: null,
      latitude: 0,
      longitude: 0,
      photoPaths: const [],
      updatedAt: DateTime.now(),
    )));

    await pumpHost(tester, auth(api()), home: const ReportFormScreen());
    expect(find.text('Restore your previous draft?'), findsOneWidget);

    await tester.tap(find.widgetWithText(TextButton, 'Discard'));
    await tester.pump();
    await settleReal(tester);

    expect(find.text('Restore your previous draft?'), findsNothing);
    expect(await tester.runAsync(offline.loadDraft), isNull);
  });

  testWidgets('autosaves a draft while the user types', (tester) async {
    await tester.runAsync(offline.start);
    await pumpHost(tester, auth(api()), home: const ReportFormScreen());

    await tester.enterText(find.byType(TextFormField).first, 'Water outage');
    await tester.pump(const Duration(milliseconds: 600));
    await settleReal(tester);

    final draft = await tester.runAsync(offline.loadDraft);
    expect(draft, isNotNull);
    expect(draft!.title, 'Water outage');
  });

  testWidgets('falls back to cached categories when the network fails',
      (tester) async {
    await tester.runAsync(offline.start);
    await tester.runAsync(() => offline.categories.save([
      {'id': 'c1', 'name': 'Roads', 'active': true},
      {'id': 'c9', 'name': 'Stale only', 'active': true},
    ]));

    await pumpHost(tester, auth(api(categoriesOk: false)),
        home: const ReportFormScreen());

    expect(
      find.text('Offline — showing saved categories, which may be outdated.'),
      findsOneWidget,
    );
    await tester.tap(find.byType(DropdownButtonFormField<Category>));
    await tester.pumpAndSettle();
    expect(find.text('Roads'), findsOneWidget);
    expect(find.text('Stale only'), findsOneWidget);
  });

  testWidgets('an offline submit queues the report instead of failing',
      (tester) async {
    fake.online = false;
    await tester.runAsync(offline.start);
    final old = PendingSubmission(
      localId: 'old-local',
      idempotencyKey: 'idem-key-0000000000000007',
      categoryId: 'c1',
      categoryName: 'Roads',
      title: 'Blocked drain',
      description: 'Overflowing',
      latitude: -6.16,
      longitude: 35.75,
      photoPaths: const [],
      state: PendingSubmissionState.failed,
      lastError: 'Rejected earlier',
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
    await tester.runAsync(() => offline.enqueue(old));

    final authController = await signedInAuth();
    await pumpHost(tester, authController);
    await openForm(tester, authController, pending: old);

    expect(find.text('Report an Issue'), findsOneWidget);
    expect(
      find.widgetWithText(TextFormField, 'Blocked drain'), findsOneWidget);

    await tester.ensureVisible(
        find.widgetWithText(ElevatedButton, 'Submit report'));
    await tester.pump();
    await tester.tap(find.widgetWithText(ElevatedButton, 'Submit report'));
    await tester.pump();
    await settleReal(tester);

    // Back on the base route — the form popped itself.
    expect(find.byType(ReportFormScreen), findsNothing);
    expect(find.textContaining('saved to your pending queue'), findsOneWidget);

    expect(offline.items, hasLength(1),
        reason: 'the failed entry was replaced, not duplicated');
    final queued = offline.items.single;
    expect(queued.localId, isNot('old-local'));
    expect(queued.title, 'Blocked drain');
    expect(queued.state, PendingSubmissionState.pending);
    expect(queued.lastError, isNull);
  });

  testWidgets('an online submit from a prefill files the report and clears '
      'the queue entry', (tester) async {
    await tester.runAsync(offline.start);
    final old = PendingSubmission(
      localId: 'old-local',
      idempotencyKey: 'idem-key-0000000000000008',
      categoryId: 'c1',
      title: 'Blocked drain',
      description: null,
      latitude: -6.16,
      longitude: 35.75,
      photoPaths: const [],
      state: PendingSubmissionState.failed,
      lastError: 'Rejected earlier',
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
    await tester.runAsync(() => offline.enqueue(old));

    final authController = await signedInAuth();
    await pumpHost(tester, authController);
    await openForm(tester, authController, pending: old);

    await tester.ensureVisible(
        find.widgetWithText(ElevatedButton, 'Submit report'));
    await tester.pump();
    await tester.tap(find.widgetWithText(ElevatedButton, 'Submit report'));
    await tester.pump();
    await settleReal(tester);

    expect(find.text('Report MTF-0001 submitted.'), findsOneWidget);
    expect(offline.items, isEmpty,
        reason: 'replaced entry is gone once the server accepted it');
    expect(find.byType(ReportFormScreen), findsNothing);
  });

  testWidgets('a submission keeps an unrelated draft intact',
      (tester) async {
    await tester.runAsync(offline.start);
    // An unfinished draft for a *different* report.
    await tester.runAsync(() => offline.saveDraft(ReportDraft(
      categoryId: 'c2',
      title: 'Other report in progress',
      description: null,
      latitude: 0,
      longitude: 0,
      photoPaths: const [],
      updatedAt: DateTime.now(),
    )));
    final old = PendingSubmission(
      localId: 'old-local',
      idempotencyKey: 'idem-key-0000000000000006',
      categoryId: 'c1',
      title: 'Blocked drain',
      description: null,
      latitude: -6.16,
      longitude: 35.75,
      photoPaths: const [],
      state: PendingSubmissionState.failed,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
    await tester.runAsync(() => offline.enqueue(old));

    final authController = await signedInAuth();
    await pumpHost(tester, authController);
    await openForm(tester, authController, pending: old);
    await tester.ensureVisible(
        find.widgetWithText(ElevatedButton, 'Submit report'));
    await tester.pump();
    await tester.tap(find.widgetWithText(ElevatedButton, 'Submit report'));
    await tester.pump();
    await settleReal(tester);

    expect(find.text('Report MTF-0001 submitted.'), findsOneWidget);
    final draft = await tester.runAsync(offline.loadDraft);
    expect(draft, isNotNull,
        reason: 'prefill submission must not wipe another draft');
    expect(draft!.title, 'Other report in progress');
    expect(offline.items, isEmpty);
  });
}
