import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mtaafix_mobile/app/theme.dart';

void main() {
  test('the mobile theme is Material 3 with brand colours', () {
    final theme = buildMtaaFixTheme();

    expect(theme.useMaterial3, isTrue);
    expect(theme.colorScheme.primary, MtaaFixColors.primaryGreen);
    expect(theme.scaffoldBackgroundColor, MtaaFixColors.canvas);
    expect(theme.appBarTheme.backgroundColor, MtaaFixColors.surface);
    expect(theme.appBarTheme.elevation, 0);
    expect(theme.cardTheme.color, MtaaFixColors.surface);
  });

  test('primary buttons use the brand green with a 48dp minimum height', () {
    final theme = buildMtaaFixTheme();
    final style = theme.elevatedButtonTheme.style;

    expect(style?.backgroundColor?.resolve({}), MtaaFixColors.primaryGreen);
    expect(style?.foregroundColor?.resolve({}), MtaaFixColors.surface);
    expect(style?.minimumSize?.resolve({}), const Size.fromHeight(48));
  });
}
