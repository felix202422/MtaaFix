import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mtaafix_mobile/app.dart';
import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_detail_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_form_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';

/// Phase 15 quality gates: the critical citizen journey end to end through
/// the real app widget tree, a small-viewport (320x640) traversal that would
/// fail on RenderFlex overflow, and semantics labels on the key controls.
void main() {
  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  http.Response jsonResponse(Object? body, {int status = 200}) =>
      http.Response(jsonEncode(body), status, headers: jsonHeaders());

  http.Response emptyPage() => jsonResponse({
        'content': <dynamic>[],
        'totalElements': 0,
        'totalPages': 0,
        'page': 0,
        'size': 20,
      });

  final createdReport = <String, dynamic>{
    'id': 'r1',
    'code': 'MTF-2026-000042',
    'title': 'Blocked drain on Nyeri Rd',
    'description': 'Water pooling across the road after rain',
    'status': 'SUBMITTED',
    'location': {'latitude': -6.811, 'longitude': 39.279},
    'reportedAt': '2026-10-21T09:00:00Z',
    'categoryId': 'cat-1',
    'statusHistory': <dynamic>[],
    'media': <dynamic>[],
    'comments': <dynamic>[],
  };

  /// Stateful mock: the list stays empty until POST /reports files one.
  MockClient api() {
    var submitted = false;
    return MockClient((request) async {
      final path = request.url.path;
      if (request.method == 'POST' && path == '/api/v1/auth/login') {
        return jsonResponse({
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
        });
      }
      if (path == '/api/v1/dashboard/me') {
        return jsonResponse({
          'stats': {
            'submittedReports': 0,
            'assignedReports': 0,
            'resolvedReports': 0
          }
        });
      }
      if (path == '/api/v1/notifications') return emptyPage();
      if (path.startsWith('/api/v1/reports/public')) return emptyPage();
      if (path == '/api/v1/categories/public') {
        return jsonResponse([
          {'id': 'cat-1', 'name': 'Pothole', 'slug': 'pothole', 'active': true},
          {
            'id': 'cat-2',
            'name': 'Streetlight',
            'slug': 'streetlight',
            'active': true
          },
        ]);
      }
      if (request.method == 'POST' && path == '/api/v1/reports') {
        submitted = true;
        return jsonResponse(createdReport, status: 201);
      }
      if (request.method == 'GET' && path == '/api/v1/reports') {
        return jsonResponse({
          'content': submitted ? [createdReport] : <dynamic>[],
          'totalElements': submitted ? 1 : 0,
          'totalPages': submitted ? 1 : 0,
          'page': 0,
          'size': 20,
        });
      }
      if (request.method == 'GET' && path == '/api/v1/reports/r1') {
        return jsonResponse(createdReport);
      }
      return jsonResponse({'message': 'not mocked: $path'}, status: 404);
    });
  }

  const geoChannel = MethodChannel('flutter.baseflow.com/geolocator');
  const connectivityMethod =
      MethodChannel('dev.fluttercommunity.plus/connectivity');
  const connectivityEvents =
      EventChannel('dev.fluttercommunity.plus/connectivity_status');
  const pathProviderChannel = MethodChannel('plugins.flutter.io/path_provider');

  late Directory tempCacheDir;

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding.seen': true});
    tempCacheDir = Directory.systemTemp.createTempSync('p15-quality-');
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(pathProviderChannel, (call) async =>
            tempCacheDir.path); // storage fallbacks for widget tests
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(connectivityMethod,
            (call) async => ['wifi']); // initial check: online
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockStreamHandler(
      connectivityEvents,
      MockStreamHandler.inline(
        onListen: (args, events) {
          // Emit online once and keep the stream open — closing it makes the
          // connectivity service resubscribe in a loop under test.
          events.success(['wifi']);
        },
        onCancel: (args) {},
      ),
    );
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(geoChannel, (call) async {
      switch (call.method) {
        case 'checkPermission':
        case 'requestPermission':
          return 2; // LocationPermission.whileInUse
        case 'isLocationServiceEnabled':
          return true;
        case 'getCurrentPosition':
          return {
            'latitude': -6.811,
            'longitude': 39.279,
            'timestamp': 1760000000000,
            'accuracy': 5.0,
            'altitude': 1130.0,
            'heading': 90.0,
            'speed': 0.0,
            'altitudeAccuracy': 6.0,
            'headingAccuracy': 3.0,
            'speedAccuracy': 1.0,
          };
      }
      return null;
    });
  });

  tearDown(() {
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(geoChannel, null);
    messenger.setMockMethodCallHandler(connectivityMethod, null);
    messenger.setMockStreamHandler(connectivityEvents, null);
    messenger.setMockMethodCallHandler(pathProviderChannel, null);
    if (tempCacheDir.existsSync()) {
      tempCacheDir.deleteSync(recursive: true);
    }
  });

  /// Lets the 400 ms draft debounce fire, root-zone file IO (draft autosave,
  /// offline queue) finish, and the UI drain - the fake-async pattern locked
  /// in during Phase 14.
  Future<void> quiesce(WidgetTester tester) async {
    await tester.pump(const Duration(milliseconds: 500));
    for (var i = 0; i < 8; i++) {
      await tester.runAsync(
          () => Future<void>.delayed(const Duration(milliseconds: 40)));
      await tester.pump();
    }
    await tester.pumpAndSettle(const Duration(milliseconds: 100),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 5));
  }

  Future<void> signIn(WidgetTester tester) async {
    await tester.enterText(
        find.byType(TextFormField).at(0), 'citizen@example.com');
    await tester.enterText(find.byType(TextFormField).at(1), 'Password1!');
    await tester.tap(find.text('Sign In'));
    await tester.pump();
    await quiesce(tester);
    expect(find.byType(MainShellScreen), findsOneWidget);
  }

  Future<void> fillAndSubmit(WidgetTester tester) async {
    await tester.tap(find.text('Category *'), warnIfMissed: false);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Pothole'));
    await tester.pump();
    await quiesce(tester);

    await tester.enterText(
        find.byType(TextFormField).at(0), 'Blocked drain on Nyeri Rd');
    await tester.enterText(find.byType(TextFormField).at(1),
        'Water pooling across the road after rain');
    await quiesce(tester);

    await tester.tap(find.text('Current location'));
    await tester.pump();
    await quiesce(tester);
    expect(find.text('-6.81100, 39.27900'), findsWidgets);

    final submit = find.widgetWithText(ElevatedButton, 'Submit report');
    await tester.ensureVisible(submit);
    await tester.pump();
    await tester.tap(submit);
    await tester.pump();
    await quiesce(tester);
  }

  testWidgets('citizen journey: sign in, file a report, see it in My Reports',
      (tester) async {
    await tester.pumpWidget(MtaaFixMobileApp(apiClient: ApiClient(
      baseUrl: 'http://journey.test/api/v1',
      client: api(),
      tokenStore: InMemoryTokenStore(),
    )));
    await quiesce(tester);

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('Welcome Back'), findsOneWidget);
    await signIn(tester);

    await tester.tap(find.text('Reports'));
    await tester.pump();
    await quiesce(tester);
    expect(find.byType(ReportListScreen), findsOneWidget);
    expect(find.text('No reports yet'), findsOneWidget);

    await tester.tap(find.text('Report an Issue'));
    await tester.pump();
    await quiesce(tester);
    expect(find.byType(ReportFormScreen), findsOneWidget);

    await fillAndSubmit(tester);

    expect(find.text('Report MTF-2026-000042 submitted.'), findsOneWidget);
    expect(find.byType(ReportDetailScreen), findsOneWidget);
    expect(find.text('Blocked drain on Nyeri Rd'), findsOneWidget);

    await tester.pageBack();
    await tester.pump();
    await quiesce(tester);
    expect(find.byType(ReportListScreen), findsOneWidget);

    await tester.tap(find.byTooltip('Refresh'));
    await tester.pump();
    await quiesce(tester);
    expect(find.text('1 report'), findsOneWidget);
    expect(find.text('Blocked drain on Nyeri Rd'), findsOneWidget);
    expect(find.text('MTF-2026-000042'), findsOneWidget);
  });

  testWidgets('critical screens fit a 320x640 phone without overflow',
      (tester) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(MtaaFixMobileApp(apiClient: ApiClient(
      baseUrl: 'http://small.test/api/v1',
      client: api(),
      tokenStore: InMemoryTokenStore(),
    )));
    await quiesce(tester);

    await signIn(tester);
    await tester.tap(find.text('Reports'));
    await tester.pump();
    await quiesce(tester);
    expect(find.byType(ReportListScreen), findsOneWidget);

    await tester.tap(find.text('Report an Issue'));
    await tester.pump();
    await quiesce(tester);
    expect(find.byType(ReportFormScreen), findsOneWidget);

    await fillAndSubmit(tester);

    expect(find.byType(ReportDetailScreen), findsOneWidget);
    expect(find.text('Blocked drain on Nyeri Rd'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('key controls expose accessible semantics labels',
      (tester) async {
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(MtaaFixMobileApp(apiClient: ApiClient(
      baseUrl: 'http://a11y.test/api/v1',
      client: api(),
      tokenStore: InMemoryTokenStore(),
    )));
    await quiesce(tester);

    expect(find.bySemanticsLabel('Email'), findsOneWidget);
    expect(find.bySemanticsLabel('Password'), findsOneWidget);

    await signIn(tester);
    expect(find.bySemanticsLabel(RegExp('Home')), findsWidgets);
    expect(find.bySemanticsLabel(RegExp('Reports')), findsWidgets);
    expect(find.bySemanticsLabel(RegExp(r'^Map')), findsWidgets);
    expect(find.bySemanticsLabel(RegExp('Alerts')), findsWidgets);
    expect(find.bySemanticsLabel(RegExp('Profile')), findsWidgets);

    await tester.tap(find.text('Reports'));
    await tester.pump();
    await quiesce(tester);
    expect(find.byTooltip('Refresh'), findsOneWidget);

    await tester.tap(find.text('Report an Issue'));
    await tester.pump();
    await quiesce(tester);
    expect(find.bySemanticsLabel(RegExp('Category')), findsWidgets);
    expect(find.bySemanticsLabel(RegExp('Title')), findsWidgets);
    expect(find.bySemanticsLabel(RegExp('Description')), findsWidgets);

    // flutter_test verifies handle disposal before package:test tearDowns run,
    // so dispose inline at the end of the body.
    semantics.dispose();
  });
}
