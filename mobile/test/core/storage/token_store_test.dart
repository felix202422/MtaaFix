import 'package:flutter_test/flutter_test.dart';

import 'package:mtaafix_mobile/core/storage/token_store.dart';

void main() {
  test('in-memory store round-trips the token pair', () async {
    final store = InMemoryTokenStore();

    expect(await store.readAccessToken(), isNull);
    expect(await store.readRefreshToken(), isNull);

    await store.writeTokens(accessToken: 'a1', refreshToken: 'r1');

    expect(await store.readAccessToken(), 'a1');
    expect(await store.readRefreshToken(), 'r1');
    expect(store.accessToken, 'a1');
    expect(store.refreshToken, 'r1');
  });

  test('in-memory store overwrites and clears tokens', () async {
    final store = InMemoryTokenStore();

    await store.writeTokens(accessToken: 'a1', refreshToken: 'r1');
    await store.writeTokens(accessToken: 'a2', refreshToken: 'r2');
    expect(store.accessToken, 'a2');

    await store.clear();

    expect(store.accessToken, isNull);
    expect(store.refreshToken, isNull);
  });
}
