import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';
import 'package:mtaafix_mobile/features/offline/domain/report_draft.dart';

/// Persists the single in-progress report draft across app restarts.
class DraftStore {
  DraftStore(this._storage);

  final OfflineStorage _storage;

  static const String _group = 'drafts';
  static const String _file = 'report_draft.json';

  Future<ReportDraft?> load() async {
    final json = await _storage.readJson(_group, _file);
    if (json == null) return null;
    return ReportDraft.tryFromJson(json);
  }

  Future<bool> save(ReportDraft draft) =>
      _storage.writeJson(_group, _file, draft.toJson());

  /// Removes the draft; when [releasePhotos] is true the draft's copied
  /// photo files are deleted too.
  Future<void> clear({bool releasePhotos = true}) async {
    if (releasePhotos) {
      final existing = await load();
      if (existing != null) {
        for (final path in existing.photoPaths) {
          await _storage.removeFileByPath(path);
        }
      }
    }
    await _storage.remove(_group, _file);
  }

  /// Deletes photo files that are no longer referenced (submitted or
  /// handed off to the pending queue).
  Future<void> releasePhotos(List<String> paths) async {
    for (final path in paths) {
      await _storage.removeFileByPath(path);
    }
  }
}
