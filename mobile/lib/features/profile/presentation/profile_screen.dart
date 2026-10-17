import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/widgets/snackbars.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  static String prettyRole(String role) {
    return switch (role) {
      'CITIZEN' => 'Citizen',
      'MODERATOR' => 'Moderator',
      'ADMINISTRATOR' => 'Administrator',
      'FIELD_WORKER' => 'Field worker',
      'ORGANIZATION_ADMIN' => 'Organization admin',
      _ => role,
    };
  }

  Future<void> _signOut(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Sign out?'),
        content: const Text('You will need to sign in again to continue.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Sign Out'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;

    final auth = AuthScope.read(context);
    await auth.logout();
    if (!context.mounted) return;
    Navigator.of(context).pushNamedAndRemoveUntil(
      Routes.login,
      (route) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = AuthScope.of(context);
    final user = auth.user;
    final scheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: ListView(
        children: [
          Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              children: [
                CircleAvatar(
                  radius: 44,
                  backgroundColor: scheme.primary,
                  child: Icon(
                    Icons.person,
                    color: scheme.onPrimary,
                    size: 48,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  user?.fullName ?? '—',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                Text(
                  user?.email ?? '—',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          ),
          ListTile(
            leading: const Icon(Icons.email_rounded),
            title: const Text('Email'),
            subtitle: Text(user?.email ?? '—'),
          ),
          ListTile(
            leading: const Icon(Icons.phone_rounded),
            title: const Text('Mobile'),
            subtitle: Text(
              (user?.mobile == null || user!.mobile!.isEmpty)
                  ? '—'
                  : user.mobile!,
            ),
          ),
          ListTile(
            leading: const Icon(Icons.badge_rounded),
            title: const Text('Role'),
            subtitle: Text(prettyRole(user?.role ?? '')),
          ),
          ListTile(
            leading: const Icon(Icons.location_on_rounded),
            title: const Text('Organisation'),
            subtitle: Text(user?.organisationId ?? '—'),
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.settings_rounded),
            title: const Text('Settings'),
            onTap: () {
              showAppSnack(context, 'Settings are not available yet.');
            },
          ),
          ListTile(
            leading: Icon(Icons.logout_rounded, color: scheme.error),
            title: Text('Sign Out', style: TextStyle(color: scheme.error)),
            onTap: () => _signOut(context),
          ),
        ],
      ),
    );
  }
}
