import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';

/// Abstraction over the platform connectivity channel so tests can drive
/// offline → online transitions without platform plugins.
abstract class ConnectivitySource {
  Future<bool> isOnline();

  /// Emits `true` when connectivity returns and `false` when it drops.
  Stream<bool> get onOnlineChange;
}

/// Production source backed by `connectivity_plus`. Failures degrade to
/// "assume online" — connectivity is only a flush hint; every submission
/// still trusts real request outcomes.
class DefaultConnectivitySource implements ConnectivitySource {
  DefaultConnectivitySource([Connectivity? connectivity])
      : _connectivity = connectivity ?? Connectivity();

  final Connectivity _connectivity;

  @override
  Future<bool> isOnline() async {
    try {
      final results = await _connectivity.checkConnectivity();
      return !results.contains(ConnectivityResult.none);
    } on Object {
      return true;
    }
  }

  @override
  Stream<bool> get onOnlineChange async* {
    try {
      await for (final results in _connectivity.onConnectivityChanged) {
        yield !results.contains(ConnectivityResult.none);
      }
    } on Object {
      // Channel unavailable — no hints; manual/refresh triggers still work.
    }
  }
}

/// Tracks an online/offline flag and notifies listeners on transitions.
class ConnectivityService extends ChangeNotifier {
  ConnectivityService({ConnectivitySource? source})
      : _source = source ?? DefaultConnectivitySource();

  final ConnectivitySource _source;
  StreamSubscription<bool>? _subscription;
  bool _online = true;

  bool get isOnline => _online;

  Future<void> start() async {
    _online = await _source.isOnline();
    notifyListeners();
    await _subscription?.cancel();
    _subscription = _source.onOnlineChange.listen((online) {
      if (online != _online) {
        _online = online;
        notifyListeners();
      }
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
}
