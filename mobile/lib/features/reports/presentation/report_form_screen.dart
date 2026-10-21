import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/app/theme.dart';
import 'package:mtaafix_mobile/core/network/api_exception.dart';
import 'package:mtaafix_mobile/core/utils/uuid.dart';
import 'package:mtaafix_mobile/core/widgets/loading_view.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_controller.dart';
import 'package:mtaafix_mobile/features/offline/application/offline_scope.dart';
import 'package:mtaafix_mobile/features/offline/domain/pending_submission.dart';
import 'package:mtaafix_mobile/features/offline/domain/report_draft.dart';
import 'package:mtaafix_mobile/features/reports/application/reports_api.dart';
import 'package:mtaafix_mobile/features/reports/domain/report.dart';
import 'package:mtaafix_mobile/features/reports/presentation/map_picker_screen.dart';

/// Report creation (Page 31) and edit (FR-2.6): category, title, description,
/// photo capture/selection, location selection, review and submission.
///
/// Phase 14: creation drafts are autosaved locally and restorable after a
/// restart; an offline submission enters the pending queue instead of
/// failing. Edits to existing reports stay online-only.
class ReportFormScreen extends StatefulWidget {
  const ReportFormScreen({super.key, this.editReport, this.pending});

  final Report? editReport;

  /// A failed pending submission being re-edited (never both at once —
  /// [editReport] wins if both were somehow supplied).
  final PendingSubmission? pending;

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

  OfflineController? _offline;
  ReportDraft? _restorableDraft;
  bool _staleCategories = false;
  Timer? _draftDebounce;

  /// One idempotency key per form session: retries (including the queued
  /// offline retry) reuse it so a timed-out create never files twice.
  String? _idempotencyKey;

