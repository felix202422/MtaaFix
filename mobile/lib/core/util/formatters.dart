import 'package:intl/intl.dart';

final DateFormat _day = DateFormat('d MMM yyyy');
final DateFormat _dayTime = DateFormat('d MMM yyyy, HH:mm');

/// Formats an ISO-8601 timestamp for display; returns '—' when absent.
String formatDate(String? iso) {
  final date = DateTime.tryParse(iso ?? '');
  if (date == null) return '—';
  return _day.format(date.toLocal());
}

String formatDateTime(String? iso) {
  final date = DateTime.tryParse(iso ?? '');
  if (date == null) return '—';
  return _dayTime.format(date.toLocal());
}
