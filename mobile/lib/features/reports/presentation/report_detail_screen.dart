import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/status_theme.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/core/util/formatters.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';
import 'package:mtaafix_mobile/core/widgets/loading_view.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// Report Details (Page 33): summary, status timeline, location, evidence,
/// comments and the lifecycle actions the citizen may perform (FR-2.5–2.9).
class ReportDetailScreen extends StatefulWidget {
  const ReportDetailScreen({super.key});

  @override
  State<ReportDetailScreen> createState() => _ReportDetailScreenState();
}

class _ReportDetailScreenState extends State<ReportDetailScreen> {
  late ReportsApi _api;
  late Map<String, String> _imageHeaders;
  bool _initiated = false;
  bool _loading = true;
  String? _error;
  Report? _report;
  String? _actionError;
  bool _acting = false;

  final _comment = TextEditingController();
  bool _postingComment = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_initiated) {
      _initiated = true;
      final auth = AuthScope.of(context);
      _api = ReportsApi(auth.api);
      final token = auth.api.accessToken;
      _imageHeaders = token == null ? const {} : {'Authorization': 'Bearer $token'};
      _load();
    }
  }

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  String? get _id {
    final args = ModalRoute.of(context)?.settings.arguments;
    return args is String && args.isNotEmpty ? args : null;
  }

  Future<void> _load() async {
    final id = _id;
    if (id == null) {
      setState(() {
        _loading = false;
        _error = 'This report could not be opened (missing reference).';
      });
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _actionError = null;
    });
    try {
      final report = await _api.get(id);
      if (!mounted) return;
      setState(() {
        _report = report;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e is ApiException
            ? e.message
            : 'Could not load this report. Check your connection.';
      });
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

  Future<void> _runAction(
    Future<Report> Function() action, {
    String success = 'Done.',
  }) async {
    if (_acting) return;
    setState(() {
      _acting = true;
      _actionError = null;
    });
    try {
      final updated = await action();
      if (!mounted) return;
      setState(() {
        _report = updated;
        _acting = false;
      });
      _snack(success);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _acting = false;
        _actionError = e.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _acting = false;
        _actionError = 'That action failed. Please try again.';
      });
    }
  }

  Future<void> _cancelReport() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Cancel this report?'),
        content: const Text(
          'The report will be withdrawn and cannot be recovered.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Keep report'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Cancel report'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    final id = _report?.id;
    if (id == null) return;
    await _runAction(() => _api.cancel(id), success: 'Report cancelled.');
  }

  Future<void> _confirmResolved() async {
    final controller = TextEditingController();
    final feedback = await showDialog<String?>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Confirm resolution'),
        content: TextField(
          controller: controller,
          maxLines: 3,
          decoration: const InputDecoration(
            hintText: 'Optional feedback (how did we do?)',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Skip'),
          ),
          FilledButton(
            onPressed: () =>
                Navigator.pop(dialogContext, controller.text.trim()),
            child: const Text('Confirm'),
          ),
        ],
      ),
    );
    final id = _report?.id;
    if (id == null) return;
    await _runAction(
      () => _api.confirm(id, feedback: feedback),
      success: 'Thanks — resolution confirmed.',
    );
  }

  Future<void> _disputeResolved() async {
    final controller = TextEditingController();
    final reason = await showDialog<String?>(
      context: context,
      builder: (dialogContext) => _DisputeDialog(controller: controller),
    );
    if (reason == null || reason.isEmpty) return;
    final id = _report?.id;
    if (id == null) return;
    await _runAction(
      () => _api.dispute(id, disputeReason: reason),
      success: 'Dispute submitted.',
    );
  }

  Future<void> _resubmit() async {
    final id = _report?.id;
    if (id == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Resubmit report?'),
        content: const Text('The report goes back into review.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Not yet'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Resubmit'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    await _runAction(() => _api.resubmit(id), success: 'Report resubmitted.');
  }

  Future<void> _postComment() async {
    final content = _comment.text.trim();
    if (content.isEmpty || _postingComment) return;
    final id = _report?.id;
    if (id == null) return;
    setState(() => _postingComment = true);
    try {
      final updated = await _api.addComment(id, content);
      if (!mounted) return;
      _comment.clear();
      setState(() {
        _report = updated;
        _postingComment = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _postingComment = false);
      _snack(e.message, error: true);
    } catch (_) {
      if (!mounted) return;
      setState(() => _postingComment = false);
      _snack('Could not post your comment.', error: true);
    }
  }

  void _editReport() {
    final report = _report;
    if (report == null) return;
    Navigator.pushNamed(context, Routes.reportNew, arguments: report)
        .then((_) => _load());
  }

  void _openPhoto(ReportMediaItem media) {
    final url = _api.resolveMediaUrl(media.url);
    showDialog<void>(
      context: context,
      builder: (dialogContext) => Dialog(
        backgroundColor: Colors.black,
        insetPadding: const EdgeInsets.all(12),
        child: Stack(
          children: [
            InteractiveViewer(
              child: Center(
                child: Image.network(
                  url,
                  headers: _imageHeaders,
                  errorBuilder: (_, __, ___) => const Padding(
                    padding: EdgeInsets.all(32),
                    child: Text(
                      'Photo could not be loaded.',
                      style: TextStyle(color: Colors.white),
                    ),
                  ),
                ),
              ),
            ),
            Positioned(
              top: 4,
              right: 4,
              child: IconButton(
                icon: const Icon(Icons.close_rounded, color: Colors.white),
                onPressed: () => Navigator.pop(dialogContext),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: LoadingView());
    }
    final error = _error;
    if (error != null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Report details')),
        body: FeedbackView(
          icon: Icons.error_outline_rounded,
          title: 'Report unavailable',
          message: error,
          actionLabel: 'Retry',
          onAction: _load,
        ),
      );
    }
    final report = _report;
    if (report == null) {
      return const Scaffold(body: SizedBox.shrink());
    }
    final text = Theme.of(context).textTheme;
    final canCancel = report.status == ReportStatus.submitted;
    final canConfirmOrDispute = report.status == ReportStatus.resolved;
    final canResubmit = report.status == ReportStatus.needsInformation;
    final canEdit = report.status == ReportStatus.submitted ||
        report.status == ReportStatus.needsInformation;

    return Scaffold(
      appBar: AppBar(
        title: Text(report.code),
        actions: [
          if (canEdit)
            IconButton(
              tooltip: 'Edit report',
              icon: const Icon(Icons.edit_rounded),
              onPressed: _acting ? null : _editReport,
            ),
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _load,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
            if (_actionError != null)
              Card(
                color: MtaaFixColors.errorRed.withValues(alpha: 0.08),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Text(
                    _actionError!,
                    style: text.bodyMedium
                        ?.copyWith(color: MtaaFixColors.errorRed),
                  ),
                ),
              ),
            Row(
              children: [
                StatusChip(status: report.status),
                const Spacer(),
                Text(
                  formatDate(report.reportedAt),
                  style: text.labelMedium
                      ?.copyWith(color: MtaaFixColors.mutedText),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(report.title, style: text.headlineSmall),
            if (report.description != null &&
                report.description!.trim().isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(report.description!.trim(), style: text.bodyLarge),
            ],
            const SizedBox(height: 16),
            if (report.location != null) _LocationCard(location: report.location!),
            if (report.media.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text('Evidence (${report.media.length})',
                  style: text.titleMedium),
              const SizedBox(height: 8),
              SizedBox(
                height: 96,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: report.media.length,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    final media = report.media[index];
                    return InkWell(
                      onTap: () => _openPhoto(media),
                      borderRadius: BorderRadius.circular(10),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(10),
                        child: Image.network(
                          _api.resolveMediaUrl(media.url),
                          width: 96,
                          height: 96,
                          fit: BoxFit.cover,
                          headers: _imageHeaders,
                          errorBuilder: (_, __, ___) => Container(
                            width: 96,
                            height: 96,
                            color: MtaaFixColors.canvas,
                            child: const Icon(Icons.broken_image_outlined),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
            if (report.statusHistory.isNotEmpty) ...[
              const SizedBox(height: 20),
              Text('Status timeline', style: text.titleMedium),
              const SizedBox(height: 8),
              ...report.statusHistory.map(
                (entry) => _TimelineTile(entry: entry),
              ),
            ],
            const SizedBox(height: 20),
            Text('Comments (${report.comments.length})',
                style: text.titleMedium),
            const SizedBox(height: 8),
            if (report.comments.isEmpty)
              Text(
                'No comments yet.',
                style: text.bodySmall
                    ?.copyWith(color: MtaaFixColors.mutedText),
              )
            else
              ...report.comments.map((c) => _CommentTile(comment: c)),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _comment,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: const InputDecoration(
                      hintText: 'Add a comment…',
                    ),
                    minLines: 1,
                    maxLines: 3,
                  ),
                ),
                const SizedBox(width: 8),
                _postingComment
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : IconButton.filled(
                        tooltip: 'Post comment',
                        icon: const Icon(Icons.send_rounded),
                        onPressed: _postComment,
                      ),
              ],
            ),
            const SizedBox(height: 24),
            if (_acting) const LinearProgressIndicator(),
            if (canCancel) ...[
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _cancelReport,
                  icon: const Icon(Icons.cancel_outlined),
                  label: const Text('Cancel report'),
                ),
              ),
              const SizedBox(height: 8),
            ],
            if (canConfirmOrDispute) ...[
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: _confirmResolved,
                  icon: const Icon(Icons.check_circle_outline_rounded),
                  label: const Text('Confirm resolution'),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _disputeResolved,
                  icon: const Icon(Icons.gavel_rounded),
                  label: const Text('Dispute resolution'),
                ),
              ),
            ],
            if (canResubmit) ...[
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: _resubmit,
                  icon: const Icon(Icons.replay_rounded),
                  label: const Text('Resubmit report'),
                ),
              ),
            ],
            const SizedBox(height: 32),
          ],
          ),
        ),
      ),
    );
  }
}

