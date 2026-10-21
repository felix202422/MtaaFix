import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';

/// Persists the pending-submission queue (the "outbox") as a single JSON
/// document; the controller owns the in-memory list and flushes it wholesale.
class OutboxStore {
  OutboxStore(this._storage);

  final OfflineStorage _storage;

  static const String _group = 'pending';
  static const String _file = 'outbox.json';

  Future<List<PendingSubmission>> load() async {
    final json = await _storage.readJson(_group, _file);
    if (json == null) return const [];
    final raw = json['items'];
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(PendingSubmission.fromJson)
        .whereType<PendingSubmission>()
        .toList(growable: true);
  }

  Future<bool> saveAll(List<PendingSubmission> items) => _storage.writeJson(
      _group, _file, {
        'schema': PendingSubmission.schemaVersion,
        'items': items.map((i) => i.toJson()).toList(growable: false),
      });
}
