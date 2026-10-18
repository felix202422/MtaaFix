import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/status_theme.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/util/formatters.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';
import 'package:mtaafix_mobile/core/widgets/loading_view.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/auth/domain/user.dart';
import 'package:mtaafix_mobile/features/notifications/application/notifications_api.dart';
import 'package:mtaafix_mobile/features/notifications/domain/notification_item.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// Mobile Home (Page 30): branding, welcome, prominent report action, report
/// summaries, recent reports, notifications entry.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool _loading = true;
  bool _initiated = false;
  String? _error;
  Map<String, int> _stats = const {};
  List<Report> _recentReports = const [];
  List<NotificationItem> _recentNotifications = const [];
  ValueNotifier<int>? _tab;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final tab = MainShellTab.maybeOf(context);
    if (!identical(tab, _tab)) {
      _tab?.removeListener(_onTabActivated);
      _tab = tab;
      _tab?.addListener(_onTabActivated);
    }
    if (!_initiated) {
      _initiated = true;
      _load();
    }
  }

  @override
  void dispose() {
    _tab?.removeListener(_onTabActivated);
    super.dispose();
  }

  void _onTabActivated() {
    if (!mounted || _tab?.value != MainShellTab.home) return;
    _load(refreshing: true);
  }

  Future<void> _load({bool refreshing = false}) async {
    if (!refreshing) setState(() { _loading = true; _error = null; });
    final auth = AuthScope.of(context);
    final reports = ReportsApi(auth.api);
    final notifications = NotificationsApi(auth.api);
    try {
      final stats = await reports.dashboardStats();
      final reportPage = await reports.list(size: 5);
      final notificationPage = await notifications.list(size: 3);
      if (!mounted) return;
      setState(() {
        _stats = stats;
        _recentReports = reportPage.content;
        _recentNotifications = notificationPage.content;
        _loading = false;
        _error = null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Could not load your home feed. Pull to retry.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = AuthScope.of(context).user;
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('MtaaFix'),
        actions: [
          IconButton(
            tooltip: 'Community map',
            icon: const Icon(Icons.map_rounded),
            onPressed: () => Navigator.pushNamed(context, Routes.map),
          ),
          IconButton(
            tooltip: 'Profile',
            icon: const Icon(Icons.person_outline_rounded),
            onPressed: () => Navigator.pushNamed(context, Routes.profile),
          ),
        ],
      ),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? FeedbackView(
                  icon: Icons.wifi_off_rounded,
                  title: 'Home unavailable',
                  message: _error!,
                  actionLabel: 'Retry',
                  onAction: () => _load(),
                )
              : RefreshIndicator(
                  onRefresh: () => _load(refreshing: true),
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _WelcomeCard(name: _firstName(user), scheme: scheme),
                        const SizedBox(height: 16),
                        SizedBox(
                          height: 52,
                          child: ElevatedButton.icon(
                            onPressed: () => Navigator.pushNamed(
                                context, Routes.reportNew),
                            icon: const Icon(Icons.add_a_photo_rounded),
                            label: const Text('Report an Issue'),
                          ),
                        ),
                        const SizedBox(height: 24),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Text('Your reports', style: text.titleMedium),
                        ),
                        const SizedBox(height: 10),
                        _StatsGrid(stats: _stats, text: text),
                        const SizedBox(height: 24),
                        _SectionHeader(
                          title: 'Recent reports',
                          actionLabel: 'See all',
                          onAction: () =>
                              Navigator.pushNamed(context, Routes.reports),
                        ),
                        if (_recentReports.isEmpty)
                          _EmptyMini(
                            message:
                                'No reports yet — your submissions will show up here.',
                            onAction: () => Navigator.pushNamed(
                                context, Routes.reportNew),
                          )
                        else
                          Column(
                            children: [
                              for (final report in _recentReports.take(3))
                                _RecentReportTile(report: report),
                            ],
                          ),
                        const SizedBox(height: 24),
                        _SectionHeader(
                          title: 'Notifications',
                          actionLabel: 'See all',
                          onAction: () =>
                              Navigator.pushNamed(context, Routes.notifications),
                        ),
                        if (_recentNotifications.isEmpty)
                          const _EmptyMini(
                            message: 'You are all caught up.',
                          )
                        else
                          Column(
                            children: [
                              for (final item in _recentNotifications)
                                _NotificationTile(item: item),
                            ],
                          ),
                        const SizedBox(height: 24),
                      ],
                    ),
                  ),
                ),
    );
  }

  String _firstName(User? user) {
    final name = user?.fullName.trim();
    if (name == null || name.isEmpty) return 'there';
    return name.split(RegExp(r'\s+')).first;
  }
}

