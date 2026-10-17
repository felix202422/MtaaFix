import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/not_found_screen.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/register_screen.dart';
import 'package:mtaafix_mobile/features/notifications/presentation/notifications_screen.dart';
import 'package:mtaafix_mobile/features/profile/presentation/profile_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_form_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';
import 'package:mtaafix_mobile/features/splash/presentation/splash_screen.dart';

/// Central route table (named routes, Navigator 1.0).
abstract final class Routes {
  static const String splash = '/';
  static const String login = '/login';
  static const String register = '/register';
  static const String reports = '/reports';
  static const String reportNew = '/reports/new';
  static const String notifications = '/notifications';
  static const String profile = '/profile';

  /// Screens that require an authenticated user.
  static const Set<String> protected = <String>{
    reports,
    reportNew,
    notifications,
    profile,
  };
}

/// Builds the route for [settings] with auth guards applied:
/// * protected screen while signed out → sign-in,
/// * sign-in/register while signed in → the report list,
/// * unknown names → a 404 screen (never a red screen).
MaterialPageRoute<dynamic> generateRoute(
  RouteSettings settings, {
  required AuthController auth,
}) {
  final name = settings.name ?? Routes.splash;
  return MaterialPageRoute<dynamic>(
    settings: settings,
    builder: (_) => _resolve(name, auth),
  );
}

Widget _resolve(String name, AuthController auth) {
  if (Routes.protected.contains(name) && !auth.isAuthenticated) {
    return const LoginScreen();
  }
  if ((name == Routes.login || name == Routes.register) && auth.isAuthenticated) {
    return const ReportListScreen();
  }
  return switch (name) {
    Routes.splash => const SplashScreen(),
    Routes.login => const LoginScreen(),
    Routes.register => const RegisterScreen(),
    Routes.reports => const ReportListScreen(),
    Routes.reportNew => const ReportFormScreen(),
    Routes.notifications => const NotificationsScreen(),
    Routes.profile => const ProfileScreen(),
    _ => const NotFoundScreen(),
  };
}
