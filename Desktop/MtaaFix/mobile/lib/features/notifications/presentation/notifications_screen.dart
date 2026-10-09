import 'package:flutter/material.dart';

class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        backgroundColor: Colors.orange.shade600,
      ),
      body: ListView.builder(
        itemCount: 5,
        itemBuilder: (context, index) {
          return ListTile(
            leading: CircleAvatar(
              backgroundColor: Colors.orange.shade100,
              child: Icon(
                Icons.notifications_active_rounded,
                color: Colors.orange.shade600,
              ),
            ),
            title: const Text('New report assigned'),
            subtitle: const Text('Your report has been assigned to a moderator'),
            trailing: const Icon(Icons.check_circle, color: Colors.green),
          );
        },
      ),
    );
  }
}
