import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// Mirrors the web design-system status mapping (DESIGN_SYSTEM.md § status).
enum StatusTint { neutral, info, warning, success, error }

class StatusMeta {
  const StatusMeta(this.label, this.tint);

  final String label;
  final StatusTint tint;
}

const Map<ReportStatus, StatusMeta> statusMeta = {
  ReportStatus.submitted: StatusMeta('Submitted', StatusTint.neutral),
  ReportStatus.underReview: StatusMeta('Under review', StatusTint.info),
  ReportStatus.needsInformation:
      StatusMeta('Needs information', StatusTint.warning),
  ReportStatus.verified: StatusMeta('Verified', StatusTint.info),
  ReportStatus.assigned: StatusMeta('Assigned', StatusTint.info),
  ReportStatus.inProgress: StatusMeta('In progress', StatusTint.info),
  ReportStatus.resolutionReview:
      StatusMeta('Resolution review', StatusTint.info),
  ReportStatus.resolved: StatusMeta('Resolved', StatusTint.success),
  ReportStatus.rejected: StatusMeta('Rejected', StatusTint.error),
  ReportStatus.duplicate: StatusMeta('Duplicate', StatusTint.neutral),
  ReportStatus.reopened: StatusMeta('Reopened', StatusTint.warning),
  ReportStatus.closed: StatusMeta('Closed', StatusTint.success),
};

String statusLabel(ReportStatus status) =>
    statusMeta[status]?.label ?? status.wire;

Color statusColor(ReportStatus status, ColorScheme scheme) =>
    switch (statusMeta[status]?.tint ?? StatusTint.neutral) {
      StatusTint.neutral => MtaaFixColors.mutedText,
      StatusTint.info => MtaaFixColors.flagBlue,
      StatusTint.warning => MtaaFixColors.warningAmber,
      StatusTint.success => MtaaFixColors.primaryGreen,
      StatusTint.error => MtaaFixColors.errorRed,
    };

class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.status, this.dense = false});

  final ReportStatus status;
  final bool dense;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = statusColor(status, scheme);
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: dense ? 8 : 10,
        vertical: dense ? 3 : 4,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.45)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 6),
          Text(
            statusLabel(status),
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: color,
                  fontWeight: FontWeight.w600,
                ),
          ),
        ],
      ),
    );
  }
}
