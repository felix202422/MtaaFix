import 'dart:collection';

import 'package:flutter/foundation.dart';

import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/features/offline/application/connectivity_service.dart';
import 'package:mtaafix_mobile/features/offline/data/category_cache.dart';
import 'package:mtaafix_mobile/features/offline/data/draft_store.dart';
import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';
import 'package:mtaafix_mobile/features/offline/data/outbox_store.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';
import 'package:mtaafix_mobile/features/offline/domain/report_draft.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// Owns offline state: the report draft, the pending-submission queue and
/// the connectivity flag. [flush] drains the queue when online; entries are
/// only removed once the server accepted them (report created + photos
/// attached) — an unsent report is never presented as submitted.
class OfflineController extends ChangeNotifier {
  OfflineController({
    OfflineStorage? storage,
    ConnectivityService? connectivity,
    Future<Report> Function(PendingSubmission item)? submit,
    Future<void> Function(String reportId, String photoPath)? uploadPhoto,
  })  : storage = storage ?? OfflineStorage(),
        connectivity = connectivity ?? ConnectivityService(),
        _submit = submit,
        _uploadPhoto = uploadPhoto {
    _drafts = DraftStore(this.storage);
    _outbox = OutboxStore(this.storage);
    _categories = CategoryCache(this.storage);
  }

  final OfflineStorage storage;
  final ConnectivityService connectivity;
  final Future<Report> Function(PendingSubmission item)? _submit;
  final Future<void> Function(String reportId, String photoPath)? _uploadPhoto;

  late final DraftStore _drafts;
  late final OutboxStore _outbox;
  late final CategoryCache _categories;

  final List<PendingSubmission> _items = [];
  bool _started = false;
  bool _flushing = false;
  final List<String> _justSubmitted = [];

  bool get isOnline => connectivity.isOnline;
  bool get isFlushing => _flushing;
  List<PendingSubmission> get items => UnmodifiableListView(_items);

  /// Everything not yet fully filed (waiting, attaching or failed).
  int get unsentCount => _items.length;

  DraftStore get drafts => _drafts;
  CategoryCache get categories => _categories;

  /// MTF codes filed since the last call; screens consume this for a toast.
  List<String> consumeJustSubmitted() {
    final codes = List<String>.of(_justSubmitted);
    _justSubmitted.clear();
    return codes;
  }

  /// Loads persisted state and subscribes to connectivity: coming back
  /// online automatically triggers a flush.
  Future<void> start() async {
    if (_started) return;
    _started = true;
    _items
      ..clear()
      ..addAll(await _outbox.load());
    _notify();
    connectivity.addListener(_onConnectivityChanged);
    await connectivity.start();
  }

  void _onConnectivityChanged() {
    _notify();
    if (connectivity.isOnline) {
      flush();
    }
  }

  /// Queues a submission that failed with a network error.
  Future<void> enqueue(PendingSubmission item) async {
    _items.insert(0, item);
    await _persist();
    _notify();
  }

  /// Removes a queued item (user discard, or replaced by a re-edited
  /// submit). Photo files are deleted unless listed in [keepPaths].
  Future<void> discard(String localId, {Set<String> keepPaths = const {}}) async {
    final item = _items.where((i) => i.localId == localId).toList();
    for (final existing in item) {
      // Files only exist for never-filed photos.
      if (!existing.isFiled) {
        for (final path in existing.photoPaths) {
          if (keepPaths.contains(path)) continue;
          await storage.removeFileByPath(path);
        }
      }
    }
    _items.removeWhere((i) => i.localId == localId);
    await _persist();
    _notify();
  }

  /// Drops local private state on sign-out (shared-device hygiene).
  Future<void> clearAll() async {
    _items.clear();
    await _outbox.saveAll(_items);
    await _drafts.clear(releasePhotos: true);
    _notify();
  }

  Future<ReportDraft?> loadDraft() => _drafts.load();

  Future<void> saveDraft(ReportDraft draft) => _drafts.save(draft);

  Future<void> clearDraft({bool releasePhotos = true}) =>
      _drafts.clear(releasePhotos: releasePhotos);

