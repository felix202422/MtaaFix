import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';

class ReportListScreen extends StatelessWidget {
  const ReportListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('My Reports')),
      body: FeedbackView(
        icon: Icons.list_alt_rounded,
        title: 'No reports yet',
        message: 'Report an issue to help your community',
        actionLabel: 'Report an Issue',
        onAction: () {
          Navigator.pushNamed(context, Routes.reportNew);
        },
      ),
    );
  }
}
