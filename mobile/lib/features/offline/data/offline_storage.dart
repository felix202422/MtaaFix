import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// File-backed JSON storage for offline artifacts (drafts, outbox, caches,
/// copied photos). The base directory can be injected for tests; production
/// uses the application support directory. All operations degrade gracefully
/// when storage is unavailable (read → null, write → false), so offline
/// features never crash the app on a locked-down device.
///
/// Every public operation runs in [Zone.root]: real file I/O must not chain
/// through a caller's zone — widget tests run in a fake-async zone where I/O
/// continuations would otherwise never drain (a form would spin forever on
/// its loading indicator).
class OfflineStorage {
  OfflineStorage({Directory? baseDir}) : _base = baseDir;

  Directory? _base;
  Future<Directory>? _rootFuture;

  Future<T> _inRoot<T>(Future<T> Function() body) => Zone.root.run(body);

  Future<Directory> _root() {
    if (_base != null) return Future.value(_base!);
    return _rootFuture ??= () async {
      try {
        final support = await getApplicationSupportDirectory();
        _base = await Directory(
                '${support.path}${Platform.pathSeparator}mtaafix_offline')
            .create(recursive: true);
      } on Object {
        // path_provider unavailable (tests / unsupported platform).
        _base = await Directory.systemTemp.createTemp('mtaafix_offline_');
      }
      return _base!;
    }();
  }

  Future<Directory> group(String name) => _inRoot(() async {
        final root = await _root();
        final dir = Directory('${root.path}${Platform.pathSeparator}$name');
        if (!await dir.exists()) {
          try {
            await dir.create(recursive: true);
          } on Object {
            return root;
          }
        }
        return dir;
      });

  Future<File> _file(String group, String name) async {
    final dir = await this.group(group);
    return File('${dir.path}${Platform.pathSeparator}$name');
  }

  Future<Map<String, dynamic>?> readJson(String group, String name) =>
      _inRoot(() async {
        try {
          final file = await _file(group, name);
          if (!await file.exists()) return null;
          final decoded = jsonDecode(await file.readAsString());
          if (decoded is Map<String, dynamic>) return decoded;
          return null;
        } on Object {
          return null;
        }
      });

  Future<bool> writeJson(
          String group, String name, Map<String, dynamic> data) =>
      _inRoot(() async {
        try {
          final file = await _file(group, name);
          final tmp = File('${file.path}.tmp');
          await tmp.writeAsString(jsonEncode(data), flush: true);
          if (await file.exists()) {
            await file.delete();
          }
          await tmp.rename(file.path);
          return true;
        } on Object {
          return false;
        }
      });

  Future<void> remove(String group, String name) => _inRoot(() async {
        try {
          final file = await _file(group, name);
          if (await file.exists()) await file.delete();
        } on Object {
          // Best effort.
        }
      });

  Future<void> removeFileByPath(String path) => _inRoot(() async {
        try {
          final file = File(path);
          if (await file.exists()) await file.delete();
        } on Object {
          // Best effort.
        }
      });

  /// Copies [sourcePath] into [group] under a fresh name and returns the new
  /// absolute path (used to keep picked photos alive past cache eviction).
  Future<String?> copyInto(String group, String sourcePath,
          {String? preferredName}) =>
      _inRoot(() async {
        try {
          final dir = await this.group(group);
          final source = File(sourcePath);
          if (!await source.exists()) return null;
          final name = preferredName ??
              '${DateTime.now().microsecondsSinceEpoch}_'
                  '${source.uri.pathSegments.last}';
          final target = File(
              '${dir.path}${Platform.pathSeparator}${name.replaceAll('/', '_')}');
          await source.copy(target.path);
          return target.path;
        } on Object {
          return null;
        }
      });
}
