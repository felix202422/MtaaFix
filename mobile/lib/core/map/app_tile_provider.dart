import 'dart:io';

import 'package:flutter_map/flutter_map.dart';

/// Tile provider used by every [TileLayer] in the app.
///
/// Production keeps the built-in disk cache (fewer tile downloads). Under
/// `flutter test` — the flutter tool sets `FLUTTER_TEST`, the same hook
/// `flutter_test` itself keys off — caching is disabled: the cache runs a
/// background worker isolate that the fake-async test runner cannot finish
/// while it is alive (Phase 15 quality runs).
TileProvider appTileProvider() => NetworkTileProvider(
      cachingProvider: Platform.environment.containsKey('FLUTTER_TEST')
          ? const DisabledMapCachingProvider()
          : null,
    );
