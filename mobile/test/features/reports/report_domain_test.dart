import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mtaafix_mobile/app/status_theme.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

void main() {
  test('ReportStatus.parse maps every wire value', () {
    for (final status in ReportStatus.values) {
      expect(ReportStatus.parse(status.wire), status);
    }
  });

  test('ReportStatus.parse falls back to SUBMITTED for unknown values', () {
    expect(ReportStatus.parse('NOT_A_STATUS'), ReportStatus.submitted);
    expect(ReportStatus.parse(null), ReportStatus.submitted);
    expect(ReportStatus.parse('closed'), ReportStatus.submitted,
        reason: 'comparison is case-sensitive on purpose');
    expect(ReportStatus.parse('CLOSED'), ReportStatus.closed);
  });

  test('every status has a human label that is not its wire value', () {
    for (final status in ReportStatus.values) {
      final label = statusLabel(status);
      expect(label, isNotEmpty);
      expect(label, isNot(status.wire));
    }
  });

  test('status labels match the design system wording', () {
    expect(statusLabel(ReportStatus.submitted), 'Submitted');
    expect(statusLabel(ReportStatus.underReview), 'Under review');
    expect(statusLabel(ReportStatus.needsInformation), 'Needs information');
    expect(statusLabel(ReportStatus.verified), 'Verified');
    expect(statusLabel(ReportStatus.assigned), 'Assigned');
    expect(statusLabel(ReportStatus.inProgress), 'In progress');
    expect(statusLabel(ReportStatus.resolutionReview), 'Resolution review');
    expect(statusLabel(ReportStatus.resolved), 'Resolved');
    expect(statusLabel(ReportStatus.rejected), 'Rejected');
    expect(statusLabel(ReportStatus.duplicate), 'Duplicate');
    expect(statusLabel(ReportStatus.reopened), 'Reopened');
    expect(statusLabel(ReportStatus.closed), 'Closed');
  });

  test('statusColor uses the design-system tint mapping', () {
    const scheme = ColorScheme.light();

    expect(statusColor(ReportStatus.submitted, scheme),
        MtaaFixColors.mutedText);
    expect(statusColor(ReportStatus.underReview, scheme),
        MtaaFixColors.flagBlue);
    expect(statusColor(ReportStatus.needsInformation, scheme),
        MtaaFixColors.warningAmber);
    expect(statusColor(ReportStatus.resolved, scheme),
        MtaaFixColors.primaryGreen);
    expect(statusColor(ReportStatus.rejected, scheme),
        MtaaFixColors.errorRed);
    expect(statusColor(ReportStatus.closed, scheme),
        MtaaFixColors.primaryGreen);
  });

  test('Report.fromJson parses the detail payload', () {
    final report = Report.fromJson({
      'id': 'r1',
      'code': 'MTF-0042',
      'title': 'Blocked drain',
      'description': 'Water pooling at the junction',
      'status': 'IN_PROGRESS',
      'location': {'latitude': -1.30, 'longitude': 36.83},
      'reportedAt': '2026-10-01T08:00:00Z',
      'categoryId': 'cat-2',
      'score': 4,
      'statusHistory': [
        {
          'fromStatus': 'SUBMITTED',
          'toStatus': 'UNDER_REVIEW',
          'timestamp': '2026-10-01T09:00:00Z',
          'comment': 'Looking into it',
        },
        {
          'fromStatus': 'UNDER_REVIEW',
          'toStatus': 'IN_PROGRESS',
          'timestamp': '2026-10-01T10:00:00Z',
        },
      ],
      'media': [
        {
          'id': 'm1',
          'fileName': 'a.jpg',
          'contentType': 'image/jpeg',
          'url': '/api/v1/media/m1/content',
          'sizeBytes': 1024,
        },
      ],
      'comments': [
        {'id': 'c1', 'content': 'Any update?', 'userId': 'u1'},
      ],
    });

    expect(report.id, 'r1');
    expect(report.status, ReportStatus.inProgress);
    expect(report.location?.latitude, -1.30);
    expect(report.reportedAt, '2026-10-01T08:00:00Z');
    expect(report.categoryId, 'cat-2');
    expect(report.score, 4);
    expect(report.statusHistory, hasLength(2));
    expect(report.statusHistory.first.toStatus, ReportStatus.underReview);
    expect(report.statusHistory.first.comment, 'Looking into it');
    expect(report.media.single.url, '/api/v1/media/m1/content');
    expect(report.media.single.sizeBytes, 1024);
    expect(report.comments.single.content, 'Any update?');
  });

  test('Report.fromJson tolerates absent optional blocks', () {
    final report = Report.fromJson({
      'id': 'r2',
      'code': 'MTF-0043',
      'title': 'Flickering light',
      'status': 'SUBMITTED',
    });

    expect(report.description, isNull);
    expect(report.location, isNull);
    expect(report.statusHistory, isEmpty);
    expect(report.media, isEmpty);
    expect(report.comments, isEmpty);
  });

  test('PublicReportDetail.fromJson parses the timeline', () {
    final detail = PublicReportDetail.fromJson({
      'code': 'MTF-0100',
      'title': 'Damaged bench',
      'status': 'RESOLVED',
      'categoryName': 'Parks',
      'reportedAt': '2026-09-30T12:00:00Z',
      'latitude': -1.28,
      'longitude': 36.81,
      'timeline': [
        {'status': 'SUBMITTED', 'timestamp': '2026-09-30T12:00:00Z'},
        {'status': 'RESOLVED', 'timestamp': '2026-10-01T12:00:00Z'},
      ],
    });

    expect(detail.code, 'MTF-0100');
    expect(detail.categoryName, 'Parks');
    expect(detail.latitude, -1.28);
    expect(detail.timeline, hasLength(2));
    expect(detail.timeline.first.status, ReportStatus.submitted);
    expect(detail.timeline.last.status, ReportStatus.resolved);
  });

  test('ReportLocation.fromJson reads numeric coordinates', () {
    final location = ReportLocation.fromJson({'latitude': 1, 'longitude': 2.5});

    expect(location.latitude, 1.0);
    expect(location.longitude, 2.5);
  });
}
