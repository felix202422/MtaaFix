import 'dart:async';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/features/offline/application/connectivity_service.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_controller.dart';
import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';
import 'package:mtaafix_mobile/features/offline/domain/report_draft.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

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

  void close() => _events.close();
}

Report report({String id = 'r1', String code = 'MTF-0001'}) =>
    Report.fromJson({'id': id, 'code': code, 'title': 'Pothole', 'status': 'SUBMITTED'});

PendingSubmission item({
  String localId = 'local-1',
  String key = 'idem-key-0000000000000001',
  List<String> photoPaths = const [],
}) {
  final now = DateTime.now();
  return PendingSubmission(
    localId: localId,
    idempotencyKey: key,
    categoryId: 'c1',
    categoryName: 'Roads',
    title: 'Pothole near market',
    description: 'Deep hole',
    latitude: -6.16,
    longitude: 35.75,
    photoPaths: photoPaths,
    createdAt: now,
    updatedAt: now,
  );
}

void main() {
  late Directory tempDir;
  late FakeConnectivity fake;
  late List<String> submittedKeys;
  late List<List<String>> uploadedPhotos;

  // Behaviours toggled per test.
  ApiException? createError;
  ApiException? uploadError;
  int? uploadFailOnCall;
  int createCalls = 0;

  OfflineController makeController() {
    final controller = OfflineController(
      storage: OfflineStorage(baseDir: tempDir),
      connectivity: ConnectivityService(source: fake),
      submit: (pending) {
        createCalls++;
        submittedKeys.add(pending.idempotencyKey);
        final error = createError;
        if (error != null) throw error;
        return Future.value(report(code: 'MTF-000$createCalls'));
      },
      uploadPhoto: (reportId, path) {
        final call = uploadedPhotos.length + 1;
        uploadedPhotos.add([reportId, path]);
        if (uploadError != null && (uploadFailOnCall == null || uploadFailOnCall == call)) {
          throw uploadError!;
        }
        return Future.value();
      },
    );
    return controller;
  }

  setUp(() {
    tempDir = Directory.systemTemp.createTempSync('mtaafix_offline_test_');
    fake = FakeConnectivity();
    submittedKeys = [];
    uploadedPhotos = [];
    createCalls = 0;
    createError = null;
    uploadError = null;
    uploadFailOnCall = null;
  });

  tearDown(() async {
    fake.close();
    // Let any last flush write finish before removing the directory
    // (Windows refuses to delete files with open handles).
    for (var attempt = 0; attempt < 5; attempt++) {
      try {
        tempDir.deleteSync(recursive: true);
        break;
      } on Object {
        await Future<void>.delayed(const Duration(milliseconds: 50));
      }
    }
  });

  test('enqueue persists the queue for the next app start', () async {
    // Restart while offline: an online controller auto-flushes the reloaded
    // queue from start() (fire-and-forget), which would drain it before the
    // assertions below — a race that CI's loaded event loop reliably loses.
    fake.online = false;
    final first = makeController();
    await first.start();
    await first.enqueue(item());
    first.dispose();

    final second = makeController();
    await second.start();
    await pumpEventQueue();
    expect(second.items, hasLength(1));
    expect(second.items.first.title, 'Pothole near market');
    expect(second.items.first.idempotencyKey, 'idem-key-0000000000000001');
    second.dispose();
  });

  test('stays queued while offline, flushes automatically once online', () async {
    fake.online = false;
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item());
    await pumpEventQueue();

    expect(controller.items, hasLength(1));
    expect(createCalls, 0);

    fake.setOnline(true);
    // The flush is triggered by the connectivity listener — wait for it.
    for (var i = 0; i < 50 && controller.items.isNotEmpty; i++) {
      await pumpEventQueue();
    }

    expect(createCalls, 1);
    expect(controller.items, isEmpty);
    expect(controller.consumeJustSubmitted(), ['MTF-0001']);
    controller.dispose();
  });

  test('network failure during flush keeps the item waiting', () async {
    createError = const ApiException(
        statusCode: 0, message: 'No connection');
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item());
    await controller.flush();
    await pumpEventQueue();

    expect(createCalls, 1);
    expect(controller.items, hasLength(1));
    expect(controller.items.first.state, PendingSubmissionState.pending);

    createError = null;
    await controller.flush();
    await pumpEventQueue();
    expect(controller.items, isEmpty);
    expect(submittedKeys.first, submittedKeys.last,
        reason: 'the retry must reuse the same idempotency key');
    controller.dispose();
  });

  test('server rejection marks the item failed and later flushes skip it',
      () async {
    createError = const ApiException(
        statusCode: 400,
        code: 'VALIDATION_ERROR',
        message: 'Title is too long');
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item());
    await controller.flush();
    await pumpEventQueue();

    expect(controller.items.single.state, PendingSubmissionState.failed);
    expect(controller.items.single.lastError, 'Title is too long');

    createError = null;
    await controller.flush();
    await pumpEventQueue();
    expect(createCalls, 1, reason: 'failed items are not retried silently');
    controller.dispose();
  });

  test('409 IN_PROGRESS waits, CONFLICT fails', () async {
    createError = const ApiException(
        statusCode: 409, code: 'IDEMPOTENCY_IN_PROGRESS', message: 'Busy');
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item());
    await controller.flush();
    await pumpEventQueue();
    expect(controller.items.single.state, PendingSubmissionState.pending);

    createError = const ApiException(
        statusCode: 409, code: 'IDEMPOTENCY_CONFLICT', message: 'Changed');
    await controller.flush();
    await pumpEventQueue();
    expect(controller.items.single.state, PendingSubmissionState.failed);
    expect(controller.items.single.lastError, 'Changed');
    controller.dispose();
  });

  test('attaches photos after the create and files the report once', () async {
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item(photoPaths: ['p1.jpg', 'p2.jpg']));
    await controller.flush();
    await pumpEventQueue();

    expect(createCalls, 1);
    expect(uploadedPhotos, [
      ['r1', 'p1.jpg'],
      ['r1', 'p2.jpg'],
    ]);
    expect(controller.items, isEmpty);
    expect(controller.consumeJustSubmitted(), ['MTF-0001']);
    controller.dispose();
  });

  test('photo network failure resumes later without re-creating the report',
      () async {
    uploadError = const ApiException(statusCode: 0, message: 'No connection');
    uploadFailOnCall = 2;
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item(photoPaths: ['p1.jpg', 'p2.jpg']));
    await controller.flush();
    await pumpEventQueue();

    expect(createCalls, 1);
    expect(controller.items, hasLength(1));
    expect(controller.items.first.state, PendingSubmissionState.attachingPhotos);
    expect(controller.items.first.photoPaths, ['p2.jpg']);
    expect(controller.items.first.uploadedPhotoCount, 1);

    uploadError = null;
    uploadFailOnCall = null;
    await controller.flush();
    await pumpEventQueue();

    expect(createCalls, 1, reason: 'must not file the report twice');
    expect(controller.items, isEmpty);
    controller.dispose();
  });

  test('persistent photo failure is reported honestly, not silently dropped',
      () async {
    uploadError = const ApiException(statusCode: 400, message: 'File rejected');
    uploadFailOnCall = 1;
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item(photoPaths: ['bad.jpg', 'good.jpg']));
    await controller.flush();
    await pumpEventQueue();

    expect(createCalls, 1);
    final entry = controller.items.single;
    expect(entry.state, PendingSubmissionState.failed);
    expect(entry.serverCode, 'MTF-0001');
    expect(entry.photoFailures, 1);
    expect(entry.lastError, contains('photo'));
    expect(uploadedPhotos, hasLength(2));
    controller.dispose();
  });

  test('flush no-ops while offline', () async {
    fake.online = false;
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item());
    await controller.flush();
    await pumpEventQueue();
    expect(createCalls, 0);
    expect(controller.items, hasLength(1));
    controller.dispose();
  });

  test('clearAll wipes the queue and the draft (sign-out hygiene)', () async {
    final controller = makeController();
    await controller.start();
    await controller.saveDraft(ReportDraft(
      categoryId: 'c1',
      title: 'Drafted',
      description: null,
      latitude: 0,
      longitude: 0,
      photoPaths: const [],
      updatedAt: DateTime.now(),
    ));
    await controller.enqueue(item());
    expect(await controller.loadDraft(), isNotNull);

    await controller.clearAll();
    expect(controller.items, isEmpty);
    expect(await controller.loadDraft(), isNull);

    controller.dispose();
    final fresh = makeController();
    await fresh.start();
    expect(fresh.items, isEmpty);
    expect(await fresh.loadDraft(), isNull);
    fresh.dispose();
  });

  test('discard removes an entry and its photo files unless kept', () async {
    final photo = File('${tempDir.path}${Platform.pathSeparator}photo.jpg')
      ..writeAsStringSync('x');
    final controller = makeController();
    await controller.start();
    await controller.enqueue(item(photoPaths: [photo.path]));
    expect(photo.existsSync(), isTrue);

    await controller.discard('local-1', keepPaths: {photo.path});
    expect(controller.items, isEmpty);
    expect(photo.existsSync(), isTrue, reason: 'kept paths survive');

    await controller.enqueue(item(localId: 'local-2', photoPaths: [photo.path]));
    await controller.discard('local-2');
    expect(controller.items, isEmpty);
    expect(photo.existsSync(), isFalse, reason: 'discarded files are removed');
    controller.dispose();
  });
}
