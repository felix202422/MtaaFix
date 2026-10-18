/// Tolerant parser for the backend's paginated payloads: the flat
/// `PagedResponse` shape (`{totalElements, totalPages, page, size, content}`),
/// Spring's default `Page` shape (`number` instead of `page`) and the V2
/// nested shape (`{page: {...}, content}`) all parse to [Paged].
class Paged<T> {
  const Paged({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.page,
    required this.size,
  });

  final List<T> content;
  final int totalElements;
  final int totalPages;
  final int page;
  final int size;

  bool get hasMore => page + 1 < totalPages;
  bool get isEmpty => content.isEmpty;

  factory Paged.parse(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic> json) fromJson,
  ) {
    final rawContent = json['content'];
    final content = <T>[];
    if (rawContent is List) {
      for (final item in rawContent) {
        if (item is Map<String, dynamic>) {
          content.add(fromJson(item));
        }
      }
    }
    final pageBlock = json['page'];
    final nested = pageBlock is Map<String, dynamic> ? pageBlock : const {};
    final pageValue = json['page'] is int
        ? json['page']
        : (json['number'] ?? nested['number'] ?? 0);
    final totalPagesValue =
        json['totalPages'] ?? nested['totalPages'] ?? (content.isEmpty ? 0 : 1);
    final totalElementsValue = json['totalElements'] ??
        nested['totalElements'] ??
        content.length;
    final sizeValue = json['size'] ?? nested['size'] ?? content.length;
    return Paged<T>(
      content: content,
      totalElements: (totalElementsValue as num).toInt(),
      totalPages: (totalPagesValue as num).toInt(),
      page: (pageValue as num).toInt(),
      size: (sizeValue as num).toInt(),
    );
  }
}
