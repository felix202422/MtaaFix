import 'package:flutter/widgets.dart';

import 'package:mtaafix_mobile/features/offline/application/offline_controller.dart';

/// Exposes the [OfflineController] (drafts, pending queue, connectivity)
/// above the [MaterialApp]. Lookups are nullable so screens and tests that
/// render without the scope keep working.
class OfflineScope extends InheritedNotifier<OfflineController> {
  const OfflineScope({
    super.key,
    required OfflineController controller,
    required super.child,
  }) : super(notifier: controller);

  /// Rebuilds the calling widget whenever offline state changes.
  static OfflineController? maybeOf(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<OfflineScope>()
      ?.notifier;

  /// One-shot read for event handlers (no rebuild dependency).
  static OfflineController? read(BuildContext context) => context
      .getInheritedWidgetOfExactType<OfflineScope>()
      ?.notifier;
}
