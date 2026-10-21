import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'package:mtaafix_mobile/features/offline/data/category_cache.dart';
import 'package:mtaafix_mobile/features/offline/data/draft_store.dart';
import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';
import 'package:mtaafix_mobile/features/offline/data/outbox_store.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';
import 'package:mtaafix_mobile/features/offline/domain/report_draft.dart';

ReportDraft draft({List<String> photoPaths = const []}) => ReportDraft(
      categoryId: 'c1',
      categoryName: 'Roads',
      title: 'Broken streetlight',
      description: 'Dark at night',
      latitude: -6.16,
      longitude: 35.75,
      photoPaths: photoPaths,
      updatedAt: DateTime.now(),
    );

void main() {
  late Directory tempDir;
  late OfflineStorage storage;

  setUp(() {
    tempDir = Directory.systemTemp.createTempSync('mtaafix_storage_test_');
    storage = OfflineStorage(baseDir: tempDir);
  });

  tearDown(() => tempDir.deleteSync(recursive: true));

  test('draft round-trips and clear() releases its photo files', () async {
    final photos = File('${tempDir.path}${Platform.pathSeparator}p1.jpg')
      ..writeAsStringSync('img');
    final store = DraftStore(storage);

    expect(await store.load(), isNull);
    expect(await store.save(draft(photoPaths: [photos.path])), isTrue);

    final loaded = await store.load();
    expect(loaded, isNotNull);
    expect(loaded!.title, 'Broken streetlight');
    expect(loaded.photoPaths, [photos.path]);

    await store.clear();
    expect(await store.load(), isNull);
    expect(photos.existsSync(), isFalse, reason: 'draft photos are released');
  });

  test('corrupt or schema-mismatched draft files degrade to null', () async {
    final store = DraftStore(storage);
    await storage.writeJson('drafts', 'report_draft.json', {'schema': 99});
    expect(await store.load(), isNull, reason: 'unknown schema ignored');

    final dir = await storage.group('drafts');
    File('${dir.path}${Platform.pathSeparator}report_draft.json')
        .writeAsStringSync('{not json');
    expect(await store.load(), isNull, reason: 'corrupt file ignored');
  });

  test('outbox round-trips pending submissions', () async {
    final store = OutboxStore(storage);
    final now = DateTime.now();
    await store.saveAll([
      PendingSubmission(
        localId: 'l1',
        idempotencyKey: 'idem-key-0000000000000009',
        categoryId: 'c1',
        title: 'Queued',
        latitude: -6.1,
        longitude: 35.7,
        state: PendingSubmissionState.attachingPhotos,
        photoPaths: const ['a.jpg'],
        serverId: 'r9',
        serverCode: 'MTF-0009',
        createdAt: now,
        updatedAt: now,
      ),
    ]);

    final loaded = await OutboxStore(storage).load();
    expect(loaded, hasLength(1));
    final entry = loaded.single;
    expect(entry.title, 'Queued');
    expect(entry.state, PendingSubmissionState.attachingPhotos);
    expect(entry.serverId, 'r9');
    expect(entry.photoPaths, ['a.jpg']);
  });

  test('missing or corrupt outbox degrades to an empty queue', () async {
    expect(await OutboxStore(storage).load(), isEmpty);
    final dir = await storage.group('pending');
    File('${dir.path}${Platform.pathSeparator}outbox.json')
        .writeAsStringSync('[]');
    expect(await OutboxStore(storage).load(), isEmpty);
  });

  test('category cache saves and loads the raw payload', () async {
    final cache = CategoryCache(storage);
    expect(await cache.load(), isNull);
    await cache.save([
      {'id': 'c1', 'name': 'Roads', 'active': true},
    ]);
    final loaded = await cache.load();
    expect(loaded, hasLength(1));
    expect(loaded!.first['name'], 'Roads');
    await cache.clear();
    expect(await cache.load(), isNull);
  });

  test('copyInto duplicates a source file into the offline store', () async {
    final source = File('${tempDir.path}${Platform.pathSeparator}src.jpg')
      ..writeAsStringSync('bytes');
    final copied = await storage.copyInto('photos', source.path);
    expect(copied, isNotNull);
    expect(File(copied!).readAsStringSync(), 'bytes');
    expect(copied, isNot(source.path), reason: 'copied, not referenced');

    final missing = await storage.copyInto('photos', '/nope/missing.jpg');
    expect(missing, isNull, reason: 'missing source degrades gracefully');
  });

  test('writes are atomic (no partial json left on success)', () async {
    expect(await storage.writeJson('cache', 'x.json', {'a': 1}), isTrue);
    final dir = await storage.group('cache');
    expect(File('${dir.path}${Platform.pathSeparator}x.json.tmp').existsSync(),
        isFalse,
        reason: 'temp file renamed away');
    expect(await storage.readJson('cache', 'x.json'), {'a': 1});
  });
}
