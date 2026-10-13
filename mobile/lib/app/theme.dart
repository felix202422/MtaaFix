import 'package:flutter/material.dart';

abstract final class MtaaFixColors {
  static const Color primaryGreen = Color(0xFF1EB53A);
  static const Color deepGreen = Color(0xFF15803D);
  static const Color flagYellow = Color(0xFFFCD116);
  static const Color primaryBlack = Color(0xFF111111);
  static const Color flagBlue = Color(0xFF00A3DD);
  static const Color surface = Color(0xFFFFFFFF);
  static const Color canvas = Color(0xFFF5F7FA);
  static const Color border = Color(0xFFE2E8F0);
  static const Color mutedText = Color(0xFF64748B);
  static const Color errorRed = Color(0xFFDC2626);
  static const Color warningAmber = Color(0xFFD97706);
}

ThemeData buildMtaaFixTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: MtaaFixColors.primaryGreen,
    primary: MtaaFixColors.primaryGreen,
    primaryContainer: MtaaFixColors.deepGreen,
    secondary: MtaaFixColors.flagBlue,
    surface: MtaaFixColors.surface,
    error: MtaaFixColors.errorRed,
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: MtaaFixColors.canvas,
    fontFamily: 'Inter',
    appBarTheme: const AppBarTheme(
      backgroundColor: MtaaFixColors.surface,
      foregroundColor: MtaaFixColors.primaryBlack,
      elevation: 0,
      centerTitle: false,
    ),
    cardTheme: CardThemeData(
      color: MtaaFixColors.surface,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: MtaaFixColors.border),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: MtaaFixColors.surface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: MtaaFixColors.border),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: MtaaFixColors.border),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: MtaaFixColors.flagBlue, width: 2),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: MtaaFixColors.errorRed),
      ),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: MtaaFixColors.primaryGreen,
        foregroundColor: MtaaFixColors.surface,
        minimumSize: const Size.fromHeight(48),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        textStyle: const TextStyle(fontWeight: FontWeight.w600),
      ),
    ),
  );
}
