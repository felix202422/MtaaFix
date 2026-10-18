import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/onboarding/presentation/onboarding_screen.dart';

/// Brand screen shown while the persisted session is restored, then routes to
/// onboarding (first launch), the home shell (signed in) or the sign-in
/// screen.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  bool _bootstrapStarted = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_bootstrapStarted) return;
    _bootstrapStarted = true;
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    final auth = AuthScope.read(context);
    final prefs = await SharedPreferences.getInstance();
    final onboardingSeen =
        prefs.getBool(OnboardingScreen.seenKey) ?? false;
    await auth.init();
    if (!mounted) return;
    if (!onboardingSeen) {
      Navigator.of(context).pushReplacementNamed(Routes.onboarding);
      return;
    }
    Navigator.of(context).pushReplacementNamed(
      auth.isAuthenticated ? Routes.home : Routes.login,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              MtaaFixColors.primaryGreen,
              MtaaFixColors.deepGreen,
            ],
          ),
        ),
        child: const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.report_problem_rounded,
                size: 80,
                color: Colors.white,
              ),
              SizedBox(height: 24),
              Text(
                'MtaaFix',
                style: TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              SizedBox(height: 8),
              Text(
                'See It. Report It. Fix It.',
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.white,
                ),
              ),
              SizedBox(height: 40),
              CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
              ),
              SizedBox(height: 16),
              Text(
                'Loading...',
                style: TextStyle(
                  color: Colors.white,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
