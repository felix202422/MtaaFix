import 'dart:io';

import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/core/widgets/loading_view.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';
import 'package:mtaafix_mobile/features/reports/presentation/map_picker_screen.dart';

/// Report creation (Page 31) and edit (FR-2.6): category, title, description,
/// photo capture/selection, location selection, review and submission.
class ReportFormScreen extends StatefulWidget {
  const ReportFormScreen({super.key, this.editReport});

  final Report? editReport;

  bool get isEdit => editReport != null;

  @override
  State<ReportFormScreen> createState() => _ReportFormScreenState();
}

class _ReportFormScreenState extends State<ReportFormScreen> {
  static const int maxPhotos = 5;
  static const int maxPhotoBytes = 20 * 1024 * 1024;
  static const allowedMime = ['image/jpeg', 'image/png', 'image/webp'];

  final _formKey = GlobalKey<FormState>();
  final _title = TextEditingController();
  final _description = TextEditingController();
  final _picker = ImagePicker();

  late ReportsApi _api;
  bool _apiReady = false;
  bool _loadingCategories = true;
  List<Category> _categories = const [];
  Category? _category;
  ReportLocation? _location;
  final List<File> _photos = [];
  bool _submitting = false;
  String? _submitError;

  @override
  void initState() {
    super.initState();
    final edit = widget.editReport;
    if (edit != null) {
      _title.text = edit.title;
      _description.text = edit.description ?? '';
      _location = edit.location;
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_apiReady) {
      _apiReady = true;
      _api = ReportsApi(AuthScope.of(context).api);
      _loadCategories();
    }
  }

  @override
  void dispose() {
    _title.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    try {
      final categories = await _api.categories();
      if (!mounted) return;
      setState(() {
        _categories = categories;
        _loadingCategories = false;
        if (_category == null && widget.isEdit) {
          final match = categories
              .where((c) => c.id == widget.editReport!.categoryId);
          if (match.isNotEmpty) _category = match.first;
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loadingCategories = false;
        _submitError = 'Could not load categories.';
      });
    }
  }

  void _snack(String message, {bool error = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: error ? MtaaFixColors.errorRed : null,
        ),
      );
  }

