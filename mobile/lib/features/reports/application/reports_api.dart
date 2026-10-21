import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/network/paged.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// Typed calls for the citizen reporting APIs (FR-2.x).
class ReportsApi {
  ReportsApi(this._api);

  final ApiClient _api;

  Future<Paged<Report>> list({
    String? status,
    String? categoryId,
    String? q,
    int page = 0,
    int size = 20,
  }) async {
    final params = <String, String>{
      'page': '$page',
      'size': '$size',
      if (status != null && status.isNotEmpty) 'status': status,
      if (categoryId != null && categoryId.isNotEmpty) 'categoryId': categoryId,
      if (q != null && q.trim().isNotEmpty) 'q': q.trim(),
    };
    final json = await _api.get('/reports?${Uri(queryParameters: params).query}');
    return Paged.parse(json, Report.fromJson);
  }

  Future<Report> get(String id) async =>
      Report.fromJson(await _api.get('/reports/${Uri.encodeComponent(id)}'));

  Future<Report> create({
    required String categoryId,
    required String title,
    String? description,
    required double latitude,
    required double longitude,
    String? idempotencyKey,
  }) async {
    final json = await _api.post(
      '/reports',
      {
        'categoryId': categoryId,
        'title': title,
        if (description != null && description.trim().isNotEmpty)
          'description': description.trim(),
        'location': {'latitude': latitude, 'longitude': longitude},
      },
      idempotencyKey,
    );
    return Report.fromJson(json);
  }

  Future<Report> update(
    String id, {
    String? title,
    String? description,
    String? categoryId,
  }) async {
    final json = await _api.put('/reports/${Uri.encodeComponent(id)}', {
      if (title != null) 'title': title,
      if (description != null) 'description': description,
      if (categoryId != null) 'categoryId': categoryId,
    });
    return Report.fromJson(json);
  }

  Future<Report> cancel(String id) async =>
      Report.fromJson(await _api.post('/reports/${Uri.encodeComponent(id)}/cancel'));

  Future<Report> confirm(String id, {String? feedback}) async =>
      Report.fromJson(await _api.post(
        '/reports/${Uri.encodeComponent(id)}/confirm',
        feedback != null && feedback.trim().isNotEmpty
            ? {'feedback': feedback.trim()}
            : <String, dynamic>{},
      ));

  Future<Report> dispute(String id, {required String disputeReason}) async =>
      Report.fromJson(await _api.post(
        '/reports/${Uri.encodeComponent(id)}/dispute',
        {'disputeReason': disputeReason},
      ));

  Future<Report> resubmit(String id) async => Report.fromJson(
      await _api.post('/reports/${Uri.encodeComponent(id)}/resubmit'));

  Future<Report> addComment(String id, String content) async =>
      Report.fromJson(await _api.post(
        '/reports/${Uri.encodeComponent(id)}/comments',
        {'content': content},
      ));

  Future<Report> uploadPhoto(String id, String filePath) async =>
      Report.fromJson(await _api
          .uploadMultipart('/reports/${Uri.encodeComponent(id)}/media/upload',
              filePath: filePath));

  Future<void> deleteMedia(String mediaId) =>
      _api.delete('/media/${Uri.encodeComponent(mediaId)}');

  Future<Paged<PublicReport>> publicList({
    String? status,
    String? categoryId,
    String? bbox,
    int page = 0,
    int size = 50,
  }) async {
    final params = <String, String>{
      'page': '$page',
      'size': '$size',
      if (status != null && status.isNotEmpty) 'status': status,
      if (categoryId != null && categoryId.isNotEmpty) 'categoryId': categoryId,
      if (bbox != null && bbox.isNotEmpty) 'bbox': bbox,
    };
    final json = await _api.get(
        '/reports/public?${Uri(queryParameters: params).query}');
    return Paged.parse(json, PublicReport.fromJson);
  }

  Future<PublicReportDetail> getPublic(String code) async =>
      PublicReportDetail.fromJson(
          await _api.get('/reports/public/${Uri.encodeComponent(code)}'));

  /// Raw `GET /categories/public` payload (for the offline category cache).
  Future<dynamic> categoriesJson() => _api.get('/categories/public');

  Future<List<Category>> categories() async {
    final json = await categoriesJson();
    final list = json['content'] ?? json;
    if (list is! List) return const [];
    return list
        .whereType<Map<String, dynamic>>()
        .map(Category.fromJson)
        .where((c) => c.id.isNotEmpty && c.active != false)
        .toList(growable: false);
  }

  Future<Map<String, int>> dashboardStats() async {
    final json = await _api.get('/dashboard/me');
    final raw = json['stats'];
    final stats = <String, int>{};
    if (raw is Map<String, dynamic>) {
      raw.forEach((key, value) {
        if (value is num) stats[key] = value.toInt();
      });
      return stats;
    }
    // Flat citizen dashboard (`GET /dashboard/me` returns scalar counters).
    const statusCounters = <String, String>{
      'submittedReports': 'SUBMITTED',
      'verifiedReports': 'VERIFIED',
      'closedReports': 'CLOSED',
      'needsInfoCount': 'NEEDS_INFORMATION',
      'resolvedAwaitingConfirmCount': 'RESOLVED',
    };
    statusCounters.forEach((field, status) {
      final value = json[field];
      if (value is num) stats[status] = value.toInt();
    });
    return stats;
  }

  /// Absolute URL for a media path returned by the backend.
  String resolveMediaUrl(String relativeUrl) => mediaUrl(_api, relativeUrl);
}

/// Resolves a media path returned by the backend (`/api/v1/media/{id}/content`)
/// against the configured API origin.
String mediaUrl(ApiClient api, String relativeUrl) {
  if (relativeUrl.isEmpty) return '';
  if (relativeUrl.startsWith('http')) return relativeUrl;
  final base = api.baseUrl.replaceFirst(RegExp(r'/api/v1/?$'), '');
  return '$base$relativeUrl';
}
