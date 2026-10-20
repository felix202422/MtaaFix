import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Badge,
  Button,
  Card,
  ErrorState,
  LoadingState,
  TextAreaField,
  useToast,
} from '../../design-system';
import { api, isApiError } from '../../lib/api';
import { formatDate, statusLabel, statusTone } from '../../lib/status';
import type { Category, Report, ReportMedia } from '../../lib/types';

const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_PHOTO_BYTES = 20 * 1024 * 1024;
const MAX_MEDIA_PER_REPORT = 10;

export function WorkerTaskDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { push } = useToast();
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionBusy, setActionBusy] = useState(false);
  const [comment, setComment] = useState('');
  const [commentBusy, setCommentBusy] = useState(false);
  const [completionNote, setCompletionNote] = useState('');
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoError, setPhotoError] = useState(null as string | null);
  const [mediaUrls, setMediaUrls] = useState<Record<string, string>>({});
  const [categories, setCategories] = useState<Category[]>([]);

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      setReport(await api.getReport(id));
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not load this task.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    api
      .getCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  // Fetch evidence photos (streaming requires auth).
  useEffect(() => {
    let cancelled = false;
    if (!report) return;
    const urls: Record<string, string> = {};
    const created: string[] = [];
    void (async () => {
      for (const m of report.media) {
        if (m.mediaType !== 'PHOTO') continue;
        try {
          const blob = await api.fetchMediaBlob(m.id);
          if (cancelled) return;
          const url = URL.createObjectURL(blob);
          urls[m.id] = url;
          created.push(url);
        } catch {
          // Skip images that fail to load.
        }
      }
      if (!cancelled) setMediaUrls(urls);
    })();
    return () => {
      cancelled = true;
      created.forEach((u) => URL.revokeObjectURL(u));
    };
  }, [report]);

  async function runAction(action: () => Promise<Report>, successMsg: string) {
    setActionBusy(true);
    try {
      const updated = await action();
      setReport(updated);
      push(successMsg, 'success');
    } catch (err) {
      push(isApiError(err) ? err.message : 'Action failed.', 'error');
    } finally {
      setActionBusy(false);
    }
  }

  async function onUpload() {
    if (!id || !photo) return;
    setActionBusy(true);
    try {
      const updated = await api.uploadPhoto(id, photo);
      setReport(updated);
      setPhoto(null);
      push('Evidence photo attached.', 'success');
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not upload photo.', 'error');
    } finally {
      setActionBusy(false);
    }
  }

  async function onProgress(e: FormEvent) {
    e.preventDefault();
    if (!id || !comment.trim()) return;
    setCommentBusy(true);
    try {
      const updated = await api.addComment(id, comment.trim());
      setReport(updated);
      setComment('');
      push('Progress update added.', 'success');
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not add the update.', 'error');
    } finally {
      setCommentBusy(false);
    }
  }

  if (loading) return <LoadingState label="Loading task…" />;
  if (error) return <ErrorState message={error} onRetry={() => void load()} />;
  if (!report) return <ErrorState message="Task not found." />;

  const categoryName =
    categories.find((c) => c.id === report.categoryId)?.name ?? null;
  const hasPhoto = report.media.some((m: ReportMedia) => m.mediaType === 'PHOTO');

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Link to="/worker" className="text-sm font-medium text-brand-green-deep hover:underline">
        ← Back to my tasks
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold">{report.title}</h1>
          <p className="mt-1 font-mono text-sm text-muted">{report.code}</p>
        </div>
        <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
      </div>

      <section className="mt-6" aria-labelledby="instructions-heading">
        <h2 id="instructions-heading" className="text-xl font-bold">
          Task and instructions
        </h2>
        <Card className="mt-3">
          {categoryName && <p className="text-sm text-muted">Category: {categoryName}</p>}
          <p className="mt-1 whitespace-pre-wrap text-sm text-fg">
            {report.description ?? 'No description provided.'}
          </p>
        </Card>
      </section>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <Card>
          <p className="text-sm text-muted">Reported</p>
          <p className="font-medium">{formatDate(report.reportedAt)}</p>
        </Card>
        {report.location && (
          <Card>
            <p className="text-sm text-muted">Location</p>
            <p className="font-medium">
              {report.location.latitude}, {report.location.longitude}
            </p>
            <Link
              to="/explore"
              className="mt-1 inline-block text-sm font-medium text-brand-green-deep hover:underline"
            >
              View on map →
            </Link>
          </Card>
        )}
      </div>

      {/* Evidence */}
      <section className="mt-8" aria-labelledby="evidence-heading">
        <h2 id="evidence-heading" className="text-xl font-bold">
          Evidence
        </h2>
        <p className="mt-1 text-sm text-muted">
          A completion claim requires at least one attached photo.
        </p>
        {report.media.length === 0 ? (
          <p className="mt-2 text-sm text-muted">No photos attached yet.</p>
        ) : (
          <ul className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
            {report.media.map((m) => (
              <li key={m.id} className="rounded-card border border-line bg-surface p-2">
                {mediaUrls[m.id] ? (
                  <img
                    src={mediaUrls[m.id]}
                    alt={m.originalName}
                    className="h-28 w-full rounded object-cover"
                  />
                ) : (
                  <div className="flex h-28 items-center justify-center rounded bg-canvas text-xs text-muted">
                    {m.originalName}
                  </div>
                )}
                <p className="mt-2 truncate text-xs text-muted">{m.originalName}</p>
              </li>
            ))}
          </ul>
        )}
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            aria-label="Attach completion evidence"
            onChange={(e) => {
              const f = e.target.files?.[0] ?? null;
              setPhotoError(null);
              if (!f) {
                setPhoto(null);
              } else if (!ACCEPTED_IMAGE_TYPES.includes(f.type)) {
                setPhotoError('Only JPEG, PNG, or WEBP photos are allowed.');
                setPhoto(null);
              } else if (f.size > MAX_PHOTO_BYTES) {
                setPhotoError('Photo must be 20 MB or smaller.');
                setPhoto(null);
              } else if ((report?.media.length ?? 0) >= MAX_MEDIA_PER_REPORT) {
                setPhotoError('This report already has 10 photos.');
                setPhoto(null);
              } else {
                setPhoto(f);
              }
              e.target.value = '';
            }}
            className="block text-sm text-muted file:mr-3 file:rounded-lg file:border-0 file:bg-canvas file:px-4 file:py-2 file:text-sm file:font-semibold file:text-brand-black"
          />
          <Button onClick={() => void onUpload()} disabled={!photo || actionBusy} size="sm">
            {actionBusy ? 'Uploading…' : 'Attach photo'}
          </Button>
        </div>
        {photoError && (
          <p role="alert" className="mt-1 text-sm font-medium text-error">
            {photoError}
          </p>
        )}
      </section>

      {/* Task actions — worker-permitted transitions only */}
      <section className="mt-8" aria-labelledby="actions-heading">
        <h2 id="actions-heading" className="text-xl font-bold">
          Actions
        </h2>
        <div className="mt-3 space-y-4">
          {report.status === 'ASSIGNED' && (
            <Card>
              <p className="font-medium">Ready to start?</p>
              <p className="mt-1 text-sm text-muted">
                Starting moves the report to in progress and notifies the citizen.
              </p>
              <Button
                className="mt-3"
                disabled={actionBusy}
                onClick={() =>
                  void runAction(() => api.reportStart(report.id), 'Work started.')
                }
              >
                Start work
              </Button>
            </Card>
          )}

          {report.status === 'IN_PROGRESS' && (
            <Card>
              <p className="font-medium">Submit completion claim</p>
              <p className="mt-1 text-sm text-muted">
                Work done? Attach at least one photo above, add a note, and submit. A reviewer
                must approve before the report is resolved.
              </p>
              <TextAreaField
                label="Completion note"
                className="mt-3"
                value={completionNote}
                onChange={(e) => setCompletionNote(e.target.value)}
              />
              <Button
                className="mt-3"
                disabled={actionBusy}
                onClick={() =>
                  void runAction(
                    () => api.reportComplete(report.id, completionNote.trim()),
                    'Completion claim submitted for review.',
                  )
                }
              >
                Submit completion
              </Button>
              {!hasPhoto && (
                <p className="mt-2 text-xs font-medium text-error">
                  No photo attached yet — the claim will be rejected until you add one.
                </p>
              )}
            </Card>
          )}

          {report.status === 'RESOLUTION_REVIEW' && (
            <Card>
              <p className="font-medium">Completion claim submitted</p>
              <p className="mt-1 text-sm text-muted">
                A reviewer is checking your claim. They can approve it or return it to you for
                more work — no action is needed from you right now.
              </p>
            </Card>
          )}

          {!['ASSIGNED', 'IN_PROGRESS', 'RESOLUTION_REVIEW'].includes(report.status) && (
            <p className="text-sm text-muted">
              No worker actions are available while the report is{' '}
              {statusLabel(report.status).toLowerCase()}.
            </p>
          )}
        </div>
      </section>

      {/* Progress updates */}
      <section className="mt-8" aria-labelledby="progress-heading">
        <h2 id="progress-heading" className="text-xl font-bold">
          Progress updates
        </h2>
        {report.comments.length === 0 ? (
          <p className="mt-2 text-sm text-muted">No updates yet.</p>
        ) : (
          <ul className="mt-3 space-y-3">
            {report.comments.map((c) => (
              <li key={c.id} className="rounded-card border border-line bg-surface px-4 py-3">
                <p className="text-sm text-fg">{c.content}</p>
                <p className="mt-1 text-xs text-muted">{formatDate(c.timestamp)}</p>
              </li>
            ))}
          </ul>
        )}
        <form onSubmit={(e) => void onProgress(e)} className="mt-4" noValidate>
          <TextAreaField
            label="Add a progress update"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
          <Button
            type="submit"
            size="sm"
            className="mt-3"
            disabled={commentBusy || !comment.trim()}
          >
            {commentBusy ? 'Posting…' : 'Post update'}
          </Button>
        </form>
      </section>

      {/* Status history */}
      <section className="mt-8" aria-labelledby="history-heading">
        <h2 id="history-heading" className="text-xl font-bold">
          Status history
        </h2>
        <ol className="mt-3 space-y-3">
          {report.statusHistory.map((h, i) => (
            <li key={i} className="rounded-card border border-line bg-surface px-4 py-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Badge tone={statusTone(h.toStatus)}>{statusLabel(h.toStatus)}</Badge>
                <span className="text-xs text-muted">{formatDate(h.timestamp)}</span>
              </div>
              {h.comment && <p className="mt-2 text-sm text-fg">{h.comment}</p>}
              {h.reasonCode && (
                <p className="mt-1 text-xs text-muted">Reason: {h.reasonCode}</p>
              )}
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}
