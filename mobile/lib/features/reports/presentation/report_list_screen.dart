import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/status_theme.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/util/formatters.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';
import 'package:mtaafix_mobile/core/widgets/loading_view.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_controller.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_scope.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// My Reports (Page 32): report cards with code/status/date, status + category
/// filters, text search and pagination (FR-2.4).
class ReportListScreen extends StatefulWidget {
  const ReportListScreen({super.key});

  @override
  State<ReportListScreen> createState() => _ReportListScreenState();
}

class _ReportListScreenState extends State<ReportListScreen> {
  late ReportsApi _api;
  bool _apiReady = false;
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  String? _actionError;

  final List<Report> _items = [];
  int _page = 0;
  int _totalPages = 1;
  int _totalElements = 0;

  String? _statusFilter;
  String? _categoryFilter;
  final TextEditingController _search = TextEditingController();
  List<Category> _categories = const [];
  ValueNotifier<int>? _tab;
  OfflineController? _offline;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final tab = MainShellTab.maybeOf(context);
    if (!identical(tab, _tab)) {
      _tab?.removeListener(_onTabActivated);
      _tab = tab;
      _tab?.addListener(_onTabActivated);
    }
    // Listen (non-dependency) so flush completions can refresh the list
    // without rebuilding on every queue change — the banner below uses
    // [OfflineScope.maybeOf] for rebuilds.
    final offline = OfflineScope.read(context);
    if (!identical(offline, _offline)) {
      _offline?.removeListener(_onOfflineChanged);
      _offline = offline;
      _offline?.addListener(_onOfflineChanged);
    }
    if (!_apiReady) {
      _apiReady = true;
      _api = ReportsApi(AuthScope.of(context).api);
      _load();
      _api.categories().then((value) {
        if (mounted) setState(() => _categories = value);
      }).catchError((_) {
        if (mounted) setState(() => _categories = const []);
      });
    }
  }

  void _onTabActivated() {
    if (!mounted || _tab?.value != MainShellTab.reports) return;
    _load();
  }

  void _onOfflineChanged() {
    if (!mounted) return;
    // A flush filed pending reports — reload so they show up.
    if (_offline?.consumeJustSubmitted().isNotEmpty ?? false) _load();
  }

  @override
  void dispose() {
    _tab?.removeListener(_onTabActivated);
    _offline?.removeListener(_onOfflineChanged);
    _search.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
      _actionError = null;
    });
    try {
      final result = await _api.list(
        status: _statusFilter,
        categoryId: _categoryFilter,
        q: _search.text,
        page: 0,
      );
      if (!mounted) return;
      setState(() {
        _items
          ..clear()
          ..addAll(result.content);
        _page = 0;
        _totalPages = result.totalPages < 1 ? 1 : result.totalPages;
        _totalElements = result.totalElements;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Could not load your reports. Check your connection.';
      });
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || _page + 1 >= _totalPages) return;
    setState(() => _loadingMore = true);
    try {
      final result = await _api.list(
        status: _statusFilter,
        categoryId: _categoryFilter,
        q: _search.text,
        page: _page + 1,
      );
      if (!mounted) return;
      setState(() {
        _items.addAll(result.content);
        _page = result.page;
        _loadingMore = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loadingMore = false;
        _actionError = 'Could not load more reports.';
      });
    }
  }

  void _applyFilters({String? status, String? category}) {
    setState(() {
      _statusFilter = status;
      _categoryFilter = category;
    });
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Reports'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _loading ? null : _load,
          ),
        ],
      ),
      body: Column(
        children: [
          _PendingReportsBanner(onOpen: () => Navigator.pushNamed(
              context, Routes.pendingSubmissions)),
          _buildFilters(scheme),
          if (_actionError != null)
            MaterialBanner(
              content: Text(_actionError!),
              actions: [
                TextButton(
                  onPressed: () => setState(() => _actionError = null),
                  child: const Text('Dismiss'),
                ),
              ],
            ),
          Expanded(child: _buildBody()),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => Navigator.pushNamed(context, Routes.reportNew),
        icon: const Icon(Icons.add_rounded),
        label: const Text('New report'),
      ),
    );
  }

  Widget _buildFilters(ColorScheme scheme) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Column(
        children: [
          TextField(
            controller: _search,
            textInputAction: TextInputAction.search,
            decoration: const InputDecoration(
              hintText: 'Search by title or reference…',
              prefixIcon: Icon(Icons.search_rounded),
            ),
            onSubmitted: (_) => _load(),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<String?>(
                  initialValue: _statusFilter,
                  decoration: const InputDecoration(labelText: 'Status'),
                  isExpanded: true,
                  items: [
                    const DropdownMenuItem<String?>(
                        value: null, child: Text('All statuses')),
                    for (final status in ReportStatus.values)
                      DropdownMenuItem<String?>(
                        value: status.wire,
                        child: Text(statusLabel(status)),
                      ),
                  ],
                  onChanged: (value) => _applyFilters(status: value),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: DropdownButtonFormField<String?>(
                  initialValue: _categoryFilter,
                  decoration: const InputDecoration(labelText: 'Category'),
                  isExpanded: true,
                  items: [
                    const DropdownMenuItem<String?>(
                        value: null, child: Text('All categories')),
                    for (final category in _categories)
                      DropdownMenuItem<String?>(
                        value: category.id,
                        child: Text(
                          category.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                  ],
                  onChanged: (value) => _applyFilters(category: value),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) return const LoadingView();
    if (_error != null) {
      return FeedbackView(
        icon: Icons.cloud_off_rounded,
        title: 'Reports unavailable',
        message: _error!,
        actionLabel: 'Retry',
        onAction: _load,
      );
    }
    if (_items.isEmpty) {
      return FeedbackView(
        icon: Icons.list_alt_rounded,
        title: 'No reports yet',
        message: 'Report an issue to help your community',
        actionLabel: 'Report an Issue',
        onAction: () => Navigator.pushNamed(context, Routes.reportNew),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 88),
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Text(
              _totalElements == 1
                  ? '1 report'
                  : '$_totalElements reports',
              style: Theme.of(context)
                  .textTheme
                  .labelMedium
                  ?.copyWith(color: MtaaFixColors.mutedText),
            ),
          ),
          for (final report in _items) _ReportCard(report: report),
          if (_page + 1 < _totalPages)
            Padding(
              padding: const EdgeInsets.only(top: 4, bottom: 16),
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

/// Amber banner listing reports that exist only on this device (waiting,
/// attaching photos, or failed) — unsent data is never mixed into the
/// server list below without saying so.
class _PendingReportsBanner extends StatelessWidget {
  const _PendingReportsBanner({required this.onOpen});

  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final count = OfflineScope.maybeOf(context)?.unsentCount ?? 0;
    if (count == 0) return const SizedBox.shrink();
    return Material(
      color: MtaaFixColors.warningAmber.withValues(alpha: 0.14),
      child: InkWell(
        onTap: onOpen,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Row(
            children: [
              const Icon(Icons.cloud_off_rounded,
                  size: 16, color: MtaaFixColors.warningAmber),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  count == 1
                      ? '1 report not yet submitted — tap to view'
                      : '$count reports not yet submitted — tap to view',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: MtaaFixColors.warningAmber,
                        fontWeight: FontWeight.w600,
                      ),
                ),
              ),
              const Icon(Icons.chevron_right_rounded,
                  size: 18, color: MtaaFixColors.warningAmber),
            ],
          ),
        ),
      ),
    );
  }
}

class _ReportCard extends StatelessWidget {
  const _ReportCard({required this.report});

  final Report report;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => Navigator.pushNamed(
          context,
          Routes.reportDetail,
          arguments: report.id,
        ),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      report.code,
                      style: text.labelMedium?.copyWith(
                        color: MtaaFixColors.primaryGreen,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  StatusChip(status: report.status, dense: true),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                report.title,
                style: text.titleMedium,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              if (report.description != null &&
                  report.description!.trim().isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(
                  report.description!.trim(),
                  style: text.bodySmall?.copyWith(
                    color: MtaaFixColors.mutedText,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 10),
              Row(
                children: [
                  const Icon(Icons.schedule_rounded,
                      size: 14, color: MtaaFixColors.mutedText),
                  const SizedBox(width: 4),
                  Text(
                    formatDate(report.reportedAt),
                    style: text.labelSmall
                        ?.copyWith(color: MtaaFixColors.mutedText),
                  ),
                  const Spacer(),
                  if (report.media.isNotEmpty) ...[
                    const Icon(Icons.photo_rounded,
                        size: 14, color: MtaaFixColors.mutedText),
                    const SizedBox(width: 4),
                    Text(
                      '${report.media.length}',
                      style: text.labelSmall
                          ?.copyWith(color: MtaaFixColors.mutedText),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
