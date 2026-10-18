import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/util/formatters.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';
import 'package:mtaafix_mobile/core/widgets/loading_view.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/notifications/application/notifications_api.dart';
import 'package:mtaafix_mobile/features/notifications/domain/notification_item.dart';

/// Notifications (Page 35): real feed from `GET /notifications` with unread
/// styling, mark-as-read, mark-all-read and deep links to report details.
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  late NotificationsApi _api;
  bool _apiReady = false;
  bool _loading = true;
  bool _loadingMore = false;
  bool _markingAll = false;
  String? _error;

  final List<NotificationItem> _items = [];
  int _page = 0;
  int _totalPages = 1;
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
    if (!_apiReady) {
      _apiReady = true;
      _api = NotificationsApi(AuthScope.of(context).api);
      _load();
    }
  }

  void _onTabActivated() {
    if (!mounted || _tab?.value != MainShellTab.notifications) return;
    _load();
  }

  @override
  void dispose() {
    _tab?.removeListener(_onTabActivated);
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await _api.list(page: 0);
      if (!mounted) return;
      setState(() {
        _items
          ..clear()
          ..addAll(result.content);
        _page = 0;
        _totalPages = result.totalPages < 1 ? 1 : result.totalPages;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Could not load notifications. Check your connection.';
      });
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || _page + 1 >= _totalPages) return;
    setState(() => _loadingMore = true);
    try {
      final result = await _api.list(page: _page + 1);
      if (!mounted) return;
      setState(() {
        _items.addAll(result.content);
        _page = result.page;
        _loadingMore = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loadingMore = false);
    }
  }

  int get _unreadCount => _items.where((n) => !n.isRead).length;

  Future<void> _markAllRead() async {
    if (_markingAll || _unreadCount == 0) return;
    setState(() => _markingAll = true);
    try {
      await _api.markAllRead();
      if (!mounted) return;
      setState(() {
        _items
          ..clear()
          ..addAll(_items.map(
            (n) => n.isRead
                ? n
                : NotificationItem(
                    id: n.id,
                    title: n.title,
                    body: n.body,
                    isRead: true,
                    timestamp: n.timestamp,
                    reportId: n.reportId,
                    type: n.type,
                  ),
          ));
        _markingAll = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _markingAll = false);
      _snack('Could not mark notifications as read.', error: true);
    }
  }

  void _snack(String message, {bool error = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: error ? MtaaFixColors.errorRed : null,
        ),
      );
  }

  Future<void> _open(NotificationItem item) async {
    if (!item.isRead) {
      try {
        await _api.markRead(item.id);
        if (mounted) {
          setState(() {
            final index = _items.indexWhere((n) => n.id == item.id);
            if (index >= 0) {
              final n = _items[index];
              _items[index] = NotificationItem(
                id: n.id,
                title: n.title,
                body: n.body,
                isRead: true,
                timestamp: n.timestamp,
                reportId: n.reportId,
                type: n.type,
              );
            }
          });
        }
      } catch (_) {
        // Still navigate — the report is what the user wants.
      }
    }
    final reportId = item.reportId;
    if (reportId != null && reportId.isNotEmpty && mounted) {
      Navigator.pushNamed(context, Routes.reportDetail, arguments: reportId);
    }
  }

  IconData _iconFor(NotificationItem item) {
    final type = item.type?.toUpperCase() ?? '';
    if (type.contains('ASSIGN')) return Icons.assignment_turned_in_rounded;
    if (type.contains('STATUS') || type.contains('UPDATE')) {
      return Icons.sync_rounded;
    }
    if (type.contains('RESOLV') || type.contains('CONFIRM')) {
      return Icons.check_circle_outline_rounded;
    }
    if (type.contains('INFORMATION') || type.contains('REOPEN')) {
      return Icons.info_outline_rounded;
    }
    return Icons.notifications_rounded;
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _loading ? null : _load,
          ),
          IconButton(
            tooltip: 'Mark all read',
            icon: _markingAll
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.done_all_rounded),
            onPressed: _unreadCount == 0 || _markingAll ? null : _markAllRead,
          ),
        ],
      ),
      body: _buildBody(text),
    );
  }

  Widget _buildBody(TextTheme text) {
    if (_loading) return const LoadingView();
    if (_error != null) {
      return FeedbackView(
        icon: Icons.notifications_off_rounded,
        title: 'Notifications unavailable',
        message: _error!,
        actionLabel: 'Retry',
        onAction: _load,
      );
    }
    if (_items.isEmpty) {
      return const FeedbackView(
        icon: Icons.notifications_none_rounded,
        title: 'No notifications',
        message: 'Updates about your reports will appear here.',
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          if (_unreadCount > 0)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              child: Text(
                '$_unreadCount unread',
                style: text.labelMedium
                    ?.copyWith(color: MtaaFixColors.primaryGreen),
              ),
            ),
          for (final item in _items) _NotificationTile(item: item),
          if (_page + 1 < _totalPages)
            Padding(
              padding: const EdgeInsets.all(16),
              child: OutlinedButton(
                onPressed: _loadingMore ? null : _loadMore,
                child: _loadingMore
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Load more'),
              ),
            ),
        ],
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
    final scheme = Theme.of(context).colorScheme;
    final state = context.findAncestorStateOfType<_NotificationsScreenState>();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      color: item.isRead ? null : scheme.primaryContainer.withValues(alpha: 0.35),
      child: ListTile(
        leading: Icon(
          state?._iconFor(item) ?? Icons.notifications_rounded,
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
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              item.body,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            if (item.timestamp != null)
              Text(
                formatDateTime(item.timestamp),
                style: text.labelSmall
                    ?.copyWith(color: MtaaFixColors.mutedText),
              ),
          ],
        ),
        trailing: item.isRead
            ? null
            : Icon(Icons.circle, size: 10, color: scheme.primary),
        onTap: () => state?._open(item),
      ),
    );
  }
}
