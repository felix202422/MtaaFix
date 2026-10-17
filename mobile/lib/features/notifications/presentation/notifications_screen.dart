import 'package:flutter/material.dart';

class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Notifications')),
      body: ListView.builder(
        itemCount: 5,
        itemBuilder: (context, index) {
          return ListTile(
            leading: CircleAvatar(
              backgroundColor: colors.primaryContainer,
              child: Icon(
                Icons.notifications_active_rounded,
                color: colors.onPrimaryContainer,
              ),
            ),
            title: const Text('New report assigned'),
            subtitle:
                const Text('Your report has been assigned to a moderator'),
            trailing: Icon(Icons.check_circle, color: colors.primary),
          );
        },
      ),
    );
  }
}
