import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:mtaafix_mobile/app.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // Set preferred orientation to portrait for both platforms
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  runApp(const MtaaFixMobileApp());
}
