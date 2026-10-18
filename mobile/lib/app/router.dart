import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/not_found_screen.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/register_screen.dart';
import 'package:mtaafix_mobile/features/map/presentation/map_screen.dart';
import 'package:mtaafix_mobile/features/notifications/presentation/notifications_screen.dart';
import 'package:mtaafix_mobile/features/onboarding/presentation/onboarding_screen.dart';
import 'package:mtaafix_mobile/features/profile/presentation/profile_screen.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_detail_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_form_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';
import 'package:mtaafix_mobile/features/splash/presentation/splash_screen.dart';

/// Central route table (named routes, Navigator 1.0).
abstract final class Routes {
  static const String splash = '/';
  static const String onboarding = '/onboarding';
  static const String home = '/home';
  static const String login = '/login';
  static const String register = '/register';
  static const String reports = '/reports';
  static const String reportNew = '/reports/new';
  static const String reportDetail = '/report-details';
  static const String map = '/map';
  static const String notifications = '/notifications';
  static const String profile = '/profile';

  /// Screens that require an authenticated user.
  static const Set<String> protected = <String>{
    home,
    reports,
    reportNew,
    reportDetail,
    map,
    notifications,
    profile,
  };
}

/// Builds the route for [settings] with auth guards applied:
/// * protected screen while signed out → sign-in,
/// * sign-in/register/onboarding while signed in → the home shell,
/// * unknown names → a 404 screen (never a red screen).
MaterialPageRoute<dynamic> generateRoute(
  RouteSettings settings, {
  required AuthController auth,
}) {
  final name = settings.name ?? Routes.splash;
  return MaterialPageRoute<dynamic>(
    settings: settings,
    builder: (_) => _resolve(name, auth, settings.arguments),
  );
}

Widget _resolve(String name, AuthController auth, Object? arguments) {
  if (Routes.protected.contains(name) && !auth.isAuthenticated) {
    return const LoginScreen();
  }
  if ((name == Routes.login ||
          name == Routes.register ||
          name == Routes.onboarding) &&
      auth.isAuthenticated) {
    return const MainShellScreen();
  }
  return switch (name) {
    Routes.splash => const SplashScreen(),
    Routes.onboarding => const OnboardingScreen(),
    Routes.home => const MainShellScreen(),
    Routes.login => const LoginScreen(),
    Routes.register => const RegisterScreen(),
    Routes.reports => const ReportListScreen(),
    Routes.reportNew =>
      ReportFormScreen(editReport: arguments is Report ? arguments : null),
    Routes.reportDetail => const ReportDetailScreen(),
    Routes.map => const MapScreen(),
    Routes.notifications => const NotificationsScreen(),
    Routes.profile => const ProfileScreen(),
    _ => const NotFoundScreen(),
  };
}
