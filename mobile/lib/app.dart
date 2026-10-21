import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'package:mtaafix_mobile/app/not_found_screen.dart';
import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/network/api_client.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/notifications/application/unread_count_controller.dart';
import 'package:mtaafix_mobile/features/notifications/application/unread_count_scope.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_controller.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_scope.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';

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
  late final UnreadCountController _unread = UnreadCountController(_api);
  late final OfflineController _offline = OfflineController(
    // Submissions are executed by the outbox flush against the shared
    // authenticated client (same idempotency key on every retry).
    submit: (item) => ReportsApi(_api).create(
      categoryId: item.categoryId,
      title: item.title,
      description: item.description,
      latitude: item.latitude,
      longitude: item.longitude,
      idempotencyKey: item.idempotencyKey,
    ),
    uploadPhoto: (reportId, path) => ReportsApi(_api).uploadPhoto(reportId, path),
  );

  @override
  void initState() {
    super.initState();
    // Loads the persisted queue and connectivity state; safe to run before
    // first frame (degrades to temp storage when platform channels are
    // unavailable, e.g. widget tests).
    _offline.start();
  }

  @override
  void dispose() {
    _auth.dispose();
    _unread.dispose();
    _offline.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AuthScope(
      controller: _auth,
      child: UnreadCountScope(
        controller: _unread,
        child: OfflineScope(
          controller: _offline,
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
        ),
      ),
    );
  }
}