  Future<void> _useCurrentLocation() async {
    try {
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied) {
        _snack(
          'Location permission denied — pick the spot on the map instead.',
          error: true,
        );
        return;
      }
      if (permission == LocationPermission.deniedForever) {
        _snack(
          'Location is blocked in system settings — pick the spot on the map instead.',
          error: true,
        );
        return;
      }
      final position = await Geolocator.getCurrentPosition();
      if (!mounted) return;
      setState(() {
        _location = ReportLocation(
          latitude: position.latitude,
          longitude: position.longitude,
        );
      });
    } catch (e) {
      _snack('Could not read your location — pick the spot on the map instead.',
          error: true);
    }
  }

  Future<void> _pickOnMap() async {
    final picked = await Navigator.push<ReportLocation>(
      context,
      MaterialPageRoute(
        builder: (_) => MapPickerScreen(initial: _location),
        settings: const RouteSettings(name: 'map-picker'),
      ),
    );
    if (picked != null) setState(() => _location = picked);
  }

  Future<void> _addPhoto(ImageSource source) async {
    try {
      final picked = await _picker.pickImage(
        source: source,
        maxWidth: 1920,
        imageQuality: 85,
      );
      if (picked == null) return;
      final file = File(picked.path);
      final size = await file.length();
      final mime = _mimeType(picked.path, picked.name);
      if (!allowedMime.contains(mime)) {
        _snack('Only JPEG, PNG, or WEBP photos are allowed.', error: true);
        return;
      }
      if (size > maxPhotoBytes) {
        _snack('Photos must be 20 MB or smaller.', error: true);
        return;
      }
      if (_photos.length >= maxPhotos) {
        _snack('You can attach up to $maxPhotos photos.', error: true);
        return;
      }
      setState(() => _photos.add(file));
    } catch (e) {
      _snack('Could not add that photo.', error: true);
    }
  }

  String _mimeType(String path, String name) {
    final lower = name.toLowerCase();
    if (lower.endsWith('.png')) return 'image/png';
    if (lower.endsWith('.webp')) return 'image/webp';
    return 'image/jpeg';
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    if (_category == null) {
      _snack('Choose a category.', error: true);
      return;
    }
    if (_location == null) {
      _snack('Add the issue location (current location or map pin).',
          error: true);
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _submitting = true;
      _submitError = null;
    });
    try {
      if (widget.isEdit) {
        await _api.update(
          widget.editReport!.id,
          title: _title.text.trim(),
          description: _description.text.trim(),
          categoryId: _category!.id,
        );
        if (!mounted) return;
        _snack('Report updated.');
        Navigator.pop(context, widget.editReport!.id);
        return;
      }

      final created = await _api.create(
        categoryId: _category!.id,
        title: _title.text.trim(),
        description: _description.text.trim(),
        latitude: _location!.latitude,
        longitude: _location!.longitude,
      );
      int failed = 0;
      for (final photo in _photos) {
        try {
          await _api.uploadPhoto(created.id, photo.path);
        } catch (e) {
          failed++;
        }
      }
      if (!mounted) return;
      if (failed > 0) {
        _snack(
          'Report ${created.code} created, but $failed photo(s) failed to attach.',
          error: true,
        );
      } else {
        _snack('Report ${created.code} submitted.');
      }
      Navigator.pushReplacementNamed(
        context,
        Routes.reportDetail,
        arguments: created.id,
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _submitting = false;
        _submitError = e.message;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _submitting = false;
        _submitError = 'Submission failed. Please try again.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isEdit ? 'Edit Report' : 'Report an Issue'),
      ),
      body: _loadingCategories
          ? const LoadingView()
          : Form(
              key: _formKey,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                  if (_submitError != null)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Text(
                        _submitError!,
                        style: text.bodyMedium
                            ?.copyWith(color: MtaaFixColors.errorRed),
                      ),
                    ),
                  DropdownButtonFormField<Category>(
                    initialValue: _category,
                    decoration: const InputDecoration(
                      labelText: 'Category *',
                      hintText: 'What kind of issue is this?',
                    ),
                    items: [
                      for (final category in _categories)
                        DropdownMenuItem<Category>(
                          value: category,
                          child: Text(
                            category.name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                    onChanged: (value) => setState(() => _category = value),
                    validator: (value) =>
                        value == null ? 'Choose a category' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _title,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: const InputDecoration(
                      labelText: 'Title *',
                      hintText: 'Short summary of the issue',
                    ),
                    validator: (value) {
                      final v = value?.trim() ?? '';
                      if (v.isEmpty) return 'A title is required';
                      if (v.length > 255) return 'Max 255 characters';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _description,
                    textCapitalization: TextCapitalization.sentences,
                    minLines: 3,
                    maxLines: 6,
                    decoration: const InputDecoration(
                      labelText: 'Description',
                      hintText: 'What is wrong? Where exactly?',
                    ),
                  ),
                  const SizedBox(height: 24),
                  Text('Location *', style: text.titleMedium),
                  const SizedBox(height: 8),
                  _location == null
                      ? Text(
                          'No location set yet.',
                          style: text.bodySmall
                              ?.copyWith(color: MtaaFixColors.mutedText),
                        )
                      : Card(
                          child: ListTile(
                            leading: const Icon(Icons.location_on_rounded,
                                color: MtaaFixColors.primaryGreen),
                            title: Text(
                              '${_location!.latitude.toStringAsFixed(5)}, '
                              '${_location!.longitude.toStringAsFixed(5)}',
                            ),
                            trailing: TextButton(
                              onPressed: () =>
                                  setState(() => _location = null),
                              child: const Text('Clear'),
                            ),
                          ),
                        ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed:
                              _submitting ? null : _useCurrentLocation,
                          icon: const Icon(Icons.my_location_rounded),
                          label: const Text('Current location'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: _submitting ? null : _pickOnMap,
                          icon: const Icon(Icons.map_rounded),
                          label: const Text('Pick on map'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  if (!widget.isEdit) ...[
                    Text('Photos (${_photos.length}/$maxPhotos)',
                        style: text.titleMedium),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        OutlinedButton.icon(
                          onPressed:
                              _submitting ? null : () => _addPhoto(ImageSource.camera),
                          icon: const Icon(Icons.photo_camera_rounded),
                          label: const Text('Camera'),
                        ),
                        const SizedBox(width: 8),
                        OutlinedButton.icon(
                          onPressed: _submitting
                              ? null
                              : () => _addPhoto(ImageSource.gallery),
                          icon: const Icon(Icons.photo_library_rounded),
                          label: const Text('Gallery'),
                        ),
                      ],
                    ),
                    if (_photos.isNotEmpty) ...[
                      const SizedBox(height: 10),
                      SizedBox(
                        height: 88,
                        child: ListView.separated(
                          scrollDirection: Axis.horizontal,
                          itemCount: _photos.length,
                          separatorBuilder: (_, __) =>
                              const SizedBox(width: 8),
                          itemBuilder: (context, index) {
                            final file = _photos[index];
                            return Stack(
                              children: [
                                ClipRRect(
                                  borderRadius: BorderRadius.circular(10),
                                  child: Image.file(
                                    file,
                                    width: 88,
                                    height: 88,
                                    fit: BoxFit.cover,
                                  ),
                                ),
                                Positioned(
                                  top: 2,
                                  right: 2,
                                  child: InkWell(
                                    onTap: () => setState(
                                        () => _photos.removeAt(index)),
                                    child: Container(
                                      decoration: BoxDecoration(
                                        color:
                                            Colors.black.withValues(alpha: 0.55),
                                        shape: BoxShape.circle,
                                      ),
                                      child: const Icon(Icons.close_rounded,
                                          size: 18, color: Colors.white),
                                    ),
                                  ),
                                ),
                              ],
                            );
                          },
                        ),
                      ),
                    ],
                    const SizedBox(height: 24),
                    Text('Review', style: text.titleMedium),
                    const SizedBox(height: 8),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _ReviewRow(
                              label: 'Category',
                              value: _category?.name ?? '—',
                            ),
                            _ReviewRow(
                              label: 'Title',
                              value: _title.text.trim().isEmpty
                                  ? '—'
                                  : _title.text.trim(),
                            ),
                            _ReviewRow(
                              label: 'Location',
                              value: _location == null
                                  ? '—'
                                  : '${_location!.latitude.toStringAsFixed(5)}, '
                                      '${_location!.longitude.toStringAsFixed(5)}',
                            ),
                            _ReviewRow(
                              label: 'Photos',
                              value: '${_photos.length}',
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                  SizedBox(
                    height: 48,
                    child: ElevatedButton(
                      onPressed: _submitting ? null : _submit,
                      child: _submitting
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: MtaaFixColors.surface,
                              ),
                            )
                          : Text(widget.isEdit ? 'Save changes' : 'Submit report'),
                    ),
                  ),
                  const SizedBox(height: 32),
                ],
                ),
              ),
            ),
    );
  }
}

class _ReviewRow extends StatelessWidget {
  const _ReviewRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 90,
            child: Text(
              label,
              style: text.labelMedium?.copyWith(color: MtaaFixColors.mutedText),
            ),
          ),
          Expanded(child: Text(value, style: text.bodyMedium)),
        ],
      ),
    );
  }
}