class _WelcomeCard extends StatelessWidget {
  const _WelcomeCard({required this.name, required this.scheme});

  final String name;
  final ColorScheme scheme;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [MtaaFixColors.primaryGreen, MtaaFixColors.deepGreen],
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Karibu, $name',
            style: text.titleLarge?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'See it. Report it. Fix it. — together we keep our streets moving.',
            style: text.bodyMedium?.copyWith(color: Colors.white),
          ),
        ],
      ),
    );
  }
}

class _StatsGrid extends StatelessWidget {
  const _StatsGrid({required this.stats, required this.text});

  final Map<String, int> stats;
  final TextTheme text;

  @override
  Widget build(BuildContext context) {
    final total = stats.values.fold<int>(0, (sum, value) => sum + value);
    if (stats.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Text(
            'No report activity yet.',
            style: text.bodyMedium,
          ),
        ),
      );
    }
    final entries = stats.entries.where((e) => e.value > 0).toList()
      ..sort((a, b) => b.value.compareTo(a.value));
    final highlights = entries.take(3).toList();
    return Column(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Text('$total', style: text.headlineMedium),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    total == 1 ? 'report submitted' : 'reports submitted',
                    style: text.bodyMedium,
                  ),
                ),
                const Icon(Icons.assignment_turned_in_rounded,
                    color: MtaaFixColors.primaryGreen),
              ],
            ),
          ),
        ),
        if (highlights.isNotEmpty) const SizedBox(height: 8),
        if (highlights.isNotEmpty)
          Row(
            children: [
              for (final entry in highlights)
                Expanded(
                  child: Card(
                    margin: const EdgeInsets.only(right: 8),
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${entry.value}', style: text.titleLarge),
                          const SizedBox(height: 2),
                          Text(
                            statusLabel(ReportStatus.parse(entry.key)),
                            style: text.labelSmall?.copyWith(
                              color: MtaaFixColors.mutedText,
                            ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
            ],
          ),
      ],
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({
    required this.title,
    required this.actionLabel,
    required this.onAction,
  });

  final String title;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        TextButton(onPressed: onAction, child: Text(actionLabel)),
      ],
    );
  }
}

class _RecentReportTile extends StatelessWidget {
  const _RecentReportTile({required this.report});

  final Report report;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text(
          report.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: text.titleSmall,
        ),
        subtitle: Text('${report.code} · ${formatDate(report.reportedAt)}'),
        trailing: StatusChip(status: report.status, dense: true),
        onTap: () => Navigator.pushNamed(
          context,
          Routes.reportDetail,
          arguments: report.id,
        ),
      ),
    );
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({required this.item});

  final NotificationItem item;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(
          item.isRead
              ? Icons.notifications_none_rounded
              : Icons.notifications_active_rounded,
          color: item.isRead
              ? MtaaFixColors.mutedText
              : MtaaFixColors.primaryGreen,
        ),
        title: Text(
          item.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: text.titleSmall?.copyWith(
            fontWeight: item.isRead ? FontWeight.w400 : FontWeight.w700,
          ),
        ),
        subtitle: Text(
          item.body,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Text(
          formatDate(item.timestamp),
          style: text.labelSmall?.copyWith(color: MtaaFixColors.mutedText),
        ),
        onTap: () =>
            Navigator.pushNamed(context, Routes.notifications),
      ),
    );
  }
}

class _EmptyMini extends StatelessWidget {
  const _EmptyMini({required this.message, this.onAction});

  final String message;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Text(message, style: text.bodyMedium, textAlign: TextAlign.center),
            if (onAction != null) ...[
              const SizedBox(height: 8),
              TextButton(onPressed: onAction, child: const Text('Report now')),
            ],
          ],
        ),
      ),
    );
  }
}
