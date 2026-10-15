import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Field, TextAreaField, useToast } from '../../design-system';
import { api, isApiError } from '../../lib/api';
import type { Category, Location, Report } from '../../lib/types';

const DRAFT_KEY = 'mtaafix.reportDraft.v1';

interface Draft {
  step: number;
  categoryId: string;
  title: string;
  description: string;
  location: Location | null;
}

const EMPTY_DRAFT: Draft = {
  step: 0,
  categoryId: '',
  title: '',
  description: '',
  location: null,
};

function loadDraft(): Draft {
  try {
    const raw = localStorage.getItem(DRAFT_KEY);
    if (!raw) return EMPTY_DRAFT;
    return { ...EMPTY_DRAFT, ...(JSON.parse(raw) as Partial<Draft>) };
  } catch {
    return EMPTY_DRAFT;
  }
}

function saveDraft(d: Draft) {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(d));
  } catch {
    // Storage unavailable — draft just won't persist.
  }
}

function clearDraft() {
  try {
    localStorage.removeItem(DRAFT_KEY);
  } catch {
    // Ignore.
  }
}

const STEPS = ['Category', 'Details', 'Location', 'Review'] as const;

const MAX_PHOTO_BYTES = 20 * 1024 * 1024;
const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export function ReportWizardPage() {
  const navigate = useNavigate();
  const { push } = useToast();
  const [draft, setDraft] = useState<Draft>(loadDraft);
  const [categories, setCategories] = useState<Category[]>([]);
  const [catError, setCatError] = useState<string | null>(null);
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [locBusy, setLocBusy] = useState(false);
  const [created, setCreated] = useState<Report | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    saveDraft(draft);
  }, [draft]);

  useEffect(() => {
    api
      .getCategories()
      .then(setCategories)
      .catch((err) =>
        setCatError(isApiError(err) ? err.message : 'Could not load categories.'),
      );
  }, []);

  const patch = useCallback((p: Partial<Draft>) => setDraft((d) => ({ ...d, ...p })), []);

  const step = draft.step;
  const canNext = useMemo(() => {
    switch (step) {
      case 0:
        return draft.categoryId !== '';
      case 1:
        return draft.title.trim().length > 0;
      case 2:
        return draft.location !== null;
      default:
        return true;
    }
  }, [step, draft.categoryId, draft.title, draft.location]);

  function onPickFile(file: File | null) {
    setPhotoError(null);
    if (!file) {
      setPhoto(null);
      return;
    }
    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      setPhotoError('Only JPEG, PNG, or WEBP photos are allowed.');
      setPhoto(null);
      return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
      setPhotoError('Photo must be 20 MB or smaller.');
      setPhoto(null);
      return;
    }
    setPhoto(file);
  }

  function useMyLocation() {
    if (!('geolocation' in navigator)) {
      push('Geolocation is not supported in this browser.', 'error');
      return;
    }
    setLocBusy(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        patch({
          location: {
            latitude: Number(pos.coords.latitude.toFixed(6)),
            longitude: Number(pos.coords.longitude.toFixed(6)),
            srid: 4326,
          },
        });
        setLocBusy(false);
      },
      () => {
        push('Could not read your location. Enter coordinates manually.', 'error');
        setLocBusy(false);
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  }

  async function submitReport() {
    setSubmitError(null);
    if (!draft.location || !draft.categoryId) return;
    setBusy(true);
    try {
      const report = await api.createReport({
        categoryId: draft.categoryId,
        title: draft.title.trim(),
        description: draft.description.trim() || undefined,
        location: {
          latitude: draft.location.latitude,
          longitude: draft.location.longitude,
          srid: draft.location.srid ?? 4326,
        },
      });
      let finalReport = report;
      if (photo) {
        finalReport = await api.uploadPhoto(report.id, photo);
      }
      clearDraft();
      setCreated(finalReport);
      push('Report submitted.', 'success');
    } catch (err) {
      setSubmitError(isApiError(err) ? err.message : 'Could not submit the report.');
    } finally {
      setBusy(false);
    }
  }

  if (created) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-12 text-center sm:px-6">
        <h1 className="text-3xl font-extrabold">Report submitted</h1>
        <p className="mt-4 text-muted">
          Your reference code is{' '}
          <span className="font-mono font-bold text-brand-green-deep">{created.code}</span>.
          Keep it handy — you can share it with anyone following the fix.
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Button onClick={() => navigate(`/app/reports/${created.id}`)}>
            Track this report
          </Button>
          <Button variant="outline" onClick={() => navigate('/app')}>
            Back to dashboard
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold">Report an issue</h1>
      <p className="mt-1 text-muted">Step {step + 1} of 4 — {STEPS[step]}</p>

      <ol className="mt-6 flex gap-2" aria-label="Progress">
        {STEPS.map((label, i) => (
          <li key={label} className="flex-1">
            <div
              className={`h-1.5 rounded-full ${i <= step ? 'bg-brand-green' : 'bg-line'}`}
              aria-hidden="true"
            />
            <span className="mt-1 block text-xs text-muted">{label}</span>
          </li>
        ))}
      </ol>

      <Card className="mt-6">
        {submitError && (
          <p role="alert" className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error">
            {submitError}
          </p>
        )}

        {step === 0 && (
          <div className="space-y-4">
            {catError && (
              <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error">
                {catError}
              </p>
            )}
            <div>
              <label htmlFor="category" className="mb-1 block text-sm font-medium">
                What kind of issue is this?
              </label>
              <select
                id="category"
                value={draft.categoryId}
                onChange={(e) => patch({ categoryId: e.target.value })}
                className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              >
                <option value="">Choose a category…</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        )}

        {step === 1 && (
          <div className="space-y-4">
            <Field
              label="Title"
              required
              hint="A short, clear summary of the problem."
              value={draft.title}
              onChange={(e) => patch({ title: e.target.value })}
            />
            <TextAreaField
              label="Description (optional)"
              hint="Anything that helps the response team understand the issue."
              value={draft.description}
              onChange={(e) => patch({ description: e.target.value })}
            />
          </div>
        )}

        {step === 2 && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3">
              <Button onClick={useMyLocation} disabled={locBusy} variant="outline">
                {locBusy ? 'Locating…' : 'Use my location'}
              </Button>
              <span className="text-sm text-muted">or enter coordinates manually</span>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                label="Latitude"
                type="number"
                step="any"
                value={draft.location ? String(draft.location.latitude) : ''}
                onChange={(e) =>
                  patch({
                    location: {
                      latitude: Number(e.target.value),
                      longitude: draft.location?.longitude ?? 0,
                      srid: 4326,
                    },
                  })
                }
              />
              <Field
                label="Longitude"
                type="number"
                step="any"
                value={draft.location ? String(draft.location.longitude) : ''}
                onChange={(e) =>
                  patch({
                    location: {
                      latitude: draft.location?.latitude ?? 0,
                      longitude: Number(e.target.value),
                      srid: 4326,
                    },
                  })
                }
              />
            </div>
            <div>
              <span className="mb-1 block text-sm font-medium">Photo (optional)</span>
              <input
                ref={fileRef}
                type="file"
                accept={ACCEPTED_IMAGE_TYPES.join(',')}
                aria-label="Attach a photo"
                onChange={(e) => onPickFile(e.target.files?.[0] ?? null)}
                className="block w-full text-sm text-muted file:mr-3 file:rounded-lg file:border-0 file:bg-canvas file:px-4 file:py-2 file:text-sm file:font-semibold file:text-brand-black"
              />
              {photo && (
                <p className="mt-1 text-sm text-muted">
                  Attached: {photo.name} ({(photo.size / 1024).toFixed(0)} KB)
                </p>
              )}
              {photoError && (
                <p role="alert" className="mt-1 text-sm font-medium text-error">
                  {photoError}
                </p>
              )}
            </div>
          </div>
        )}

        {step === 3 && (
          <dl className="space-y-3 text-sm">
            <div>
              <dt className="text-muted">Category</dt>
              <dd className="font-medium">
                {categories.find((c) => c.id === draft.categoryId)?.name ?? '—'}
              </dd>
            </div>
            <div>
              <dt className="text-muted">Title</dt>
              <dd className="font-medium">{draft.title || '—'}</dd>
            </div>
            {draft.description && (
              <div>
                <dt className="text-muted">Description</dt>
                <dd className="font-medium">{draft.description}</dd>
              </div>
            )}
            <div>
              <dt className="text-muted">Location</dt>
              <dd className="font-medium">
                {draft.location
                  ? `${draft.location.latitude}, ${draft.location.longitude}`
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-muted">Photo</dt>
              <dd className="font-medium">{photo ? photo.name : 'None'}</dd>
            </div>
          </dl>
        )}

        <div className="mt-6 flex justify-between">
          <Button
            variant="outline"
            onClick={() => patch({ step: Math.max(0, step - 1) })}
            disabled={step === 0 || busy}
          >
            Back
          </Button>
          {step < 3 ? (
            <Button onClick={() => patch({ step: step + 1 })} disabled={!canNext}>
              Next
            </Button>
          ) : (
            <Button onClick={() => void submitReport()} disabled={busy}>
              {busy ? 'Submitting…' : 'Submit report'}
            </Button>
          )}
        </div>
      </Card>
    </div>
  );
}
