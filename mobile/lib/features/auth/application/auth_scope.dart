import 'package:flutter/widgets.dart';

import 'package:mtaafix_mobile/features/auth/application/auth_controller.dart';

/// Exposes the [AuthController] to the widget tree.
///
/// Placed above the [MaterialApp] so every routed screen can resolve it.
class AuthScope extends InheritedNotifier<AuthController> {
  const AuthScope({
    super.key,
    required AuthController controller,
    required super.child,
  }) : super(notifier: controller);

  /// Rebuilds the calling widget whenever auth state changes.
  static AuthController of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AuthScope>();
    assert(scope != null, 'AuthScope is missing above this widget');
    return scope!.notifier!;
  }

  /// One-shot read for event handlers (no rebuild dependency).
  static AuthController read(BuildContext context) {
    final scope = context.getInheritedWidgetOfExactType<AuthScope>();
    assert(scope != null, 'AuthScope is missing above this widget');
    return scope!.notifier!;
  }
}