  @override
  void initState() {
    super.initState();
    final edit = widget.editReport;
    if (edit != null) {
      _title.text = edit.title;
      _description.text = edit.description ?? '';
      _location = edit.location;
    }
    final pending = widget.pending;
    if (pending != null && edit == null) {
      _title.text = pending.title;
      _description.text = pending.description ?? '';
      _location = ReportLocation(
        latitude: pending.latitude,
        longitude: pending.longitude,
      );
      _photos.addAll(pending.photoPaths.map(File.new));
    }
    _title.addListener(_scheduleDraftSave);
    _description.addListener(_scheduleDraftSave);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_apiReady) {
      _apiReady = true;
      _api = ReportsApi(AuthScope.of(context).api);
      _offline = OfflineScope.read(context);
      _loadCategories();
      if (!widget.isEdit && widget.pending == null) _checkForDraft();
    }
  }

  @override
  void dispose() {
    _draftDebounce?.cancel();
    _title.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    try {
      final raw = await _api.categoriesJson();
      final list = raw is Map<String, dynamic> && raw['content'] is List
          ? raw['content'] as List
          : raw;
      final categories = <Category>[];
      if (list is List) {
        for (final entry in list) {
          if (entry is! Map<String, dynamic>) continue;
          final category = Category.fromJson(entry);
          if (category.id.isNotEmpty && category.active != false) {
            categories.add(category);
          }
        }
      }
      // Success → refresh the offline copy for the next offline session.
      await _offline?.categories.save(list is List ? list : const []);
      if (!mounted) return;
      setState(() {
        _categories = categories;
        _loadingCategories = false;
        _staleCategories = false;
        _applyPrefillCategory(categories);
      });
    } catch (e) {
      // Offline or failed → fall back to the last cached copy.
      final cached = await _offline?.categories.load();
      if (!mounted) return;
      if (cached != null && cached.isNotEmpty) {
        final categories = cached.map(Category.fromJson).toList(growable: false);
        setState(() {
          _categories = categories;
          _loadingCategories = false;
          _staleCategories = true;
          _applyPrefillCategory(categories);
        });
      } else {
        setState(() {
          _loadingCategories = false;
          _submitError = 'Could not load categories.';
        });
      }
    }
  }

  /// Matches the dropdown to the edited/pending report's category once the
  /// list is available.
  void _applyPrefillCategory(List<Category> categories) {
    if (_category != null) return;
    final wantedId = widget.editReport?.categoryId ?? widget.pending?.categoryId;
    if (wantedId == null) return;
    for (final category in categories) {
      if (category.id == wantedId) {
        _category = category;
        return;
      }
    }
  }

  Future<void> _checkForDraft() async {
    final draft = await _offline?.loadDraft();
    if (!mounted || draft == null) return;
    final hasContent = draft.title.trim().isNotEmpty ||
        (draft.description?.trim().isNotEmpty ?? false) ||
        draft.photoPaths.isNotEmpty;
    if (!hasContent) return;
    setState(() => _restorableDraft = draft);
  }

  void _scheduleDraftSave() {
    if (widget.isEdit || widget.pending != null || _offline == null) return;
    _draftDebounce?.cancel();
    _draftDebounce = Timer(const Duration(milliseconds: 400), _persistDraft);
  }

  Future<void> _persistDraft() async {
    final offline = _offline;
    if (offline == null || !mounted || widget.isEdit || widget.pending != null) {
      return;
    }
    final title = _title.text.trim();
    final description = _description.text.trim();
    if (title.isEmpty &&
        description.isEmpty &&
        _photos.isEmpty &&
        _category == null &&
        _location == null) {
      await offline.clearDraft(releasePhotos: true);
      return;
    }
    await offline.saveDraft(ReportDraft(
      categoryId: _category?.id ?? '',
      categoryName: _category?.name,
      title: title,
      description: description.isEmpty ? null : description,
      // 0/0 is the "no location yet" sentinel (documented in the design doc).
      latitude: _location?.latitude ?? 0,
      longitude: _location?.longitude ?? 0,
      photoPaths: _photos.map((f) => f.path).toList(growable: false),
      updatedAt: DateTime.now(),
    ));
  }

  Future<void> _restoreDraft() async {
    final draft = _restorableDraft;
    if (draft == null) return;
    setState(() {
      _restorableDraft = null;
      _title.text = draft.title;
      _description.text = draft.description ?? '';
      if (draft.latitude != 0 || draft.longitude != 0) {
        _location = ReportLocation(
          latitude: draft.latitude,
          longitude: draft.longitude,
        );
      }
      _category = _categories
          .where((c) => c.id == draft.categoryId)
          .firstOrNull;
    });
    // Only restore photo files that still exist on disk.
    final surviving = <File>[];
    for (final path in draft.photoPaths) {
      if (await File(path).exists()) surviving.add(File(path));
    }
    if (!mounted) return;
    setState(() => _photos
      ..clear()
      ..addAll(surviving));
  }

  void _discardRestorableDraft() {
    final draft = _restorableDraft;
    setState(() => _restorableDraft = null);
    if (draft == null) return;
    _offline?.clearDraft(releasePhotos: true);
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
      _scheduleDraftSave();
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
    if (picked != null) {
      setState(() => _location = picked);
      _scheduleDraftSave();
    }
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
      // Copy into the offline store so the file survives cache eviction and
      // can be attached later by the pending queue.
      if (!mounted) return;
      final copied = await OfflineScope.read(context)
          ?.storage
          .copyInto('photos', picked.path);
      setState(() => _photos.add(File(copied ?? picked.path)));
      _scheduleDraftSave();
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
    _draftDebounce?.cancel();
    setState(() {
      _submitting = true;
      _submitError = null;
    });
    final offline = _offline;
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

      // Offline → hand the whole submission to the pending queue instead of
      // failing; nothing is shown as submitted until the server accepts it.
      if (offline != null && !offline.isOnline) {
        await _enqueueOffline(offline);
        return;
      }

      _idempotencyKey ??= generateUuidV4();
      final created = await _api.create(
        categoryId: _category!.id,
        title: _title.text.trim(),
        description: _description.text.trim(),
        latitude: _location!.latitude,
        longitude: _location!.longitude,
        idempotencyKey: _idempotencyKey,
      );
      int failed = 0;
      final strandedPhotos = <String>[];
      for (final photo in _photos) {
        try {
          await _api.uploadPhoto(created.id, photo.path);
        } on ApiException catch (e) {
          if (e.isNetwork && offline != null) {
            // Connection dropped mid-attach — keep the file and queue the
            // remaining photos for the pending queue (report exists).
            strandedPhotos.add(photo.path);
          } else {
            failed++;
          }
        } catch (e) {
          failed++;
        }
      }
      await _finishSuccessfulSubmission(offline, keepPhotoPaths: strandedPhotos);
      if (!mounted) return;
      if (strandedPhotos.isNotEmpty && offline != null) {
        final now = DateTime.now();
        await offline.enqueue(PendingSubmission(
          localId: generateUuidV4(),
          idempotencyKey: _idempotencyKey!,
          categoryId: _category!.id,
          categoryName: _category?.name,
          title: _title.text.trim(),
          description: _description.text.trim().isEmpty
              ? null
              : _description.text.trim(),
          latitude: _location!.latitude,
          longitude: _location!.longitude,
          photoPaths: List<String>.of(strandedPhotos),
          uploadedPhotoCount: _photos.length - strandedPhotos.length,
          state: PendingSubmissionState.attachingPhotos,
          serverId: created.id,
          serverCode: created.code,
          createdAt: now,
          updatedAt: now,
        ));
        _snack(
          'Report ${created.code} created — ${strandedPhotos.length} '
          'photo(s) will attach when you are back online.',
        );
      } else if (failed > 0) {
        _snack(
          'Report ${created.code} created, but $failed photo(s) failed to attach.',
          error: true,
        );
      } else {
        _snack('Report ${created.code} submitted.');
      }
      if (!mounted) return;
      Navigator.pushReplacementNamed(
        context,
        Routes.reportDetail,
        arguments: created.id,
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      if (!widget.isEdit && offline != null && e.isNetwork) {
        await _enqueueOffline(offline);
        return;
      }
      setState(() {
        _submitting = false;
        _submitError = e.message;
      });
      _persistDraft();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _submitting = false;
        _submitError = 'Submission failed. Please try again.';
      });
      _persistDraft();
    }
  }

  /// Queues the current form as a pending submission (offline path).
  Future<void> _enqueueOffline(OfflineController offline) async {
    _draftDebounce?.cancel();
    final description = _description.text.trim();
    final item = PendingSubmission(
      localId: generateUuidV4(),
      // Reuse the form-session key: if an earlier attempt actually reached
      // the server, the retry replays instead of duplicating.
      idempotencyKey: _idempotencyKey ??= generateUuidV4(),
      categoryId: _category!.id,
      categoryName: _category?.name,
      title: _title.text.trim(),
      description: description.isEmpty ? null : description,
      latitude: _location!.latitude,
      longitude: _location!.longitude,
      photoPaths: _photos.map((f) => f.path).toList(growable: false),
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
    await offline.enqueue(item);
    // Editing an old failed submission: drop it now that its content lives
    // in the new entry (photo files referenced by the new entry survive).
    if (widget.pending != null) {
      await offline.discard(widget.pending!.localId,
          keepPaths: item.photoPaths.toSet());
    } else {
      // This submission was the draft — it now lives in the queue.
      await offline.clearDraft(releasePhotos: false);
    }
    if (!mounted) return;
    _snack(
      'No connection — saved to your pending queue. It will be submitted '
      'automatically when you are back online.',
    );
    Navigator.pop(context);
  }

  /// After a fully accepted submission: drop the draft and its local photo
  /// files (files in [keepPhotoPaths] stay — they belong to the queue).
  Future<void> _finishSuccessfulSubmission(
    OfflineController? offline, {
    List<String> keepPhotoPaths = const [],
  }) async {
    if (offline == null) return;
    if (widget.pending != null) {
      await offline.discard(widget.pending!.localId,
          keepPaths: keepPhotoPaths.toSet());
    } else {
      // Creation mode: the draft is this report. (A prefill submission must
      // not wipe a draft belonging to a different, unfinished report.)
      final draft = await offline.loadDraft();
      if (draft != null) {
        final keep = keepPhotoPaths.toSet();
        await offline.drafts.releasePhotos(
            draft.photoPaths.where((p) => !keep.contains(p)).toList());
        await offline.clearDraft(releasePhotos: false);
      }
    }
    final keep = keepPhotoPaths.toSet();
    for (final photo in _photos) {
      if (!keep.contains(photo.path)) {
        await offline.storage.removeFileByPath(photo.path);
      }
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
                  if (_restorableDraft != null)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Card(
                        color: MtaaFixColors.flagYellow.withValues(alpha: 0.18),
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(12, 8, 4, 4),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Restore your previous draft?',
                                style: text.titleSmall,
                              ),
                              Text(
                                'A draft from your last session was found.',
                                style: text.bodySmall?.copyWith(
                                    color: MtaaFixColors.mutedText),
                              ),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.end,
                                children: [
                                  TextButton(
                                    onPressed: _discardRestorableDraft,
                                    child: const Text('Discard'),
                                  ),
                                  TextButton(
                                    onPressed: _restoreDraft,
                                    child: const Text('Restore'),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
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
                    onChanged: (value) {
                      setState(() => _category = value);
                      _scheduleDraftSave();
                    },
                    validator: (value) =>
                        value == null ? 'Choose a category' : null,
                  ),
                  if (_staleCategories)
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(
                        'Offline — showing saved categories, which may be '
                        'outdated.',
                        style: text.bodySmall
                            ?.copyWith(color: MtaaFixColors.warningAmber),
                      ),
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
                              onPressed: () {
                                setState(() => _location = null);
                                _scheduleDraftSave();
                              },
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
                                    onTap: () {
                                      setState(() => _photos.removeAt(index));
                                      _scheduleDraftSave();
                                    },
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
