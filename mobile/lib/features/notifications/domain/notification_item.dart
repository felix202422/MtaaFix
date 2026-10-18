class NotificationItem {
  const NotificationItem({
    required this.id,
    required this.title,
    required this.body,
    this.isRead = false,
    this.timestamp,
    this.reportId,
    this.type,
  });

  final String id;
  final String title;
  final String body;
  final bool isRead;
  final String? timestamp;
  final String? reportId;
  final String? type;

  factory NotificationItem.fromJson(Map<String, dynamic> json) =>
      NotificationItem(
        id: json['id'] as String? ?? '',
        title: json['title'] as String? ?? '',
        body: json['body'] as String? ?? '',
        isRead: json['isRead'] as bool? ?? false,
        timestamp: json['timestamp'] as String?,
        reportId: json['reportId'] as String?,
        type: json['type'] as String?,
      );
}
