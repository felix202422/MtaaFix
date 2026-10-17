import 'package:flutter/material.dart';

/// Consistent floating message for action outcomes and validation failures.
void showAppSnack(
  BuildContext context,
  String message, {
  bool error = false,
}) {
  final scheme = Theme.of(context).colorScheme;
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(message),
      backgroundColor: error ? scheme.error : null,
    ),
  );
}
