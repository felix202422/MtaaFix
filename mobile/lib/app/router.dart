import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/not_found_screen.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/presentation/change_password_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/forgot_password_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/login_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/register_screen.dart';
import 'package:mtaafix_mobile/features/auth/presentation/reset_password_screen.dart';
import 'package:mtaafix_mobile/features/map/presentation/map_screen.dart';
import 'package:mtaafix_mobile/features/notifications/presentation/notifications_screen.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';
import 'package:mtaafix_mobile/features/offline/presentation/pending_submissions_screen.dart';
import 'package:mtaafix_mobile/features/onboarding/presentation/onboarding_screen.dart';
import 'package:mtaafix_mobile/features/profile/presentation/notification_preferences_screen.dart';
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
  static const String forgotPassword = '/forgot-password';
  static const String resetPassword = '/reset-password';
  static const String changePassword = '/change-password';
  static const String reports = '/reports';
  static const String reportNew = '/reports/new';
  static const String reportDetail = '/report-details';
  static const String map = '/map';
  static const String notifications = '/notifications';
  static const String profile = '/profile';
  static const String notificationPreferences = '/notification-preferences';
  static const String pendingSubmissions = '/pending-submissions';

  /// Screens that require an authenticated user.
  static const Set<String> protected = <String>{
    home,
    changePassword,
    reports,
    reportNew,
    reportDetail,
    map,
    notifications,
    profile,
    notificationPreferences,
    pendingSubmissions,
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
          name == Routes.forgotPassword ||
          name == Routes.resetPassword ||
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
    Routes.forgotPassword => const ForgotPasswordScreen(),
    Routes.resetPassword => const ResetPasswordScreen(),
    Routes.changePassword => const ChangePasswordScreen(),
    Routes.reports => const ReportListScreen(),
    Routes.reportNew => ReportFormScreen(
        editReport: arguments is Report ? arguments : null,
        pending: arguments is PendingSubmission ? arguments : null,
      ),
    Routes.reportDetail => const ReportDetailScreen(),
    Routes.map => const MapScreen(),
    Routes.notifications => const NotificationsScreen(),
    Routes.profile => const ProfileScreen(),
    Routes.notificationPreferences => const NotificationPreferencesScreen(),
    Routes.pendingSubmissions => const PendingSubmissionsScreen(),
    _ => const NotFoundScreen(),
  };
}
