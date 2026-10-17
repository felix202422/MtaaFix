# MtaaFix Mobile (Flutter)

Cross-platform Flutter application for MtaaFix (Android + iOS). Status:
**Phase 11 — application foundation** (navigation, theme, API/auth plumbing,
platform configuration, initial tests). Citizen feature screens arrive with
Phase 12.

## Requirements

- Flutter 3.47.1 / Dart 3.13.1 (repo baseline)
- Backend running locally: `cd backend && mvn spring-boot:run` (port **8080**)

## Configuration

The API base URL is injected at build/run time via `--dart-define`:

| Target | Base URL |
| ------ | -------- |
| Android emulator (default) | `http://10.0.2.2:8080/api/v1` |
| iOS simulator | `http://localhost:8080/api/v1` |
| Physical device | `http://<your-LAN-IP>:8080/api/v1` |

```bash
# iOS simulator example
flutter run --dart-define=MTAAFIX_API_BASE=http://localhost:8080/api/v1
```

Cleartext HTTP is enabled for development only
(`android:usesCleartextTraffic`, iOS `NSAllowsArbitraryLoads`) — production
must front the API with HTTPS.

## Structure

```
lib/
  main.dart                 entry point (portrait lock, runApp)
  app.dart                  MaterialApp + AuthScope (DI for the auth controller)
  app/router.dart           named route table + auth guards + 404 screen
  app/theme.dart            MtaaFixColors + Material 3 theme
  core/network/             ApiClient (bearer, refresh-on-401, typed ApiException)
  core/storage/             TokenStore (flutter_secure_storage / in-memory tests)
  core/widgets/             LoadingView, FeedbackView, showAppSnack
  features/<name>/{application,domain,presentation}
```

## Development gates

```bash
flutter pub get
flutter analyze      # must be clean
flutter test         # unit + widget tests
flutter build apk --debug   # Android build (SDK licenses required)
```

## Notes

- Tokens are stored encrypted at rest (`flutter_secure_storage`).
- Route guards: `/reports`, `/reports/new`, `/notifications`, `/profile`
  require an authenticated session; unknown routes render a 404 screen.
- Android builds require accepted SDK licenses
  (`flutter doctor --android-licenses`); iOS builds require macOS/Xcode and
  are not produced in this Windows environment.
