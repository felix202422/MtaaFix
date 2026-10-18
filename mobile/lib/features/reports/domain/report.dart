enum ReportStatus {
  submitted('SUBMITTED'),
  underReview('UNDER_REVIEW'),
  needsInformation('NEEDS_INFORMATION'),
  verified('VERIFIED'),
  assigned('ASSIGNED'),
  inProgress('IN_PROGRESS'),
  resolutionReview('RESOLUTION_REVIEW'),
  resolved('RESOLVED'),
  rejected('REJECTED'),
  duplicate('DUPLICATE'),
  reopened('REOPENED'),
  closed('CLOSED');

  const ReportStatus(this.wire);

  final String wire;

  static ReportStatus parse(String? value) =>
      ReportStatus.values.firstWhere((s) => s.wire == value,
          orElse: () => ReportStatus.submitted);
}

class ReportLocation {
  const ReportLocation({required this.latitude, required this.longitude});

  final double latitude;
  final double longitude;

  factory ReportLocation.fromJson(Map<String, dynamic> json) => ReportLocation(
        latitude: (json['latitude'] as num?)?.toDouble() ?? 0,
        longitude: (json['longitude'] as num?)?.toDouble() ?? 0,
      );
}

class StatusHistoryEntry {
  const StatusHistoryEntry({
    required this.fromStatus,
    required this.toStatus,
    required this.timestamp,
    this.comment,
    this.reasonCode,
    this.userId,
  });

  final ReportStatus fromStatus;
  final ReportStatus toStatus;
  final String? timestamp;
  final String? comment;
  final String? reasonCode;
  final String? userId;

  factory StatusHistoryEntry.fromJson(Map<String, dynamic> json) =>
      StatusHistoryEntry(
        fromStatus: ReportStatus.parse(json['fromStatus'] as String?),
        toStatus: ReportStatus.parse(json['toStatus'] as String?),
        timestamp: json['timestamp'] as String?,
        comment: json['comment'] as String?,
        reasonCode: json['reasonCode'] as String?,
        userId: json['userId'] as String?,
      );
}

class ReportMediaItem {
  const ReportMediaItem({
    required this.id,
    required this.fileName,
    required this.contentType,
    required this.url,
    this.originalName,
    this.sizeBytes,
    this.mediaType,
  });

  final String id;
  final String fileName;
  final String? originalName;
  final String? contentType;
  final int? sizeBytes;
  final String url;
  final String? mediaType;

  factory ReportMediaItem.fromJson(Map<String, dynamic> json) =>
      ReportMediaItem(
        id: json['id'] as String? ?? '',
        fileName: json['fileName'] as String? ?? '',
        originalName: json['originalName'] as String?,
        contentType: json['contentType'] as String?,
        sizeBytes: (json['sizeBytes'] as num?)?.toInt(),
        url: json['url'] as String? ?? '',
        mediaType: json['mediaType'] as String?,
      );
}

class ReportComment {
  const ReportComment({
    required this.id,
    required this.content,
    this.userId,
    this.timestamp,
  });

  final String id;
  final String content;
  final String? userId;
  final String? timestamp;

  factory ReportComment.fromJson(Map<String, dynamic> json) => ReportComment(
        id: json['id'] as String? ?? '',
        content: json['content'] as String? ?? '',
        userId: json['userId'] as String?,
        timestamp: json['timestamp'] as String?,
      );
}

class Report {
  const Report({
    required this.id,
    required this.code,
    required this.title,
    required this.status,
    this.description,
    this.location,
    this.reportedAt,
    this.categoryId,
    this.score,
    this.statusHistory = const [],
    this.media = const [],
    this.comments = const [],
  });

  final String id;
  final String code;
  final String title;
  final String? description;
  final ReportStatus status;
  final ReportLocation? location;
  final String? reportedAt;
  final String? categoryId;
  final int? score;
  final List<StatusHistoryEntry> statusHistory;
  final List<ReportMediaItem> media;
  final List<ReportComment> comments;

