/// Local draft of a not-yet-submitted report (Phase 14). A draft is never
/// shown in "My Reports" and never carries an MTF code.
class ReportDraft {
  const ReportDraft({
    required this.categoryId,
    required this.title,
    required this.description,
    required this.latitude,
    required this.longitude,
    required this.photoPaths,
    required this.updatedAt,
    this.categoryName,
  });

  final String categoryId;
  final String? categoryName;
  final String title;
  final String? description;
  final double latitude;
  final double longitude;
  final List<String> photoPaths;
  final DateTime updatedAt;

  static const int schemaVersion = 1;

  Map<String, dynamic> toJson() => {
        'schema': schemaVersion,
        'categoryId': categoryId,
        'categoryName': categoryName,
        'title': title,
        'description': description,
        'latitude': latitude,
        'longitude': longitude,
        'photoPaths': photoPaths,
        'updatedAt': updatedAt.toIso8601String(),
      };

  static ReportDraft? tryFromJson(Map<String, dynamic> json) {
    if (json['schema'] != schemaVersion) return null;
    final categoryId = json['categoryId'];
    final title = json['title'];
    final latitude = json['latitude'];
    final longitude = json['longitude'];
    // An empty categoryId is valid: autosaves can happen before the user
    // picks a category, and losing the typed text would be worse.
    if (categoryId is! String ||
        title is! String ||
        latitude is! num ||
        longitude is! num) {
      return null;
    }
    return ReportDraft(
      categoryId: categoryId,
      categoryName: json['categoryName'] as String?,
      title: title,
      description: json['description'] as String?,
      latitude: latitude.toDouble(),
      longitude: longitude.toDouble(),
      photoPaths: (json['photoPaths'] as List?)
              ?.whereType<String>()
              .toList(growable: false) ??
          const [],
      updatedAt: DateTime.tryParse(json['updatedAt']?.toString() ?? '') ??
          DateTime.now(),
    );
  }
}
