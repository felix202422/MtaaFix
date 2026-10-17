import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';

/// Rendered for any unknown route instead of a red error screen.
class NotFoundScreen extends StatelessWidget {
  const NotFoundScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Not found')),
      body: FeedbackView(
        icon: Icons.explore_off_rounded,
        title: 'Page not found',
        message: 'The screen you tried to open does not exist.',
        actionLabel: 'Go to My Reports',
        onAction: () {
          Navigator.of(context).pushNamedAndRemoveUntil(
            Routes.reports,
            (route) => false,
          );
        },
      ),
    );
  }
}
