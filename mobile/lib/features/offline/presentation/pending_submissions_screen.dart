import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/util/formatters.dart';
import 'package:mtaafix_mobile/core/widgets/feedback_view.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_scope.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';

/// Queue of locally saved reports that are not yet filed on the server
/// (Phase 14). Entries here are honest about their state — nothing is shown
/// as submitted until the server accepted it.
class PendingSubmissionsScreen extends StatelessWidget {
  const PendingSubmissionsScreen({super.key});

  Future<void> _discard(BuildContext context,
      PendingSubmission item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Discard this draft?'),
        content: Text(
          item.isFiled
              ? 'The report ${item.serverCode ?? ''} was already filed — '
                  'only the local copy (including unattached photos) is '
                  'removed.'
              : 'This report has not been submitted and will be lost.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Discard'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;
    await OfflineScope.read(context)?.discard(item.localId);
  }

  Future<void> _retryNow(BuildContext context) async {
    final offline = OfflineScope.read(context);
    if (offline == null) return;
    await offline.flush();
    if (!context.mounted) return;
    final codes = offline.consumeJustSubmitted();
    if (codes.isEmpty) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(codes.length == 1
              ? 'Report ${codes.first} submitted.'
              : '${codes.length} reports submitted.'),
        ),
      );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final offline = OfflineScope.maybeOf(context);
    final items = offline?.items ?? const <PendingSubmission>[];
    final online = offline?.isOnline ?? true;
    final flushing = offline?.isFlushing ?? false;

    return Scaffold(
      appBar: AppBar(title: const Text('Pending reports')),
      body: items.isEmpty
          ? const FeedbackView(
              icon: Icons.inbox_rounded,
              title: 'Nothing pending',
              message: 'Reports saved for offline sending will appear here.',
            )
          : ListView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 96),
              children: [
                if (!online)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Row(
                      children: [
                        const Icon(Icons.cloud_off_rounded,
                            size: 16, color: MtaaFixColors.warningAmber),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            'You are offline — reports are sent automatically '
                            'once you reconnect.',
                            style: text.bodySmall
                                ?.copyWith(color: MtaaFixColors.warningAmber),
                          ),
                        ),
                      ],
                    ),
                  ),
                for (final item in items)
                  Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              _StateBadge(state: item.state),
                              const Spacer(),
                              if (!item.isFiled && item.state !=
                                  PendingSubmissionState.failed)
                                Text(
                                  'Not yet submitted',
                                  style: text.labelSmall?.copyWith(
                                      color: MtaaFixColors.warningAmber,
                                      fontWeight: FontWeight.w700),
                                ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(item.title,
                              style: text.titleMedium,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis),
                          const SizedBox(height: 4),
                          Text(
                            [
                              if (item.categoryName != null)
                                item.categoryName!,
                              '${item.photoPaths.length} photo(s) pending',
                              formatDate(item.createdAt.toIso8601String()),
                            ].join(' · '),
                            style: text.bodySmall
                                ?.copyWith(color: MtaaFixColors.mutedText),
                          ),
                          if (item.serverCode != null) ...[
                            const SizedBox(height: 4),
                            Text(
                              'Filed as ${item.serverCode}',
                              style: text.labelMedium?.copyWith(
                                  color: MtaaFixColors.primaryGreen,
                                  fontWeight: FontWeight.w700),
                            ),
                          ],
                          if (item.lastError != null) ...[
                            const SizedBox(height: 6),
                            Text(
                              item.lastError!,
                              style: text.bodySmall
                                  ?.copyWith(color: MtaaFixColors.errorRed),
                            ),
                          ],
                          const SizedBox(height: 8),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              if (!item.isFiled &&
                                  item.state ==
                                      PendingSubmissionState.failed) ...[
                                TextButton.icon(
                                  onPressed: () =>
                                      Navigator.pushNamed(context, '/reports/new',
                                          arguments: item),
                                  icon: const Icon(Icons.edit_rounded, size: 18),
                                  label: const Text('Edit'),
                                ),
                                const SizedBox(width: 4),
                              ],
                              TextButton.icon(
                                onPressed: () => _discard(context, item),
                                icon: const Icon(Icons.delete_outline_rounded,
                                    size: 18),
                                label: const Text('Discard'),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
      floatingActionButton: items.isEmpty
          ? null
          : FloatingActionButton.extended(
              onPressed: (online && !flushing) ? () => _retryNow(context) : null,
              icon: flushing
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.send_rounded),
              label: Text(online
                  ? (flushing ? 'Sending…' : 'Try sending now')
                  : 'Waiting for connection'),
            ),
    );
  }
}

class _StateBadge extends StatelessWidget {
  const _StateBadge({required this.state});

  final PendingSubmissionState state;

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (state) {
      PendingSubmissionState.pending => (
          'Waiting',
          MtaaFixColors.warningAmber,
        ),
      PendingSubmissionState.attachingPhotos => (
          'Sending photos',
          MtaaFixColors.flagBlue,
        ),
      PendingSubmissionState.failed => (
          'Needs attention',
          MtaaFixColors.errorRed,
        ),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: Theme.of(context)
            .textTheme
            .labelSmall
            ?.copyWith(color: color, fontWeight: FontWeight.w700),
      ),
    );
  }
}
