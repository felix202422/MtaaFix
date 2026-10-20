import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

void main() {
  const base = 'http://reports.test/api/v1';

  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  Map<String, dynamic> reportJson({
    String id = 'r1',
    String code = 'MTF-0001',
    String status = 'SUBMITTED',
  }) =>
      {
        'id': id,
        'code': code,
        'title': 'Pothole on Main Street',
        'description': 'Deep pothole near the market',
        'status': status,
        'location': {'latitude': -1.29, 'longitude': 36.82},
        'reportedAt': '2026-10-01T09:00:00Z',
        'categoryId': 'cat-1',
        'media': <dynamic>[],
        'comments': <dynamic>[],
        'statusHistory': <dynamic>[],
      };

  ReportsApi apiWith(MockClient mock) => ReportsApi(
        ApiClient(baseUrl: base, client: mock, tokenStore: InMemoryTokenStore()),
      );

  test('list sends page, size and filters as query parameters', () async {
    late Uri seen;
    final api = apiWith(MockClient((request) async {
      seen = request.url;
      expect(request.method, 'GET');
      return http.Response(
        jsonEncode({
          'content': [reportJson()],
          'totalElements': 1,
          'totalPages': 1,
          'page': 2,
          'size': 10,
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final page = await api.list(
      status: 'UNDER_REVIEW',
      categoryId: 'cat-9',
      q: 'market',
      page: 2,
      size: 10,
    );

    expect(seen.path, '/api/v1/reports');
    expect(seen.queryParameters['status'], 'UNDER_REVIEW');
    expect(seen.queryParameters['categoryId'], 'cat-9');
    expect(seen.queryParameters['q'], 'market');
    expect(seen.queryParameters['page'], '2');
    expect(seen.queryParameters['size'], '10');
    expect(page.totalElements, 1);
    expect(page.content.single.code, 'MTF-0001');
  });

  test('list omits empty filters', () async {
    late Uri seen;
    final api = apiWith(MockClient((request) async {
      seen = request.url;
      return http.Response(jsonEncode({'content': <dynamic>[]}), 200,
          headers: jsonHeaders());
    }));

    await api.list(q: '   ');

    expect(seen.queryParameters.containsKey('status'), isFalse);
    expect(seen.queryParameters.containsKey('categoryId'), isFalse);
    expect(seen.queryParameters.containsKey('q'), isFalse);
  });

  test('create posts the category, title and location body', () async {
    late Map<String, dynamic> body;
    final api = apiWith(MockClient((request) async {
      expect(request.method, 'POST');
      expect(request.url.path, '/api/v1/reports');
      body = jsonDecode(request.body) as Map<String, dynamic>;
      return http.Response(jsonEncode(reportJson()), 201,
          headers: jsonHeaders());
    }));

    final created = await api.create(
      categoryId: 'cat-1',
      title: 'Pothole on Main Street',
      description: 'Deep pothole near the market',
      latitude: -1.29,
      longitude: 36.82,
    );

    expect(body['categoryId'], 'cat-1');
    expect(body['title'], 'Pothole on Main Street');
    expect(body['description'], 'Deep pothole near the market');
    expect(body['location'], {'latitude': -1.29, 'longitude': 36.82});
    expect(created.id, 'r1');
  });

  test('update sends a PUT with only the changed fields', () async {
    late String method;
    late Map<String, dynamic> body;
    final api = apiWith(MockClient((request) async {
      method = request.method;
      expect(request.url.path, '/api/v1/reports/r1');
      body = jsonDecode(request.body) as Map<String, dynamic>;
      return http.Response(jsonEncode(reportJson()), 200,
          headers: jsonHeaders());
    }));

    await api.update('r1', title: 'New title');

    expect(method, 'PUT');
    expect(body.containsKey('title'), isTrue);
    expect(body['title'], 'New title');
    expect(body.containsKey('categoryId'), isFalse);
    expect(body.containsKey('description'), isFalse);
  });

  test('lifecycle actions hit their exact endpoints', () async {
    final hits = <String>[];
    final api = apiWith(MockClient((request) async {
      hits.add('${request.method} ${request.url.path}');
      return http.Response(jsonEncode(reportJson()), 200,
          headers: jsonHeaders());
    }));

    await api.cancel('r1');
    await api.confirm('r1', feedback: 'Looks good');
    await api.dispute('r1', disputeReason: 'Still flooded');
    await api.resubmit('r1');
    await api.addComment('r1', 'Any update?');

    expect(hits, [
      'POST /api/v1/reports/r1/cancel',
      'POST /api/v1/reports/r1/confirm',
      'POST /api/v1/reports/r1/dispute',
      'POST /api/v1/reports/r1/resubmit',
      'POST /api/v1/reports/r1/comments',
    ]);
  });

  test('confirm without feedback posts an empty object', () async {
    late String requestBody;
    final api = apiWith(MockClient((request) async {
      requestBody = request.body;
      return http.Response(jsonEncode(reportJson(status: 'CLOSED')), 200,
          headers: jsonHeaders());
    }));

    final updated = await api.confirm('r1');

    expect(requestBody, '{}');
    expect(updated.status, ReportStatus.closed);
  });

  test('dispute posts the required disputeReason', () async {
    late Map<String, dynamic> body;
    final api = apiWith(MockClient((request) async {
      body = jsonDecode(request.body) as Map<String, dynamic>;
      return http.Response(jsonEncode(reportJson()), 200,
          headers: jsonHeaders());
    }));

    await api.dispute('r1', disputeReason: 'Road is still blocked');

    expect(body['disputeReason'], 'Road is still blocked');
  });

  test('uploadPhoto posts multipart to the media endpoint', () async {
    late String path;
    final api = apiWith(MockClient((request) async {
      path = request.url.path;
      expect(request.method, 'POST');
      expect(request.headers['content-type'], contains('multipart/form-data'));
      return http.Response(jsonEncode(reportJson()), 200,
          headers: jsonHeaders());
    }));

    final dir = await Directory.systemTemp.createTemp('mtaafix_test');
    final file = File('${dir.path}/photo.txt');
    file.writeAsStringSync('fake-photo-bytes');

    await api.uploadPhoto('r1', file.path);

    expect(path, '/api/v1/reports/r1/media/upload');
    await file.delete();
    await dir.delete();
  });

  test('categories parses content list and hides inactive entries', () async {
    final api = apiWith(MockClient((request) async {
      expect(request.url.path, '/api/v1/categories/public');
      return http.Response(
        jsonEncode([
          {'id': 'c1', 'name': 'Roads', 'active': true},
          {'id': 'c2', 'name': 'Water', 'active': false},
          {'id': 'c3', 'name': 'Waste'},
        ]),
        200,
        headers: jsonHeaders(),
      );
    }));

    final categories = await api.categories();

    expect(categories.map((c) => c.id), ['c1', 'c3']);
    expect(categories.first.name, 'Roads');
  });

  test('dashboardStats parses the stats map', () async {
    final api = apiWith(MockClient((request) async {
      expect(request.url.path, '/api/v1/dashboard/me');
      return http.Response(
        jsonEncode({
          'organisationId': 'org-1',
          'stats': {'SUBMITTED': 3, 'RESOLVED': 7, 'not-a-number': 'x'},
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final stats = await api.dashboardStats();

    expect(stats, {'SUBMITTED': 3, 'RESOLVED': 7});
  });

  test('dashboardStats maps the flat citizen counters to statuses', () async {
    final api = apiWith(MockClient((request) async {
      return http.Response(
        jsonEncode({
          'initials': 'MTFAM',
          'name': 'Asha Mrema',
          'role': 'CITIZEN',
          'totalReports': 4,
          'submittedReports': 2,
          'verifiedReports': 1,
          'closedReports': 0,
          'needsInfoCount': 1,
          'resolvedAwaitingConfirmCount': 0,
          'activeCount': 3,
          'attentionReports': <dynamic>[],
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final stats = await api.dashboardStats();

    expect(stats, {
      'SUBMITTED': 2,
      'VERIFIED': 1,
      'CLOSED': 0,
      'NEEDS_INFORMATION': 1,
      'RESOLVED': 0,
    });
  });

  test('publicList passes the bbox filter', () async {
    late Uri seen;
    final api = apiWith(MockClient((request) async {
      seen = request.url;
      return http.Response(jsonEncode({'content': <dynamic>[]}), 200,
          headers: jsonHeaders());
    }));

    await api.publicList(bbox: '36.0,-2.0,37.0,-1.0', status: 'RESOLVED');

    expect(seen.path, '/api/v1/reports/public');
    expect(seen.queryParameters['bbox'], '36.0,-2.0,37.0,-1.0');
    expect(seen.queryParameters['status'], 'RESOLVED');
  });

  test('getPublic fetches the public detail by code', () async {
    final api = apiWith(MockClient((request) async {
      expect(request.url.path, '/api/v1/reports/public/MTF-0001');
      return http.Response(
        jsonEncode({
          'code': 'MTF-0001',
          'title': 'Broken streetlight',
          'status': 'RESOLVED',
          'categoryName': 'Lighting',
          'timeline': [
            {'status': 'SUBMITTED', 'timestamp': '2026-10-01T09:00:00Z'},
            {'status': 'RESOLVED', 'timestamp': '2026-10-02T09:00:00Z'},
          ],
        }),
        200,
        headers: jsonHeaders(),
      );
    }));

    final detail = await api.getPublic('MTF-0001');

    expect(detail.categoryName, 'Lighting');
    expect(detail.timeline, hasLength(2));
    expect(detail.timeline.last.status, ReportStatus.resolved);
  });

  test('deleteMedia issues DELETE /media/{id}', () async {
    final hits = <String>[];
    final api = apiWith(MockClient((request) async {
      hits.add('${request.method} ${request.url.path}');
      return http.Response('{}', 200, headers: jsonHeaders());
    }));

    await api.deleteMedia('m1');

    expect(hits, ['DELETE /api/v1/media/m1']);
  });

  test('resolveMediaUrl strips the /api/v1 prefix from relative paths',
      () async {
    final api = apiWith(MockClient((_) async => fail('No HTTP expected')));

    expect(
      api.resolveMediaUrl('/api/v1/media/m1/content'),
      'http://reports.test/api/v1/media/m1/content',
    );
    expect(api.resolveMediaUrl(''), '');
    expect(api.resolveMediaUrl('https://cdn.test/a.jpg'),
        'https://cdn.test/a.jpg');
  });
}
