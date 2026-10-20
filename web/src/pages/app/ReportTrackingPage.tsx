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
import type { Report, ReportMedia, ReportStatus } from '../../lib/types';

function canCancel(s: ReportStatus): boolean {
  return s === 'SUBMITTED';
}

function canConfirm(s: ReportStatus): boolean {
  return s === 'RESOLVED';
}

function canDispute(s: ReportStatus): boolean {
  return s === 'RESOLVED';
}

function canResubmit(s: ReportStatus): boolean {
  return s === 'NEEDS_INFORMATION';
}

const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_PHOTO_BYTES = 20 * 1024 * 1024;
const MAX_MEDIA_PER_REPORT = 10;

export function ReportTrackingPage() {
  const { id } = useParams<{ id: string }>();
  const { push } = useToast();
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [comment, setComment] = useState('');
  const [commentBusy, setCommentBusy] = useState(false);
  const [disputeReason, setDisputeReason] = useState('');
  const [feedback, setFeedback] = useState('');
  const [actionBusy, setActionBusy] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoError, setPhotoError] = useState(null as string | null);
  const [mediaUrls, setMediaUrls] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      setReport(await api.getReport(id));
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not load this report.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  // Fetch photo blobs (streaming requires auth).
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

  async function onComment(e: FormEvent) {
    e.preventDefault();
    if (!id || !comment.trim()) return;
    setCommentBusy(true);
    try {
      const updated = await api.addComment(id, comment.trim());
      setReport(updated);
      setComment('');
      push('Comment posted.', 'success');
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not post comment.', 'error');
    } finally {
      setCommentBusy(false);
    }
  }

  async function onUpload() {
    if (!id || !photo) return;
    setActionBusy(true);
    try {
      const updated = await api.uploadPhoto(id, photo);
      setReport(updated);
      setPhoto(null);
      push('Photo attached.', 'success');
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not upload photo.', 'error');
    } finally {
      setActionBusy(false);
    }
  }

  async function onDeleteMedia(mediaId: string) {
    if (!id) return;
    setActionBusy(true);
    try {
      await api.deleteMedia(mediaId);
      await load();
      push('Photo removed.', 'success');
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not remove photo.', 'error');
    } finally {
      setActionBusy(false);
    }
  }

  if (loading) return <LoadingState label="Loading report…" />;
  if (error) return <ErrorState message={error} onRetry={() => void load()} />;
  if (!report) return <ErrorState message="Report not found." />;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Link to="/app/reports" className="text-sm font-medium text-brand-green-deep hover:underline">
        ← Back to my reports
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold">{report.title}</h1>
          <p className="mt-1 font-mono text-sm text-muted">{report.code}</p>
        </div>
        <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
      </div>

      {report.description && <p className="mt-4 text-fg">{report.description}</p>}

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
          </Card>
        )}
      </div>

      {/* Evidence photos */}
      <section className="mt-8" aria-labelledby="evidence-heading">
        <h2 id="evidence-heading" className="text-xl font-bold">
          Evidence photos
        </h2>
        {report.media.length === 0 ? (
          <p className="mt-2 text-sm text-muted">No photos attached yet.</p>
        ) : (
          <ul className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
            {report.media.map((m: ReportMedia) => (
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
                <div className="mt-2 flex items-center justify-between">
                  <span className="truncate text-xs text-muted">{m.originalName}</span>
                  <button
                    type="button"
                    onClick={() => void onDeleteMedia(m.id)}
                    disabled={actionBusy}
                    className="text-xs font-medium text-error hover:underline disabled:opacity-50"
                  >
                    Remove
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="mt-3 flex flex-wrap items-center gap-3">
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            aria-label="Attach a photo"
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
              } else if (report.media.length >= MAX_MEDIA_PER_REPORT) {
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

      {/* Lifecycle actions */}
      {(canCancel(report.status) ||
        canConfirm(report.status) ||
        canDispute(report.status) ||
        canResubmit(report.status)) && (
        <section className="mt-8" aria-labelledby="actions-heading">
          <h2 id="actions-heading" className="text-xl font-bold">
            Actions
          </h2>
          <div className="mt-3 space-y-4">
            {canCancel(report.status) && (
              <Card>
                <p className="text-sm text-muted">
                  Not the issue you meant to report? You can cancel it while it is still
                  submitted.
                </p>
                <Button
                  variant="danger"
                  className="mt-3"
                  disabled={actionBusy}
                  onClick={() =>
                    void runAction(
                      () => api.cancelReport(report.id),
                      'Report cancelled.',
                    )
                  }
                >
                  Cancel report
                </Button>
              </Card>
            )}

            {canConfirm(report.status) && (
              <Card>
                <p className="font-medium">Confirm the fix</p>
                <p className="mt-1 text-sm text-muted">
                  If the issue is really resolved, confirm it to close the report. Add
                  optional feedback for the team.
                </p>
                <TextAreaField
                  label="Feedback (optional)"
                  className="mt-3"
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                />
                <Button
                  className="mt-3"
                  disabled={actionBusy}
                  onClick={() =>
                    void runAction(
                      () => api.confirmReport(report.id, feedback.trim() || undefined),
                      'Resolution confirmed. Thank you!',
                    )
                  }
                >
                  Confirm resolved
                </Button>
              </Card>
            )}

            {canDispute(report.status) && (
              <Card>
                <p className="font-medium">Something still wrong?</p>
                <p className="mt-1 text-sm text-muted">
                  If the issue is not actually fixed, tell us why and the report will go
                  back for review.
                </p>
                <TextAreaField
                  label="Why is this not resolved?"
                  required
                  className="mt-3"
                  value={disputeReason}
                  onChange={(e) => setDisputeReason(e.target.value)}
                />
                <Button
                  variant="secondary"
                  className="mt-3"
                  disabled={actionBusy || disputeReason.trim().length === 0}
                  onClick={() =>
                    void runAction(
                      () => api.disputeReport(report.id, disputeReason.trim()),
                      'Dispute submitted.',
                    )
                  }
                >
                  Dispute resolution
                </Button>
              </Card>
            )}

            {canResubmit(report.status) && (
              <Card>
                <p className="font-medium">More information needed</p>
                <p className="mt-1 text-sm text-muted">
                  The review team asked for more details. Once you have added them,
                  resubmit the report.
                </p>
                <Button
                  className="mt-3"
                  disabled={actionBusy}
                  onClick={() =>
                    void runAction(
                      () => api.resubmitReport(report.id),
                      'Report resubmitted.',
                    )
                  }
                >
                  Resubmit
                </Button>
              </Card>
            )}
          </div>
        </section>
      )}

      {/* Timeline */}
      <section className="mt-8" aria-labelledby="timeline-heading">
        <h2 id="timeline-heading" className="text-xl font-bold">
          Timeline
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

      {/* Comments */}
      <section className="mt-8" aria-labelledby="comments-heading">
        <h2 id="comments-heading" className="text-xl font-bold">
          Comments
        </h2>
        {report.comments.length === 0 ? (
          <p className="mt-2 text-sm text-muted">No comments yet.</p>
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
        <form onSubmit={(e) => void onComment(e)} className="mt-4" noValidate>
          <TextAreaField
            label="Add a comment"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
          <Button type="submit" size="sm" className="mt-3" disabled={commentBusy || !comment.trim()}>
            {commentBusy ? 'Posting…' : 'Post comment'}
          </Button>
        </form>
      </section>
    </div>
  );
}
