import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'package:mtaafix_mobile/app/not_found_screen.dart';
import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';

class MtaaFixMobileApp extends StatefulWidget {
  const MtaaFixMobileApp({super.key, this.apiClient});

  /// Injectable for tests; production uses the default ([ApiClient] with
  /// secure token storage).
  final ApiClient? apiClient;

  @override
  State<MtaaFixMobileApp> createState() => _MtaaFixMobileAppState();
}

class _MtaaFixMobileAppState extends State<MtaaFixMobileApp> {
  late final ApiClient _api = widget.apiClient ?? ApiClient();
  late final AuthController _auth = AuthController(_api);

  @override
  void dispose() {
    _auth.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AuthScope(
      controller: _auth,
      child: MaterialApp(
        title: 'MtaaFix',
        debugShowCheckedModeBanner: false,
        theme: buildMtaaFixTheme(),
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: const [
          Locale('en'),
          Locale('sw'),
          Locale('am'),
          Locale('ti'),
        ],
        initialRoute: Routes.splash,
        onGenerateRoute: (settings) => generateRoute(settings, auth: _auth),
        onUnknownRoute: (settings) => MaterialPageRoute<dynamic>(
          settings: settings,
          builder: (_) => const NotFoundScreen(),
        ),
      ),
    );
  }
}
