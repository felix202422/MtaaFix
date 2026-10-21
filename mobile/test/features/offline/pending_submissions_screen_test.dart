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
import 'package:mtaafix_mobile/features/offline/presentation/pending_submissions_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_form_screen.dart';

class FakeConnectivity implements ConnectivitySource {
  FakeConnectivity({this.online = true});

  bool online;

  @override
  Future<bool> isOnline() async => online;

  @override
  Stream<bool> get onOnlineChange => const Stream<bool>.empty();
}

/// A real on-disk image so Image.file thumbnails can decode during tests.
File? gPhoto;

PendingSubmission entry({
  String localId = 'local-1',
  PendingSubmissionState state = PendingSubmissionState.pending,
  String? lastError,
  List<String>? photoPaths,
}) {
  final now = DateTime.now();
  return PendingSubmission(
    localId: localId,
    idempotencyKey: 'idem-key-000000000000000$localId',
    categoryId: 'c1',
    categoryName: 'Roads',
    title: 'Blocked drain',
    description: null,
    latitude: -6.16,
    longitude: 35.75,
    photoPaths: photoPaths ?? <String>[if (gPhoto != null) gPhoto!.path],
    state: state,
    lastError: lastError,
    createdAt: now,
    updatedAt: now,
  );
}

void main() {
  late Directory tempDir;
  late FakeConnectivity fake;
  late OfflineController controller;
  late AuthController auth;

  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  /// Lets real dart:io work (outbox writes/deletes) finish: [runAsync] turns
  /// let I/O complete, [pump] flushes the fake-async-zone callbacks.
  Future<void> settleReal(WidgetTester tester, {int rounds = 8}) async {
    for (var i = 0; i < rounds; i++) {
      await tester.runAsync(
          () => Future<void>.delayed(const Duration(milliseconds: 40)));
      await tester.pump();
    }
    await tester.pumpAndSettle();
  }

  Future<AuthController> signedInAuth() async {
    final controller = AuthController(
      ApiClient(
        baseUrl: 'http://pending.test/api/v1',
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
          if (path == '/api/v1/categories/public') {
            return http.Response(
              jsonEncode([
                {'id': 'c1', 'name': 'Roads', 'active': true},
              ]),
              200,
              headers: jsonHeaders(),
            );
          }
          return http.Response(
            jsonEncode({'message': 'not mocked: $path'}),
            404,
            headers: jsonHeaders(),
          );
        }),
        tokenStore: InMemoryTokenStore(),
      ),
    );
    final ok = await controller.login('citizen@example.com', 'Password1!');
    expect(ok, isTrue);
    return controller;
  }

  Future<void> pumpScreen(WidgetTester tester) async {
    auth = await signedInAuth();
    await tester.pumpWidget(
      OfflineScope(
        controller: controller,
        child: AuthScope(
          controller: auth,
          child: MaterialApp(
            home: const PendingSubmissionsScreen(),
            onGenerateRoute: (settings) =>
                generateRoute(settings, auth: auth),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
    tempDir = Directory.systemTemp.createTempSync('mtaafix_pending_test_');
    gPhoto = File('${tempDir.path}${Platform.pathSeparator}sample.png')
      ..writeAsBytesSync(base64Decode(
          'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='));
    fake = FakeConnectivity();
    controller = OfflineController(
      storage: OfflineStorage(baseDir: tempDir),
      connectivity: ConnectivityService(source: fake),
    );
  });

  tearDown(() {
    controller.dispose();
    gPhoto = null;
    tempDir.deleteSync(recursive: true);
  });

  testWidgets('shows the empty state when nothing is queued', (tester) async {
    await tester.runAsync(controller.start);
    await pumpScreen(tester);

    expect(find.text('Nothing pending'), findsOneWidget);
    expect(find.text('Pending reports'), findsOneWidget);
  });

  testWidgets('lists a queued report as not submitted', (tester) async {
    await tester.runAsync(controller.start);
    await tester.runAsync(() => controller.enqueue(entry()));
    await pumpScreen(tester);

    expect(find.text('Blocked drain'), findsOneWidget);
    expect(find.text('Not yet submitted'), findsOneWidget);
    expect(find.text('Waiting'), findsOneWidget);
    expect(find.textContaining('1 photo(s) pending'), findsOneWidget);
    expect(find.textContaining('not been submitted'), findsNothing);
  });

  testWidgets('discarding asks for confirmation and removes the entry',
      (tester) async {
    await tester.runAsync(controller.start);
    await tester.runAsync(() => controller.enqueue(entry()));
    await pumpScreen(tester);

    await tester.tap(find.widgetWithText(TextButton, 'Discard'));
    await tester.pumpAndSettle();
    expect(find.text('Discard this draft?'), findsOneWidget);
    expect(
      find.text('This report has not been submitted and will be lost.'),
      findsOneWidget,
    );

    final confirm = find.descendant(
      of: find.byType(AlertDialog),
      matching: find.widgetWithText(TextButton, 'Discard'),
    );
    await tester.tap(confirm);
    await tester.pumpAndSettle();
    await settleReal(tester);

    expect(controller.items, isEmpty);
    expect(find.text('Nothing pending'), findsOneWidget);
  });

  testWidgets('failed entries offer Edit which opens the prefill form',
      (tester) async {
    await tester.runAsync(controller.start);
    await tester.runAsync(() => controller.enqueue(entry(
      state: PendingSubmissionState.failed,
      lastError: 'Title is too long',
    )));
    await pumpScreen(tester);

    expect(find.text('Needs attention'), findsOneWidget);
    expect(find.text('Title is too long'), findsOneWidget);

    // The route guard sends signed-in users to the form with the entry.
    await tester.tap(find.widgetWithText(TextButton, 'Edit'));
    await tester.pump();
    await settleReal(tester);
    expect(find.text('Report an Issue'), findsOneWidget);
    // Prefill landed in the title field (the Review section also echoes the
    // title, so assert on the field itself).
    expect(
      find.descendant(
        of: find.byType(ReportFormScreen),
        matching: find.widgetWithText(TextFormField, 'Blocked drain'),
      ),
      findsOneWidget,
    );
  });

  testWidgets('shows the offline hint and disables retry while offline',
      (tester) async {
    fake.online = false;
    await tester.runAsync(controller.start);
    await tester.runAsync(() => controller.enqueue(entry()));
    await pumpScreen(tester);

    expect(
      find.textContaining('You are offline'),
      findsOneWidget,
    );
    final retry = tester.widget<FloatingActionButton>(
      find.byType(FloatingActionButton),
    );
    expect(retry.onPressed, isNull);
    expect(find.text('Waiting for connection'), findsOneWidget);
  });
}
