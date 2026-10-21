import 'package:mtaafix_mobile/features/offline/data/offline_storage.dart';

/// Caches the last successful `GET /categories/public` payload so the
/// report form can be completed offline (Phase 14).
class CategoryCache {
  CategoryCache(this._storage);

  final OfflineStorage _storage;

  static const String _group = 'cache';
  static const String _file = 'categories.json';

  Future<bool> save(List<dynamic> rawCategories) =>
      _storage.writeJson(_group, _file, {
        'schema': 1,
        'categories': rawCategories,
      });

  Future<List<Map<String, dynamic>>?> load() async {
    final json = await _storage.readJson(_group, _file);
    if (json == null || json['schema'] != 1) return null;
    final raw = json['categories'];
    if (raw is! List) return null;
    return raw.whereType<Map<String, dynamic>>().toList(growable: false);
  }

  Future<void> clear() => _storage.remove(_group, _file);
}
