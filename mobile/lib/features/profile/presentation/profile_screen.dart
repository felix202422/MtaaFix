import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/auth/domain/user.dart';

/// Profile (Page 36): account details, editable name/mobile (FR-5.x),
/// privacy information, about and sign-out.
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

  Future<void> _editProfile(BuildContext context) async {
    final auth = AuthScope.read(context);
    final user = auth.user;
    final nameController = TextEditingController(text: user?.fullName ?? '');
    final mobileController = TextEditingController(text: user?.mobile ?? '');

    final saved = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => _EditProfileDialog(
        nameController: nameController,
        mobileController: mobileController,
      ),
    );
    if (saved != true || !context.mounted) return;

    final messenger = ScaffoldMessenger.of(context);
    final ok = await auth.updateProfile(
      fullName: nameController.text.trim(),
      mobile: mobileController.text.trim(),
    );
    if (!context.mounted) return;
    messenger.showSnackBar(
      SnackBar(
        content: Text(ok ? 'Profile updated.' : 'Could not update profile.'),
        backgroundColor:
            ok ? null : MtaaFixColors.errorRed,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = AuthScope.of(context);
    final user = auth.user;
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
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
                  style: text.titleLarge,
                ),
                Text(
                  user?.email ?? '—',
                  style: text.bodyMedium,
                ),
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  onPressed: () => _editProfile(context),
                  icon: const Icon(Icons.edit_rounded, size: 18),
                  label: const Text('Edit profile'),
                ),
              ],
            ),
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.email_rounded),
            title: const Text('Email'),
            subtitle: Text(user?.email ?? '—'),
          ),
          ListTile(
            leading: const Icon(Icons.phone_rounded),
            title: const Text('Mobile'),
            subtitle: Text(_mobile(user)),
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
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Text('Privacy', style: text.titleMedium),
          ),
          const Card(
            margin: EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Padding(
              padding: EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _PrivacyLine(
                    icon: Icons.report_gmailerrorred_rounded,
                    text:
                        'Your reports are visible to moderators and, in '
                        'anonymised form on the public map.',
                  ),
                  _PrivacyLine(
                    icon: Icons.location_on_outlined,
                    text:
                        'Exact coordinates are attached to a report so the '
                        'team can find the issue.',
                  ),
                  _PrivacyLine(
                    icon: Icons.storage_rounded,
                    text:
                        'Your session token is stored securely on this device '
                        'until you sign out.',
                  ),
                ],
              ),
            ),
          ),
          const ListTile(
            leading: Icon(Icons.notifications_active_outlined),
            title: Text('Notification preferences'),
            subtitle: Text(
              'Delivery channels and quiet hours arrive with push '
              'notifications in the next phase.',
            ),
          ),
          const ListTile(
            leading: Icon(Icons.info_outline_rounded),
            title: Text('About MtaaFix'),
            subtitle: Text(
              'MtaaFix — See it. Report it. Fix it. Community reporting for '
              'cleaner, safer streets.',
            ),
          ),
          const SizedBox(height: 8),
          ListTile(
            leading: Icon(Icons.logout_rounded, color: scheme.error),
            title: Text('Sign Out', style: TextStyle(color: scheme.error)),
            onTap: () => _signOut(context),
          ),
          const SizedBox(height: 24),
        ],
        ),
      ),
    );
  }

  String _mobile(User? user) {
    final mobile = user?.mobile;
    return (mobile == null || mobile.isEmpty) ? '—' : mobile;
  }
}

class _PrivacyLine extends StatelessWidget {
  const _PrivacyLine({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: MtaaFixColors.primaryGreen),
          const SizedBox(width: 8),
          Expanded(
            child: Text(text, style: Theme.of(context).textTheme.bodySmall),
          ),
        ],
      ),
    );
  }
}

class _EditProfileDialog extends StatelessWidget {
  const _EditProfileDialog({
    required this.nameController,
    required this.mobileController,
  });

  final TextEditingController nameController;
  final TextEditingController mobileController;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Edit profile'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: nameController,
            textCapitalization: TextCapitalization.words,
            decoration: const InputDecoration(labelText: 'Full name'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: mobileController,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(labelText: 'Mobile number'),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            if (nameController.text.trim().isEmpty) return;
            Navigator.pop(context, true);
          },
          child: const Text('Save'),
        ),
      ],
    );
  }
}