class _LocationCard extends StatelessWidget {
  const _LocationCard({required this.location});

  final ReportLocation location;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final point = LatLng(location.latitude, location.longitude);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            height: 160,
            width: double.infinity,
            child: FlutterMap(
              options: MapOptions(
                initialCenter: point,
                initialZoom: 15,
                interactionOptions: const InteractionOptions(
                  flags: InteractiveFlag.none,
                ),
              ),
              children: [
                TileLayer(
                  urlTemplate:
                      'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                  userAgentPackageName: 'com.mtaafix.mobile',
                ),
                MarkerLayer(
                  markers: [
                    Marker(
                      point: point,
                      width: 40,
                      height: 40,
                      child: const Icon(
                        Icons.location_pin,
                        size: 40,
                        color: MtaaFixColors.errorRed,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Text(
              'Lat ${location.latitude.toStringAsFixed(5)}, '
              'Lng ${location.longitude.toStringAsFixed(5)}',
              style: text.labelMedium?.copyWith(color: MtaaFixColors.mutedText),
            ),
          ),
        ],
      ),
    );
  }
}

class _TimelineTile extends StatelessWidget {
  const _TimelineTile({required this.entry});

  final StatusHistoryEntry entry;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.circle_rounded,
            size: 10,
            color: statusColor(
                entry.toStatus, Theme.of(context).colorScheme),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(statusLabel(entry.toStatus), style: text.labelLarge),
                if (entry.timestamp != null)
                  Text(
                    formatDateTime(entry.timestamp),
                    style: text.labelSmall
                        ?.copyWith(color: MtaaFixColors.mutedText),
                  ),
                if (entry.comment != null && entry.comment!.isNotEmpty)
                  Text(entry.comment!, style: text.bodySmall),
                if (entry.reasonCode != null && entry.reasonCode!.isNotEmpty)
                  Text(
                    'Reason: ${entry.reasonCode}',
                    style: text.bodySmall
                        ?.copyWith(color: MtaaFixColors.mutedText),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CommentTile extends StatelessWidget {
  const _CommentTile({required this.comment});

  final ReportComment comment;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(comment.content, style: text.bodyMedium),
            if (comment.timestamp != null) ...[
              const SizedBox(height: 6),
              Text(
                formatDateTime(comment.timestamp),
                style:
                    text.labelSmall?.copyWith(color: MtaaFixColors.mutedText),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _DisputeDialog extends StatefulWidget {
  const _DisputeDialog({required this.controller});

  final TextEditingController controller;

  @override
  State<_DisputeDialog> createState() => _DisputeDialogState();
}

class _DisputeDialogState extends State<_DisputeDialog> {
  String? _validation;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Dispute resolution'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: widget.controller,
            maxLines: 3,
            decoration: const InputDecoration(
              labelText: 'Why is this not resolved?',
              hintText: 'Describe what is still wrong',
            ),
          ),
          if (_validation != null)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(
                _validation!,
                style: TextStyle(
                  fontSize: 12,
                  color: Theme.of(context).colorScheme.error,
                ),
              ),
            ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            if (widget.controller.text.trim().isEmpty) {
              setState(() => _validation = 'A dispute reason is required.');
              return;
            }
            Navigator.pop(context, widget.controller.text.trim());
          },
          child: const Text('Submit dispute'),
        ),
      ],
    );
  }
}
