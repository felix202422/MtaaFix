import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, isApiError } from '../../lib/api';
import type { Organisation, OrganisationMember, Report } from '../../lib/types';
import {
  Badge,
  Button,
  Card,
  ErrorState,
  Field,
  LoadingState,
  TextAreaField,
  useToast,
} from '../../design-system';
import { statusLabel, statusTone, formatDate } from '../../lib/status';
import { AdminPageShell } from './AdminPageShell';
import { ReportMap } from '../../components/map/ReportMap';

const REJECT_REASONS = ['NOT_RELEVANT', 'DUPLICATE_REPORT', 'INSUFFICIENT_EVIDENCE', 'OTHER'];

export function AdminReportDetailsPage() {
  const { id = '' } = useParams();
  const { push } = useToast();
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [needsInfoText, setNeedsInfoText] = useState('');
  const [rejectReason, setRejectReason] = useState(REJECT_REASONS[0]);
  const [rejectComment, setRejectComment] = useState('');
  const [duplicateRef, setDuplicateRef] = useState('');
  const [reopenJustification, setReopenJustification] = useState('');
  const [commentText, setCommentText] = useState('');

  const [organisations, setOrganisations] = useState<Organisation[]>([]);
  const [selectedOrg, setSelectedOrg] = useState('');
  const [members, setMembers] = useState<OrganisationMember[]>([]);
  const [selectedWorker, setSelectedWorker] = useState('');

  const load = useCallback(() => {
    api
      .getReport(id)
      .then(setReport)
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load the report.'));
  }, [id]);

  useEffect(() => {
    load();
    api
      .listOrganisations()
      .then(setOrganisations)
      .catch(() => setOrganisations([]));
  }, [load]);

  useEffect(() => {
    if (!selectedOrg) {
      setMembers([]);
      return;
    }
    api
      .listOrganisationMembers(selectedOrg)
      .then(setMembers)
      .catch(() => setMembers([]));
  }, [selectedOrg]);

  async function run(action: () => Promise<Report>, successMessage: string) {
    setBusy(true);
    try {
      const updated = await action();
      setReport(updated);
      push(successMessage, 'success');
    } catch (err) {
      push(isApiError(err) ? err.message : 'The action failed.', 'error');
    } finally {
      setBusy(false);
    }
  }

  return (
    <AdminPageShell
      title={report ? `${report.code} — ${report.title}` : 'Report details'}
      description="Full authorized details: evidence, map, history, decisions and permitted actions (P16)."
      actions={
        <Link to="/admin/reports" className="text-sm font-medium text-brand-blue hover:underline">
          ← Back to reports
        </Link>
      }
    >
      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !report && <LoadingState label="Loading report…" />}
      {report && (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="flex flex-col gap-6 lg:col-span-2">
            <Card aria-label="Report summary">
              <div className="mb-3 flex flex-wrap items-center gap-3">
                <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
                <span className="text-sm text-muted">Reported {formatDate(report.reportedAt)}</span>
                {report.score !== null && (
                  <span className="text-sm text-muted">Priority score {report.score}</span>
                )}
              </div>
              <h2 className="text-lg font-semibold text-brand-black">{report.title}</h2>
              <p className="mt-2 whitespace-pre-wrap text-sm text-brand-black">
                {report.description ?? 'No description provided.'}
              </p>
              {report.location && (
                <div className="mt-4">
                  <ReportMap
                    points={[
                      {
                        code: report.code,
                        title: report.title,
                        latitude: report.location.latitude,
                        longitude: report.location.longitude,
                      },
                    ]}
                    height="14rem"
                  />
                </div>
              )}
            </Card>

            <Card aria-label="Evidence">
              <h2 className="mb-3 text-base font-semibold text-brand-black">Evidence</h2>
              {report.media.length === 0 ? (
                <p className="text-sm text-muted">No media attached.</p>
              ) : (
                <ul className="flex flex-col gap-2 text-sm">
                  {report.media.map((media) => (
                    <li key={media.id} className="flex items-center justify-between gap-3">
                      <span>
                        {media.originalName}{' '}
                        <span className="text-muted">({media.contentType})</span>
                      </span>
                      <a
                        href={api.getMediaUrl(media.id)}
                        target="_blank"
                        rel="noreferrer"
                        className="font-medium text-brand-blue hover:underline"
                      >
                        View
                      </a>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card aria-label="Status history">
              <h2 className="mb-3 text-base font-semibold text-brand-black">Status history</h2>
              {report.statusHistory.length === 0 ? (
                <p className="text-sm text-muted">No transitions recorded.</p>
              ) : (
                <ol className="flex flex-col gap-3 text-sm">
                  {report.statusHistory.map((entry, index) => (
                    <li key={`${entry.toStatus}-${index}`} className="border-l-2 border-line pl-3">
                      <p className="font-medium text-brand-black">
                        {statusLabel(entry.fromStatus ?? ('—' as never))}
                        {' → '}
                        {statusLabel(entry.toStatus)}
                      </p>
                      <p className="text-muted">{formatDate(entry.timestamp)}</p>
                      {entry.comment && <p className="text-brand-black">{entry.comment}</p>}
                      {entry.reasonCode && <p className="text-muted">Reason: {entry.reasonCode}</p>}
                    </li>
                  ))}
                </ol>
              )}
            </Card>

            <Card aria-label="Comments and notes">
              <h2 className="mb-3 text-base font-semibold text-brand-black">
                Comments and internal notes
              </h2>
              {report.comments.length === 0 ? (
                <p className="text-sm text-muted">No comments yet.</p>
              ) : (
                <ul className="mb-4 flex flex-col gap-3 text-sm">
                  {report.comments.map((comment) => (
                    <li key={comment.id} className="rounded-lg bg-canvas p-3">
                      <p className="whitespace-pre-wrap">{comment.content}</p>
                      <p className="mt-1 text-xs text-muted">{formatDate(comment.timestamp)}</p>
                    </li>
                  ))}
                </ul>
              )}
              <form
                className="flex flex-col gap-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  const content = commentText.trim();
                  if (!content) return;
                  void run(() => api.addComment(report.id, content), 'Comment added.');
                  setCommentText('');
                }}
              >
                <TextAreaField
                  label="Add a comment"
                  placeholder="Visible to staff and the report creator"
                  value={commentText}
                  onChange={(event) => setCommentText(event.target.value)}
                />
                <Button type="submit" size="sm" variant="secondary" disabled={busy}>
                  Post comment
                </Button>
              </form>
            </Card>
          </div>

          <div className="flex flex-col gap-6">
            <Card aria-label="Moderation actions">
              <h2 className="mb-3 text-base font-semibold text-brand-black">
                Moderation decisions
              </h2>
              <div className="flex flex-col gap-4 text-sm">
                <div>
                  <Button
                    size="sm"
                    disabled={busy}
                    onClick={() =>
                      void run(
                        () =>
                          api.reportNeedsInfo(
                            report.id,
                            needsInfoText.trim() || 'Please provide more detail.',
                          ),
                        'Information requested.',
                      )
                    }
                  >
                    Request information
                  </Button>
                  <Field
                    label="Clarification message"
                    className="mt-2"
                    value={needsInfoText}
                    onChange={(event) => setNeedsInfoText(event.target.value)}
                  />
                </div>
                <Button
                  size="sm"
                  disabled={busy}
                  onClick={() => void run(() => api.reportVerify(report.id), 'Report verified.')}
                >
                  Verify
                </Button>
                <div className="flex flex-col gap-2">
                  <label htmlFor="reject-reason" className="font-medium">
                    Reject
                  </label>
                  <select
                    id="reject-reason"
                    className="rounded-lg border border-line bg-surface px-3 py-2"
                    value={rejectReason}
                    onChange={(event) => setRejectReason(event.target.value)}
                  >
                    {REJECT_REASONS.map((reason) => (
                      <option key={reason} value={reason}>
                        {reason}
                      </option>
                    ))}
                  </select>
                  <TextAreaField
                    label="Rejection comment"
                    value={rejectComment}
                    onChange={(event) => setRejectComment(event.target.value)}
                  />
                  <Button
                    size="sm"
                    variant="danger"
                    disabled={busy || !rejectComment.trim()}
                    onClick={() =>
                      void run(
                        () => api.reportReject(report.id, { reasonCode: rejectReason, comment: rejectComment.trim() }),
                        'Report rejected.',
                      )
                    }
                  >
                    Reject report
                  </Button>
                </div>
                <div className="flex flex-col gap-2">
                  <Field
                    label="Mark duplicate of (reference)"
                    placeholder="MTF-…"
                    value={duplicateRef}
                    onChange={(event) => setDuplicateRef(event.target.value)}
                  />
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={busy || !duplicateRef.trim()}
                    onClick={() =>
                      void run(
                        () => api.reportDuplicate(report.id, duplicateRef.trim()),
                        'Marked as duplicate.',
                      )
                    }
                  >
                    Mark duplicate
                  </Button>
                </div>
              </div>
            </Card>

            <Card aria-label="Assignment">
              <h2 className="mb-3 text-base font-semibold text-brand-black">Assignment</h2>
              <div className="flex flex-col gap-3 text-sm">
                <div>
                  <label htmlFor="assign-org" className="mb-1 block font-medium">
                    Organisation
                  </label>
                  <select
                    id="assign-org"
                    className="w-full rounded-lg border border-line bg-surface px-3 py-2"
                    value={selectedOrg}
                    onChange={(event) => setSelectedOrg(event.target.value)}
                  >
                    <option value="">Select an organisation…</option>
                    {organisations.map((org) => (
                      <option key={org.id} value={org.id}>
                        {org.name} ({org.memberCount} members)
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="assign-worker" className="mb-1 block font-medium">
                    Field worker
                  </label>
                  <select
                    id="assign-worker"
                    className="w-full rounded-lg border border-line bg-surface px-3 py-2"
                    value={selectedWorker}
                    onChange={(event) => setSelectedWorker(event.target.value)}
                  >
                    <option value="">Select a worker…</option>
                    {members
                      .filter((member) => member.role === 'FIELD_WORKER')
                      .map((member) => (
                        <option key={member.userId} value={member.userId}>
                          {member.fullName} ({member.email})
                        </option>
                      ))}
                  </select>
                </div>
                <Button
                  size="sm"
                  disabled={busy || !selectedOrg || !selectedWorker}
                  onClick={() =>
                    void run(
                      () =>
                        api.reportAssign(report.id, {
                          assigneeId: selectedWorker,
                          organisationId: selectedOrg,
                        }),
                      'Report assigned.',
                    )
                  }
                >
                  Assign
                </Button>
                {report.assigneeId && (
                  <p className="text-muted">Currently assigned to worker {report.assigneeId}.</p>
                )}
              </div>
            </Card>

            <Card aria-label="Workflow actions">
              <h2 className="mb-3 text-base font-semibold text-brand-black">Workflow</h2>
              <div className="flex flex-col gap-2 text-sm">
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={busy}
                  onClick={() => void run(() => api.reportStart(report.id), 'Work started.')}
                >
                  Start work
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={busy}
                  onClick={() =>
                    void run(() => api.reportResolve(report.id), 'Completion approved.')
                  }
                >
                  Approve completion (resolve)
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={busy}
                  onClick={() => void run(() => api.reportClose(report.id), 'Report closed.')}
                >
                  Close report
                </Button>
                <div className="flex flex-col gap-2">
                  <TextAreaField
                    label="Reopen justification"
                    value={reopenJustification}
                    onChange={(event) => setReopenJustification(event.target.value)}
                  />
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={busy || !reopenJustification.trim()}
                    onClick={() =>
                      void run(
                        () => api.reportReopen(report.id, reopenJustification.trim()),
                        'Report reopened.',
                      )
                    }
                  >
                    Reopen
                  </Button>
                </div>
                <p className="text-xs text-muted">
                  Actions are authorized server-side per the role matrix; a 403 means this role
                  may not perform the action in the report&apos;s current state.
                </p>
              </div>
            </Card>
          </div>
        </div>
      )}
    </AdminPageShell>
  );
}
