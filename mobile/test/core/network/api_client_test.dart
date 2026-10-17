import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';

void main() {
  const base = 'http://api.test/api/v1';
  late InMemoryTokenStore store;

  setUp(() => store = InMemoryTokenStore());

  ApiClient clientWith(http.Client http) => ApiClient(
        baseUrl: base,
        client: http,
        tokenStore: store,
      );

  test('parses a successful JSON response', () async {
    final client = clientWith(
      MockClient(
        (_) async => http.Response(
          jsonEncode({'id': 'c1', 'name': 'Potholes'}),
          200,
          headers: {'content-type': 'application/json'},
        ),
      ),
    );
    final body = await client.get('/categories/public');
    expect(body['name'], 'Potholes');
  });

  test('surfaces backend error code and message as ApiException', () async {
    final client = clientWith(
      MockClient(
        (_) async => http.Response(
          jsonEncode({
            'status': 400,
            'code': 'VALIDATION_ERROR',
            'message': 'Title is required',
            'path': '/api/v1/reports',
          }),
          400,
          headers: {'content-type': 'application/json'},
        ),
      ),
    );
    await expectLater(
      client.post('/reports', {'x': 1}),
      throwsA(
        isA<ApiException>()
            .having((e) => e.statusCode, 'statusCode', 400)
            .having((e) => e.code, 'code', 'VALIDATION_ERROR')
            .having((e) => e.message, 'message', 'Title is required'),
      ),
    );
  });

  test('maps an unreachable server to NETWORK_ERROR with statusCode 0',
      () async {
    final client = clientWith(
      MockClient((_) async => throw http.ClientException('refused')),
    );
    await expectLater(
      client.get('/auth/me'),
      throwsA(
        isA<ApiException>()
            .having((e) => e.statusCode, 'statusCode', 0)
            .having((e) => e.code, 'code', 'NETWORK_ERROR')
            .having((e) => e.isNetwork, 'isNetwork', true),
      ),
    );
  });

  test('resolves an empty 2xx body to an empty map', () async {
    final client = clientWith(MockClient((_) async => http.Response('', 200)));
    final body = await client.delete('/reports/r1/media/m1');
    expect(body, isEmpty);
  });

  test('attaches the bearer token after setSession', () async {
    String? seenAuthorization;
    final client = clientWith(
      MockClient((request) async {
        seenAuthorization = request.headers['authorization'];
        return http.Response(
          jsonEncode({'id': 'u1'}),
          200,
          headers: {'content-type': 'application/json'},
        );
      }),
    );
    await client.setSession(accessToken: 'access-1', refreshToken: 'refresh-1');
    await client.get('/auth/me');
    expect(seenAuthorization, 'Bearer access-1');
  });

  test('refreshes once on 401, persists rotated tokens and retries', () async {
    var getCalls = 0;
    var refreshCalls = 0;
    final client = clientWith(
      MockClient((request) async {
        if (request.method == 'GET') {
          getCalls++;
          if (getCalls == 1) {
            return http.Response(
              jsonEncode({
                'status': 401,
                'code': 'INVALID_TOKEN',
                'message': 'expired',
              }),
              401,
              headers: {'content-type': 'application/json'},
            );
          }
          return http.Response(
            jsonEncode({'ok': true}),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        if (request.method == 'POST' &&
            request.url.path.endsWith('/auth/refresh')) {
          refreshCalls++;
          final payload =
              jsonDecode(request.body) as Map<String, dynamic>;
          expect(payload['refreshToken'], 'old-refresh');
          return http.Response(
            jsonEncode({
              'accessToken': 'new-access',
              'refreshToken': 'new-refresh',
              'accessTokenExpiresIn': 3600,
            }),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        fail('Unexpected request: ${request.method} ${request.url}');
      }),
    );
    await client.setSession(accessToken: 'old-access', refreshToken: 'old-refresh');

    final body = await client.get('/reports/me');

    expect(body['ok'], isTrue);
    expect(refreshCalls, 1);
    expect(getCalls, 2);
    expect(store.accessToken, 'new-access');
    expect(store.refreshToken, 'new-refresh');
    expect(client.accessToken, 'new-access');
  });

  test('clears the session when the refresh token is rejected', () async {
    final client = clientWith(
      MockClient((request) async {
        if (request.method == 'GET') {
          return http.Response(
            jsonEncode({'status': 401, 'message': 'expired'}),
            401,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response(
          jsonEncode({'status': 401, 'message': 'bad refresh'}),
          401,
          headers: {'content-type': 'application/json'},
        );
      }),
    );
    await client.setSession(accessToken: 'a', refreshToken: 'r');

    await expectLater(
      client.get('/auth/me'),
      throwsA(isA<ApiException>().having((e) => e.statusCode, 'statusCode', 401)),
    );
    expect(client.hasSession, isFalse);
    expect(store.accessToken, isNull);
    expect(store.refreshToken, isNull);
  });

  test('keeps stored tokens when the refresh attempt hits the network',
      () async {
    final client = clientWith(
      MockClient((request) async {
        if (request.method == 'GET') {
          return http.Response(
            jsonEncode({'status': 401, 'message': 'expired'}),
            401,
            headers: {'content-type': 'application/json'},
          );
        }
        throw http.ClientException('network down');
      }),
    );
    await client.setSession(accessToken: 'a', refreshToken: 'r');

    await expectLater(
      client.get('/auth/me'),
      throwsA(isA<ApiException>().having((e) => e.statusCode, 'statusCode', 401)),
    );
    expect(store.accessToken, 'a');
    expect(store.refreshToken, 'r');
  });
}
