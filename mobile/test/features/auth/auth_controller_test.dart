import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/core/storage/token_store.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/domain/auth_state.dart';

void main() {
  const base = 'http://auth.test/api/v1';

  const validUser = <String, dynamic>{
    'id': 'u1',
    'email': 'citizen@example.com',
    'fullName': 'Asha Mrema',
    'mobile': '+255712345678',
    'role': 'CITIZEN',
    'organisationId': null,
    'active': true,
  };

  const validTokens = <String, dynamic>{
    'accessToken': 'access-1',
    'refreshToken': 'refresh-1',
    'accessTokenExpiresIn': 3600,
    'user': validUser,
  };

  Map<String, String> jsonHeaders() => {'content-type': 'application/json'};

  AuthController controllerWith(MockClient mock, InMemoryTokenStore store) =>
      AuthController(ApiClient(baseUrl: base, client: mock, tokenStore: store));

  test('login success authenticates and stores the token pair', () async {
    final store = InMemoryTokenStore();
    late Map<String, dynamic> payload;
    final controller = controllerWith(
      MockClient((request) async {
        expect(request.method, 'POST');
        expect(request.url.path, '/api/v1/auth/login');
        payload = jsonDecode(request.body) as Map<String, dynamic>;
        return http.Response(jsonEncode(validTokens), 200,
            headers: jsonHeaders());
      }),
      store,
    );

    final ok = await controller.login('citizen@example.com', 'Password1!');

    expect(ok, isTrue);
    expect(payload['email'], 'citizen@example.com');
    expect(payload['password'], 'Password1!');
    expect(controller.isAuthenticated, isTrue);
    expect(controller.state.status, AuthStatus.authenticated);
    expect(controller.user?.id, 'u1');
    expect(store.accessToken, 'access-1');
    expect(store.refreshToken, 'refresh-1');
  });

  test('login with bad credentials exposes the backend message', () async {
    final store = InMemoryTokenStore();
    final controller = controllerWith(
      MockClient((_) async => http.Response(
            jsonEncode({
              'status': 401,
              'code': 'INVALID_CREDENTIALS',
              'message': 'Invalid email or password',
            }),
            401,
            headers: jsonHeaders(),
          )),
      store,
    );

    final ok = await controller.login('bad@example.com', 'wrongpass');

    expect(ok, isFalse);
    expect(controller.state.status, AuthStatus.error);
    expect(controller.errorMessage, contains('Invalid email or password'));
    expect(controller.isAuthenticated, isFalse);
    expect(store.accessToken, isNull);
  });

  test(
      'init without stored tokens stays unauthenticated and skips the network',
      () async {
    final controller = controllerWith(
      MockClient((_) async => fail('No HTTP call expected')),
      InMemoryTokenStore(),
    );

    await controller.init();

    expect(controller.state.status, AuthStatus.unauthenticated);
    expect(controller.isAuthenticated, isFalse);
  });

  test('init restores a stored session via GET /auth/me', () async {
    final store = InMemoryTokenStore();
    final controller = controllerWith(
      MockClient((request) async {
        expect(request.url.path, '/api/v1/auth/me');
        expect(request.headers['authorization'], 'Bearer stored-access');
        return http.Response(jsonEncode(validUser), 200,
            headers: jsonHeaders());
      }),
      store,
    );
    await store.writeTokens(
        accessToken: 'stored-access', refreshToken: 'stored-refresh');

    await controller.init();

    expect(controller.state.status, AuthStatus.authenticated);
    expect(controller.user?.email, 'citizen@example.com');
    expect(store.refreshToken, 'stored-refresh');
  });

  test('init with stored tokens but an unreachable server keeps the tokens',
      () async {
    final store = InMemoryTokenStore();
    final controller = controllerWith(
      MockClient((_) async => throw http.ClientException('down')),
      store,
    );
    await store.writeTokens(
        accessToken: 'stored-access', refreshToken: 'stored-refresh');

    await controller.init();

    expect(controller.state.status, AuthStatus.unauthenticated);
    expect(controller.errorMessage, isNotNull);
    expect(store.accessToken, 'stored-access');
    expect(store.refreshToken, 'stored-refresh');
  });

  test('init with a rejected token clears the session', () async {
    final store = InMemoryTokenStore();
    final controller = controllerWith(
      MockClient((_) async => http.Response(
            jsonEncode({'status': 401, 'message': 'expired'}),
            401,
            headers: jsonHeaders(),
          )),
      store,
    );
    await store.writeTokens(
        accessToken: 'expired-access', refreshToken: 'expired-refresh');

    await controller.init();

    expect(controller.state.status, AuthStatus.unauthenticated);
    expect(store.accessToken, isNull);
    expect(store.refreshToken, isNull);
  });

  test('register creates the account and signs the user in', () async {
    final store = InMemoryTokenStore();
    final controller = controllerWith(
      MockClient((request) async {
        if (request.url.path == '/api/v1/auth/register') {
          final payload = jsonDecode(request.body) as Map<String, dynamic>;
          expect(payload['fullName'], 'Asha Mrema');
          expect(payload['mobile'], '+255712345678');
          return http.Response(jsonEncode({'id': 'u2'}), 201,
              headers: jsonHeaders());
        }
        if (request.url.path == '/api/v1/auth/login') {
          return http.Response(jsonEncode(validTokens), 200,
              headers: jsonHeaders());
        }
        fail('Unexpected request: ${request.method} ${request.url}');
      }),
      store,
    );

    final ok = await controller.register(
      email: 'citizen@example.com',
      password: 'Password1!',
      fullName: 'Asha Mrema',
      mobile: '+255712345678',
    );

    expect(ok, isTrue);
    expect(controller.isAuthenticated, isTrue);
    expect(controller.user?.id, 'u1');
    expect(store.accessToken, 'access-1');
  });

  test('change password adopts the rotated token pair (Phase 16 revocation)',
      () async {
    final store = InMemoryTokenStore();
    late Map<String, dynamic> changeBody;
    final controller = controllerWith(
      MockClient((request) async {
        if (request.url.path == '/api/v1/auth/login') {
          return http.Response(jsonEncode(validTokens), 200,
              headers: jsonHeaders());
        }
        if (request.url.path == '/api/v1/auth/password/change') {
          changeBody = jsonDecode(request.body) as Map<String, dynamic>;
          // The backend revokes the pre-change pair and returns a new one.
          return http.Response(
              jsonEncode({
                'accessToken': 'access-2',
                'refreshToken': 'refresh-2',
                'accessTokenExpiresIn': 900,
                'user': validUser,
              }),
              200,
              headers: jsonHeaders());
        }
        fail('Unexpected request: ${request.method} ${request.url}');
      }),
      store,
    );
    await controller.login('citizen@example.com', 'Password1!');

    final ok = await controller.changePassword(
        currentPassword: 'Password1!', newPassword: 'NewPassword1');

    expect(ok, isTrue);
    expect(changeBody['currentPassword'], 'Password1!');
    expect(changeBody['newPassword'], 'NewPassword1');
    expect(store.accessToken, 'access-2',
        reason: 'rotated access token must be persisted');
    expect(store.refreshToken, 'refresh-2',
        reason: 'rotated refresh token must be persisted');
    expect(controller.isAuthenticated, isTrue);
  });

  test('logout posts the refresh token and clears the session', () async {
    final store = InMemoryTokenStore();
    String? loggedOutRefreshToken;
    final controller = controllerWith(
      MockClient((request) async {
        if (request.url.path == '/api/v1/auth/login') {
          return http.Response(jsonEncode(validTokens), 200,
              headers: jsonHeaders());
        }
        if (request.url.path == '/api/v1/auth/logout') {
          final payload = jsonDecode(request.body) as Map<String, dynamic>;
          loggedOutRefreshToken = payload['refreshToken'] as String?;
          return http.Response('', 204);
        }
        fail('Unexpected request: ${request.method} ${request.url}');
      }),
      store,
    );
    await controller.login('citizen@example.com', 'Password1!');

    await controller.logout();

    expect(loggedOutRefreshToken, 'refresh-1');
    expect(controller.isAuthenticated, isFalse);
    expect(controller.state.status, AuthStatus.unauthenticated);
    expect(store.accessToken, isNull);
    expect(store.refreshToken, isNull);
  });
}
