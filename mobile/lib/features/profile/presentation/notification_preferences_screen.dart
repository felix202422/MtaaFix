import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';
import 'package:mtaafix_mobile/core/widgets/loading_view.dart';
import 'package:mtaafix_mobile/core/widgets/snackbars.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/notifications/application/notifications_api.dart';
import 'package:mtaafix_mobile/features/notifications/domain/notification_preference.dart';

/// Notification preferences (FR-6.4): per-channel opt-in for the signed-in
/// user. The in-app feed is always on (FR-6.1) and push is read-only until a
/// provider is configured on the deployment.
class NotificationPreferencesScreen extends StatefulWidget {
  const NotificationPreferencesScreen({super.key});

  @override
  State<NotificationPreferencesScreen> createState() =>
      _NotificationPreferencesScreenState();
}

class _NotificationPreferencesScreenState
    extends State<NotificationPreferencesScreen> {
  late NotificationsApi _api;
  bool _apiReady = false;
  bool _loading = true;
  bool _saving = false;
  String? _error;
  NotificationPreference? _prefs;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_apiReady) {
      _apiReady = true;
      _api = NotificationsApi(AuthScope.of(context).api);
      _load();
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final prefs = await _api.getPreferences();
      if (!mounted) return;
      setState(() {
        _prefs = prefs;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Could not load your preferences. Check your connection.';
      });
    }
  }

  Future<void> _save({bool? emailEnabled, bool? pushEnabled}) async {
    if (_saving) return;
    setState(() => _saving = true);
    try {
      final prefs = await _api.updatePreferences(
        emailEnabled: emailEnabled,
        pushEnabled: pushEnabled,
      );
      if (!mounted) return;
      setState(() {
        _prefs = prefs;
        _saving = false;
      });
      showAppSnack(context, 'Notification preferences saved.');
    } catch (_) {
      if (!mounted) return;
      setState(() => _saving = false);
      // Reload so the switches reflect the server's truth again.
      await _load();
      if (!mounted) return;
      showAppSnack(context, 'Could not save your preferences.', error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Notification preferences')),
      body: _buildBody(text),
    );
  }

  Widget _buildBody(TextTheme text) {
    if (_loading) return const LoadingView();
    final error = _error;
    if (error != null) {
      return FeedbackView(
        icon: Icons.notifications_off_rounded,
        title: 'Preferences unavailable',
        message: error,
        actionLabel: 'Retry',
        onAction: _load,
      );
    }
    final prefs = _prefs;
    if (prefs == null) return const LoadingView();
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 8),
      children: [
        const SwitchListTile(
          secondary: Icon(Icons.notifications_active_outlined),
          title: Text('In-app notifications'),
          subtitle: Text(
            'Always on — updates appear in your Alerts feed.',
          ),
          value: true,
          onChanged: null,
        ),
        SwitchListTile(
          secondary: const Icon(Icons.mail_outline_rounded),
          title: const Text('Email notifications'),
          subtitle: const Text('Get an email when your report status changes.'),
          value: prefs.emailEnabled,
          onChanged: _saving
              ? null
              : (value) => _save(emailEnabled: value),
        ),
        SwitchListTile(
          secondary: const Icon(Icons.phone_iphone_rounded),
          title: const Text('Push notifications'),
          subtitle: Text(
            prefs.pushAvailable
                ? 'Receive push notifications on this device.'
                : 'No push provider is configured for this deployment yet.',
          ),
          value: prefs.pushEnabled,
          onChanged: _saving || !prefs.pushAvailable
              ? null
              : (value) => _save(pushEnabled: value),
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Text(
            'Email delivery follows the same content as your in-app '
            'notifications. Push delivery becomes available once a push '
            'provider is configured.',
            style: text.bodySmall?.copyWith(color: MtaaFixColors.mutedText),
          ),
        ),
      ],
    );
  }
}
