import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:mtaafix_mobile/core/map/app_tile_provider.dart';
import 'package:latlong2/latlong.dart';

import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';

/// Map pin picker used by the report form when "Pick on map" is chosen.
class MapPickerScreen extends StatefulWidget {
  const MapPickerScreen({super.key, this.initial});

  final ReportLocation? initial;

  @override
  State<MapPickerScreen> createState() => _MapPickerScreenState();
}

class _MapPickerScreenState extends State<MapPickerScreen> {
  static const LatLng _fallbackCenter = LatLng(-1.286389, 36.817223);

  late LatLng _center;
  LatLng? _picked;
  bool _placedByTap = false;

  @override
  void initState() {
    super.initState();
    final initial = widget.initial;
    if (initial != null) {
      _center = LatLng(initial.latitude, initial.longitude);
      _picked = _center;
      _placedByTap = true;
    } else {
      _center = _fallbackCenter;
    }
  }

  void _confirm() {
    final picked = _picked;
    if (picked == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Tap the map to drop a pin first.')),
      );
      return;
    }
    Navigator.pop(
      context,
      ReportLocation(latitude: picked.latitude, longitude: picked.longitude),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pick a location'),
        actions: [
          TextButton(onPressed: _confirm, child: const Text('Use this pin')),
        ],
      ),
      body: Stack(
        children: [
          FlutterMap(
            options: MapOptions(
              initialCenter: _center,
              initialZoom: widget.initial != null ? 16 : 12,
              onTap: (tapPosition, point) {
                setState(() {
                  _picked = point;
                  _placedByTap = true;
                });
              },
            ),
            children: [
              TileLayer(
                urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                userAgentPackageName: 'com.mtaafix.mobile',
                tileProvider: appTileProvider(),
              ),
              if (_picked != null)
                MarkerLayer(
                  markers: [
                    Marker(
                      point: _picked!,
                      width: 44,
                      height: 44,
                      child: const Icon(
                        Icons.location_pin,
                        size: 44,
                        color: MtaaFixColors.errorRed,
                      ),
                    ),
                  ],
                ),
            ],
          ),
          Positioned(
            left: 16,
            right: 16,
            bottom: 24,
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _picked == null
                          ? 'Tap anywhere on the map to place a pin.'
                          : 'Pin: ${_picked!.latitude.toStringAsFixed(5)}, '
                              '${_picked!.longitude.toStringAsFixed(5)}',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton(
                            onPressed: () {
                              setState(() {
                                _picked = _center;
                                _placedByTap = true;
                              });
                            },
                            child: const Text('Pin at map centre'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _confirm,
                            child: const Text('Use this pin'),
                          ),
                        ),
                      ],
                    ),
                    if (_placedByTap && _picked != null) ...[
                      const SizedBox(height: 6),
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton.icon(
                          onPressed: () => setState(() {
                            _picked = null;
                            _placedByTap = false;
                          }),
                          icon: const Icon(Icons.delete_outline_rounded,
                              size: 18),
                          label: const Text('Remove pin'),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
