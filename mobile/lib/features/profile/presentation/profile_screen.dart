import 'package:flutter/material.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
        backgroundColor: Colors.orange.shade600,
      ),
      body: ListView(
        children: [
          const Padding(
            padding: EdgeInsets.all(16),
            child: CircleAvatar(
              radius: 50,
              backgroundColor: Colors.orange,
              child: Icon(Icons.person, color: Colors.white, size: 50),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.email_rounded),
            title: const Text('Email'),
            subtitle: const Text('user@example.com'),
          ),
          ListTile(
            leading: const Icon(Icons.phone_rounded),
            title: const Text('Mobile'),
            subtitle: const Text('+255 712 345 678'),
          ),
          ListTile(
            leading: const Icon(Icons.badge_rounded),
            title: const Text('Role'),
            subtitle: const Text('Citizen'),
          ),
          ListTile(
            leading: const Icon(Icons.location_on_rounded),
            title: const Text('Organisation'),
            subtitle: const Text('MtaaFix'),
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.settings_rounded),
            title: const Text('Settings'),
            onTap: () {},
          ),
          ListTile(
            leading: const Icon(Icons.logout_rounded),
            title: const Text('Sign Out'),
            onTap: () {
              // Handle sign out
            },
          ),
        ],
      ),
    );
  }
}
