import 'package:flutter/widgets.dart';

import 'package:mtaafix_mobile/features/notifications/application/unread_count_controller.dart';

/// Exposes the [UnreadCountController] (unread badge count) to the widget
/// tree. Placed above the [MaterialApp]; lookups are nullable so screens and
/// tests that render without the scope keep working.
class UnreadCountScope extends InheritedNotifier<UnreadCountController> {
  const UnreadCountScope({
    super.key,
    required UnreadCountController controller,
    required super.child,
  }) : super(notifier: controller);

  /// Rebuilds the calling widget whenever the unread count changes.
  static UnreadCountController? maybeOf(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<UnreadCountScope>()
      ?.notifier;

  /// One-shot read for event handlers (no rebuild dependency).
  static UnreadCountController? read(BuildContext context) => context
      .getInheritedWidgetOfExactType<UnreadCountScope>()
      ?.notifier;
}
