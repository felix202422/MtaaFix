/// State of a locally queued (not-yet-filed) report submission.
enum PendingSubmissionState {
  /// Waiting for connectivity; the report does not exist on the server.
  pending,

  /// The report was created on the server; photos are still attaching.
  attachingPhotos,

  /// Rejected by the server or blocked; needs user attention.
  failed,
}

/// A report the user chose to submit while offline, kept in the outbox until
/// it is filed. The stable [idempotencyKey] makes retries exactly-once
/// against `POST /reports` (backend V6).
class PendingSubmission {
  const PendingSubmission({
    required this.localId,
    required this.idempotencyKey,
    required this.categoryId,
    required this.title,
    required this.latitude,
    required this.longitude,
    required this.createdAt,
    required this.updatedAt,
    this.categoryName,
    this.description,
    this.photoPaths = const [],
    this.uploadedPhotoCount = 0,
    this.photoFailures = 0,
    this.state = PendingSubmissionState.pending,
    this.lastError,
    this.serverId,
    this.serverCode,
  });

  final String localId;
  final String idempotencyKey;
  final String categoryId;
  final String? categoryName;
  final String title;
  final String? description;
  final double latitude;
  final double longitude;
  final List<String> photoPaths;
  final int uploadedPhotoCount;
  final int photoFailures;
  final PendingSubmissionState state;
  final String? lastError;
  final String? serverId;
  final String? serverCode;
  final DateTime createdAt;
  final DateTime updatedAt;

  static const int schemaVersion = 1;

  bool get isFiled => serverId != null;

  PendingSubmission copyWith({
    List<String>? photoPaths,
    int? uploadedPhotoCount,
    int? photoFailures,
    PendingSubmissionState? state,
    String? lastError,
    bool clearError = false,
    String? serverId,
    String? serverCode,
    DateTime? updatedAt,
  }) {
    return PendingSubmission(
      localId: localId,
      idempotencyKey: idempotencyKey,
      categoryId: categoryId,
      categoryName: categoryName,
      title: title,
      description: description,
      latitude: latitude,
      longitude: longitude,
      photoPaths: photoPaths ?? this.photoPaths,
      uploadedPhotoCount: uploadedPhotoCount ?? this.uploadedPhotoCount,
      photoFailures: photoFailures ?? this.photoFailures,
      state: state ?? this.state,
      lastError: clearError ? null : (lastError ?? this.lastError),
      serverId: serverId ?? this.serverId,
      serverCode: serverCode ?? this.serverCode,
      createdAt: createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  Map<String, dynamic> toJson() => {
        'schema': schemaVersion,
        'localId': localId,
        'idempotencyKey': idempotencyKey,
        'categoryId': categoryId,
        'categoryName': categoryName,
        'title': title,
        'description': description,
        'latitude': latitude,
        'longitude': longitude,
        'photoPaths': photoPaths,
        'uploadedPhotoCount': uploadedPhotoCount,
        'photoFailures': photoFailures,
        'state': state.name,
        'lastError': lastError,
        'serverId': serverId,
        'serverCode': serverCode,
        'createdAt': createdAt.toIso8601String(),
        'updatedAt': updatedAt.toIso8601String(),
      };

  static PendingSubmission? fromJson(Map<String, dynamic> json) {
    if (json['schema'] != schemaVersion) return null;
    final localId = json['localId'];
    final key = json['idempotencyKey'];
    final categoryId = json['categoryId'];
    final title = json['title'];
    final latitude = json['latitude'];
    final longitude = json['longitude'];
    if (localId is! String ||
        key is! String ||
        categoryId is! String ||
        title is! String ||
        latitude is! num ||
        longitude is! num) {
      return null;
    }
    return PendingSubmission(
      localId: localId,
      idempotencyKey: key,
      categoryId: categoryId,
      categoryName: json['categoryName'] as String?,
      title: title,
      description: json['description'] as String?,
      latitude: latitude.toDouble(),
      longitude: longitude.toDouble(),
      photoPaths: (json['photoPaths'] as List?)
              ?.whereType<String>()
              .toList(growable: true) ??
          const [],
      uploadedPhotoCount: (json['uploadedPhotoCount'] as num?)?.toInt() ?? 0,
      photoFailures: (json['photoFailures'] as num?)?.toInt() ?? 0,
      state: PendingSubmissionState.values.firstWhere(
        (s) => s.name == json['state'],
        orElse: () => PendingSubmissionState.pending,
      ),
      lastError: json['lastError'] as String?,
      serverId: json['serverId'] as String?,
      serverCode: json['serverCode'] as String?,
      createdAt:
          DateTime.tryParse(json['createdAt']?.toString() ?? '') ??
              DateTime.now(),
      updatedAt:
          DateTime.tryParse(json['updatedAt']?.toString() ?? '') ??
              DateTime.now(),
    );
  }
}