  /// Drains the queue. Safe to call repeatedly; concurrent calls no-op.
  Future<void> flush() async {
    if (_disposed || _flushing || !connectivity.isOnline) return;
    final submit = _submit;
    final uploadPhoto = _uploadPhoto;
    if (submit == null || uploadPhoto == null) return;
    _flushing = true;
    _notify();
    try {
      for (final seed in List<PendingSubmission>.of(_items)) {
        if (seed.state == PendingSubmissionState.failed) continue;
        if (!connectivity.isOnline) break;
        await _flushOne(seed, submit, uploadPhoto);
      }
    } finally {
      _flushing = false;
      _notify();
    }
  }

  Future<void> _flushOne(
    PendingSubmission seed,
    Future<Report> Function(PendingSubmission item) submit,
    Future<void> Function(String reportId, String photoPath) uploadPhoto,
  ) async {
    final found = _byId(seed.localId);
    if (found == null || found.state == PendingSubmissionState.failed) return;
    var item = found;

    if (item.serverId == null) {
      try {
        final report = await submit(item);
        item = item.copyWith(
          serverId: report.id,
          serverCode: report.code,
          state: PendingSubmissionState.attachingPhotos,
          updatedAt: DateTime.now(),
          clearError: true,
        );
        await _replace(item);
      } on ApiException catch (e) {
        if (e.isNetwork) return; // Connection dropped — keep waiting.
        if (e.statusCode == 409) {
          if (e.code == 'IDEMPOTENCY_IN_PROGRESS') return; // Retry later.
          await _replace(item.copyWith(
            state: PendingSubmissionState.failed,
            lastError: e.userMessage,
            updatedAt: DateTime.now(),
          ));
          return;
        }
        await _replace(item.copyWith(
          state: PendingSubmissionState.failed,
          lastError: e.userMessage,
          updatedAt: DateTime.now(),
        ));
        return;
      }
    }

    final remaining = List<String>.of(item.photoPaths);
    var uploaded = item.uploadedPhotoCount;
    var failures = item.photoFailures;
    while (remaining.isNotEmpty) {
      if (!connectivity.isOnline) {
        item = item.copyWith(
          photoPaths: remaining,
          uploadedPhotoCount: uploaded,
          photoFailures: failures,
        );
        await _replace(item);
        return;
      }
      try {
        await uploadPhoto(item.serverId!, remaining.first);
        remaining.removeAt(0);
        uploaded++;
        item = item.copyWith(
          photoPaths: List<String>.of(remaining),
          uploadedPhotoCount: uploaded,
          photoFailures: failures,
          updatedAt: DateTime.now(),
        );
        await _replace(item);
      } on ApiException catch (e) {
        if (e.isNetwork) {
          item = item.copyWith(
            photoPaths: List<String>.of(remaining),
            uploadedPhotoCount: uploaded,
            photoFailures: failures,
          );
          await _replace(item);
          return; // Connection dropped — photos continue next flush.
        }
        // Persistent photo failure: report already exists — drop the file,
        // count the loss honestly, continue with the rest.
        remaining.removeAt(0);
        failures++;
        item = item.copyWith(
          photoPaths: List<String>.of(remaining),
          uploadedPhotoCount: uploaded,
          photoFailures: failures,
          lastError: e.userMessage,
          updatedAt: DateTime.now(),
        );
        await _replace(item);
      }
    }

    final settled = _byId(item.localId);
    if (settled == null) return;
    if (settled.photoFailures > 0) {
      await _replace(settled.copyWith(
        state: PendingSubmissionState.failed,
        lastError: 'Report ${settled.serverCode} was filed, but '
            '${settled.photoFailures} photo(s) could not be attached.',
        updatedAt: DateTime.now(),
      ));
      return;
    }
    _items.removeWhere((i) => i.localId == settled.localId);
    if (settled.serverCode != null) _justSubmitted.add(settled.serverCode!);
    for (final path in settled.photoPaths) {
      await storage.removeFileByPath(path);
    }
    await _persist();
    _notify();
  }

  PendingSubmission? _byId(String localId) {
    for (final i in _items) {
      if (i.localId == localId) return i;
    }
    return null;
  }

  Future<void> _replace(PendingSubmission item) async {
    final index = _items.indexWhere((i) => i.localId == item.localId);
    if (index >= 0) _items[index] = item;
    await _persist();
    _notify();
  }

  Future<void> _persist() => _outbox.saveAll(_items);

  bool _disposed = false;

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    connectivity.removeListener(_onConnectivityChanged);
    connectivity.dispose();
    super.dispose();
  }
}