  factory Report.fromJson(Map<String, dynamic> json) {
    final location = json['location'];
    return Report(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      title: json['title'] as String? ?? '',
      description: json['description'] as String?,
      status: ReportStatus.parse(json['status'] as String?),
      location: location is Map<String, dynamic>
          ? ReportLocation.fromJson(location)
          : null,
      reportedAt: json['reportedAt'] as String?,
      categoryId: json['categoryId'] as String?,
      score: (json['score'] as num?)?.toInt(),
      statusHistory: _list(json['statusHistory'], StatusHistoryEntry.fromJson),
      media: _list(json['media'], ReportMediaItem.fromJson),
      comments: _list(json['comments'], ReportComment.fromJson),
    );
  }
}

class Category {
  const Category({
    required this.id,
    required this.name,
    this.slug,
    this.active,
    this.displayOrder,
  });

  final String id;
  final String name;
  final String? slug;
  final bool? active;
  final int? displayOrder;

  factory Category.fromJson(Map<String, dynamic> json) => Category(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        slug: json['slug'] as String?,
        active: json['active'] as bool?,
        displayOrder: (json['displayOrder'] as num?)?.toInt(),
      );
}

class PublicReport {
  const PublicReport({
    required this.code,
    required this.title,
    required this.status,
    this.description,
    this.categoryId,
    this.categoryName,
    this.reportedAt,
    this.latitude,
    this.longitude,
  });

  final String code;
  final String title;
  final String? description;
  final ReportStatus status;
  final String? categoryId;
  final String? categoryName;
  final String? reportedAt;
  final double? latitude;
  final double? longitude;

  factory PublicReport.fromJson(Map<String, dynamic> json) => PublicReport(
        code: json['code'] as String? ?? '',
        title: json['title'] as String? ?? '',
        description: json['description'] as String?,
        status: ReportStatus.parse(json['status'] as String?),
        categoryId: json['categoryId'] as String?,
        categoryName: json['categoryName'] as String?,
        reportedAt: json['reportedAt'] as String?,
        latitude: (json['latitude'] as num?)?.toDouble(),
        longitude: (json['longitude'] as num?)?.toDouble(),
      );
}

class PublicTimelineEntry {
  const PublicTimelineEntry({required this.status, this.timestamp});

  final ReportStatus status;
  final String? timestamp;

  factory PublicTimelineEntry.fromJson(Map<String, dynamic> json) =>
      PublicTimelineEntry(
        status: ReportStatus.parse(json['status'] as String?),
        timestamp: json['timestamp'] as String?,
      );
}

class PublicReportDetail {
  const PublicReportDetail({
    required this.code,
    required this.title,
    required this.status,
    this.description,
    this.categoryName,
    this.reportedAt,
    this.latitude,
    this.longitude,
    this.timeline = const [],
  });

  final String code;
  final String title;
  final String? description;
  final ReportStatus status;
  final String? categoryName;
  final String? reportedAt;
  final double? latitude;
  final double? longitude;
  final List<PublicTimelineEntry> timeline;

  factory PublicReportDetail.fromJson(Map<String, dynamic> json) =>
      PublicReportDetail(
        code: json['code'] as String? ?? '',
        title: json['title'] as String? ?? '',
        description: json['description'] as String?,
        status: ReportStatus.parse(json['status'] as String?),
        categoryName: json['categoryName'] as String?,
        reportedAt: json['reportedAt'] as String?,
        latitude: (json['latitude'] as num?)?.toDouble(),
        longitude: (json['longitude'] as num?)?.toDouble(),
        timeline: _list(json['timeline'], PublicTimelineEntry.fromJson),
      );
}

List<T> _list<T>(Object? raw, T Function(Map<String, dynamic>) fromJson) {
  if (raw is! List) return const [];
  return raw
      .whereType<Map<String, dynamic>>()
      .map(fromJson)
      .toList(growable: false);
}
