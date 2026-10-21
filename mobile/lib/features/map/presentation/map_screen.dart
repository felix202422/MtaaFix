import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:mtaafix_mobile/core/map/app_tile_provider.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';

import 'package:mtaafix_mobile/app/main_shell.dart';
import 'package:mtaafix_mobile/app/status_theme.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/util/formatters.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// Public Map (Page 34): live citizen reports on OpenStreetMap with status and
/// category filters, my-location and per-marker details (FR-2.3).
class MapScreen extends StatefulWidget {
  const MapScreen({super.key});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  static const LatLng _fallbackCenter = LatLng(-1.286389, 36.817223);

  final MapController _mapController = MapController();
  late ReportsApi _api;
  bool _initiated = false;
  bool _fetching = true;
  String? _error;
  List<PublicReport> _markers = const [];
  List<Category> _categories = const [];

  String? _statusFilter;
  String? _categoryFilter;

  Timer? _debounce;
  ValueNotifier<int>? _tab;

  /// The map (and its tile loads) is only built the first time the tab is
  /// actually shown — inside the shell's IndexedStack every tab builds on
  /// mount, and mounting the tile layer eagerly costs startup work nobody
  /// sees. Standalone routes (no tab notifier) mount immediately.
  bool _mapBooted = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final tab = MainShellTab.maybeOf(context);
    if (!identical(tab, _tab)) {
      _tab?.removeListener(_onTabActivated);
      _tab = tab;
      _tab?.addListener(_onTabActivated);
    }
    if (tab == null || tab.value == MainShellTab.map) {
      _mapBooted = true;
    }
    if (!_initiated) {
      _initiated = true;
      _api = ReportsApi(AuthScope.of(context).api);
      _loadCategories();
      // The map camera only exists after the first frame, so the very first
      // fetch is deferred to include the viewport bbox.
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _fetch();
      });
    }
  }

  void _onTabActivated() {
    if (!mounted || _tab?.value != MainShellTab.map) return;
    if (!_mapBooted) setState(() => _mapBooted = true);
    _fetch();
  }

  @override
  void dispose() {
    _tab?.removeListener(_onTabActivated);
    _debounce?.cancel();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    try {
      final categories = await _api.categories();
      if (mounted) setState(() => _categories = categories);
    } catch (_) {
      if (mounted) setState(() => _categories = const []);
    }
  }

  String? _bbox() {
    try {
      final camera = _mapController.camera;
      final bounds = camera.visibleBounds;
      return '${bounds.west},${bounds.south},${bounds.east},${bounds.north}';
    } catch (_) {
      return null;
    }
  }

  Future<void> _fetch() async {
    setState(() {
      _fetching = true;
      _error = null;
    });
    try {
      final result = await _api.publicList(
        status: _statusFilter,
        categoryId: _categoryFilter,
        bbox: _bbox(),
      );
      if (!mounted) return;
      setState(() {
        _markers = result.content
            .where((r) => r.latitude != null && r.longitude != null)
            .toList(growable: false);
        _fetching = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _fetching = false;
        _error = 'Could not load the map feed.';
      });
    }
  }

  void _onCameraChanged() {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), _fetch);
  }

  Future<void> _goToMyLocation() async {
    try {
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text(
                'Location permission is off — pan the map instead.',
              ),
            ),
          );
        }
        return;
      }
      final position = await Geolocator.getCurrentPosition();
      _mapController.move(
        LatLng(position.latitude, position.longitude),
        15,
      );
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not read your location.')),
        );
      }
    }
  }

  Color _markerColor(ReportStatus status) => switch (status) {
        ReportStatus.submitted ||
        ReportStatus.underReview ||
        ReportStatus.verified =>
          MtaaFixColors.warningAmber,
        ReportStatus.needsInformation ||
        ReportStatus.rejected ||
        ReportStatus.duplicate =>
          MtaaFixColors.errorRed,
        ReportStatus.inProgress ||
        ReportStatus.assigned ||
        ReportStatus.resolutionReview ||
        ReportStatus.reopened =>
          MtaaFixColors.primaryGreen,
        ReportStatus.resolved || ReportStatus.closed => Colors.blueGrey,
      };

  Future<void> _showDetails(PublicReport report) async {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) {
        return FutureBuilder<PublicReportDetail>(
          future: _api.getPublic(report.code),
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              );
            }
            final detail = snapshot.data;
            final error = snapshot.error;
            final text = Theme.of(sheetContext).textTheme;
            if (detail == null) {
              return Padding(
                padding: const EdgeInsets.all(24),
                child: Text('Details unavailable: $error'),
              );
            }
            return Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      StatusChip(status: detail.status, dense: true),
                      const Spacer(),
                      Text(
                        detail.code,
                        style: text.labelMedium?.copyWith(
                          color: MtaaFixColors.primaryGreen,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text(detail.title, style: text.titleLarge),
                  if (detail.categoryName != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      detail.categoryName!,
                      style: text.labelMedium
                          ?.copyWith(color: MtaaFixColors.mutedText),
                    ),
                  ],
                  if (detail.reportedAt != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      'Reported ${formatDate(detail.reportedAt)}',
                      style: text.labelSmall
                          ?.copyWith(color: MtaaFixColors.mutedText),
                    ),
                  ],
                  if (detail.description != null &&
                      detail.description!.trim().isNotEmpty) ...[
                    const SizedBox(height: 10),
                    Text(detail.description!.trim(), style: text.bodyMedium),
                  ],
                  if (detail.timeline.isNotEmpty) ...[
                    const SizedBox(height: 14),
                    Text('Timeline', style: text.titleSmall),
                    const SizedBox(height: 6),
                    for (final entry in detail.timeline)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: Row(
                          children: [
                            Icon(Icons.circle_rounded,
                                size: 8,
                                color: statusColor(
                                    entry.status,
                                    Theme.of(sheetContext).colorScheme)),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                '${statusLabel(entry.status)}'
                                '${entry.timestamp != null ? ' · ${formatDate(entry.timestamp)}' : ''}',
                                style: text.bodySmall,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ],
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Community Map'),
        actions: [
          IconButton(
            tooltip: 'Refresh map',
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _fetch,
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
            child: Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<String?>(
                    initialValue: _statusFilter,
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Status',
                      isDense: true,
                    ),
                    items: [
                      const DropdownMenuItem<String?>(
                          value: null, child: Text('All statuses')),
                      for (final status in ReportStatus.values)
                        DropdownMenuItem<String?>(
                          value: status.wire,
                          child: Text(statusLabel(status)),
                        ),
                    ],
                    onChanged: (value) {
                      setState(() => _statusFilter = value);
                      _fetch();
                    },
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: DropdownButtonFormField<String?>(
                    initialValue: _categoryFilter,
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Category',
                      isDense: true,
                    ),
                    items: [
                      const DropdownMenuItem<String?>(
                          value: null, child: Text('All categories')),
                      for (final category in _categories)
                        DropdownMenuItem<String?>(
                          value: category.id,
                          child: Text(
                            category.name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                    onChanged: (value) {
                      setState(() => _categoryFilter = value);
                      _fetch();
                    },
                  ),
                ),
              ],
            ),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      _error!,
                      style: text.bodySmall
                          ?.copyWith(color: MtaaFixColors.errorRed),
                    ),
                  ),
                  TextButton(onPressed: _fetch, child: const Text('Retry')),
                ],
              ),
            ),
          Expanded(
            child: Stack(
              children: [
                if (!_mapBooted)
                  const SizedBox.expand(
                    child: ColoredBox(
                      color: Color(0xFFEDF1EE),
                      child: Center(
                        child: Icon(
                          Icons.map_outlined,
                          size: 56,
                          color: MtaaFixColors.mutedText,
                        ),
                      ),
                    ),
                  )
                else
                  FlutterMap(
                  mapController: _mapController,
                  options: MapOptions(
                    initialCenter: _fallbackCenter,
                    initialZoom: 12,
                    onPositionChanged: (_, __) => _onCameraChanged(),
                  ),
                  children: [
                    TileLayer(
                      urlTemplate:
                          'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                      userAgentPackageName: 'com.mtaafix.mobile',
                      tileProvider: appTileProvider(),
                    ),
                    MarkerLayer(
                      markers: [
                        for (final report in _markers)
                          Marker(
                            point: LatLng(
                                report.latitude!, report.longitude!),
                            width: 40,
                            height: 40,
                            child: GestureDetector(
                              onTap: () => _showDetails(report),
                              child: Icon(
                                Icons.location_pin,
                                size: 36,
                                color: _markerColor(report.status),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
                if (_fetching)
                  const Positioned(
                    top: 8,
                    left: 0,
                    right: 0,
                    child: Center(
                      child: SizedBox(
                        width: 26,
                        height: 26,
                        child: CircularProgressIndicator(strokeWidth: 3),
                      ),
                    ),
                  ),
                Positioned(
                  right: 12,
                  bottom: 12,
                  child: Column(
                    children: [
                      FloatingActionButton.small(
                        heroTag: 'map-my-location',
                        tooltip: 'My location',
                        onPressed: _goToMyLocation,
                        child: const Icon(Icons.my_location_rounded),
                      ),
                      const SizedBox(height: 8),
                      FloatingActionButton.small(
                        heroTag: 'map-count',
                        tooltip: 'Reports on screen',
                        onPressed: null,
                        child: Text('${_markers.length}'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
