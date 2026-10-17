import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, isApiError } from '../../lib/api';
import type { Report } from '../../lib/types';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Field,
  LoadingState,
  Modal,
  TextAreaField,
  useToast,
} from '../../design-system';
import { statusLabel, statusTone, formatDate } from '../../lib/status';
import { AdminPageShell } from './AdminPageShell';

const QUEUE_TABS = [
  { key: 'SUBMITTED', label: 'Submitted' },
  { key: 'UNDER_REVIEW', label: 'Under review' },
  { key: 'NEEDS_INFORMATION', label: 'Needs information' },
  { key: 'RESOLUTION_REVIEW', label: 'Resolution review' },
] as const;

type QueueKey = (typeof QUEUE_TABS)[number]['key'];

const REJECT_REASONS = ['NOT_RELEVANT', 'DUPLICATE_REPORT', 'INSUFFICIENT_EVIDENCE', 'OTHER'];

export function AdminModerationPage() {
  const { push } = useToast();
  const [tab, setTab] = useState<QueueKey>('SUBMITTED');
  const [reports, setReports] = useState<Report[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const [needsInfoTarget, setNeedsInfoTarget] = useState<Report | null>(null);
  const [needsInfoText, setNeedsInfoText] = useState('');
  const [rejectTarget, setRejectTarget] = useState<Report | null>(null);
  const [rejectReason, setRejectReason] = useState(REJECT_REASONS[0]);
  const [rejectComment, setRejectComment] = useState('');
  const [duplicateTarget, setDuplicateTarget] = useState<Report | null>(null);
  const [duplicateRef, setDuplicateRef] = useState('');

  const load = useCallback(() => {
    setReports(null);
    setError(null);
    api
      .listStaffReports({ status: tab, size: 50 })
      .then((page) => setReports(page.content))
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load the queue.'));
  }, [tab]);

  useEffect(() => {
    load();
  }, [load]);

  async function act(report: Report, action: () => Promise<Report>, message: string) {
    setBusyId(report.id);
    try {
      await action();
      push(message, 'success');
      load();
    } catch (err) {
      push(isApiError(err) ? err.message : 'The action failed.', 'error');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <AdminPageShell
      title="Moderation and review"
      description="Review queue with evidence, verification, rejection reasons, clarification requests and duplicate handling (P17)."
    >
      <div role="tablist" aria-label="Queue status" className="flex flex-wrap gap-2">
        {QUEUE_TABS.map((queue) => (
          <button
            key={queue.key}
            type="button"
            role="tab"
            aria-selected={tab === queue.key}
            onClick={() => setTab(queue.key)}
            className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
              tab === queue.key
                ? 'bg-brand-green-deep text-white'
                : 'border border-line bg-surface text-muted hover:text-brand-green-deep'
            }`}
          >
            {queue.label}
          </button>
        ))}
      </div>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !reports && <LoadingState label="Loading queue…" />}
      {reports && reports.length === 0 && (
        <EmptyState
          title="Nothing in this queue."
          description="Reports appear here as soon as citizens submit them or request resolution review."
        />
      )}
      {reports && reports.length > 0 && (
        <div className="flex flex-col gap-4">
          {reports.map((report) => (
            <Card key={report.id} aria-label={`Queue item ${report.code}`}>
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <Link
                      to={`/admin/reports/${report.id}`}
                      className="font-semibold text-brand-blue hover:underline"
                    >
                      {report.code}
                    </Link>
                    <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
                    <span className="text-xs text-muted">{formatDate(report.reportedAt)}</span>
                  </div>
                  <h2 className="mt-1 text-base font-semibold text-brand-black">{report.title}</h2>
                  <p className="mt-1 text-sm text-muted">
                    {report.description ?? 'No description.'}
                  </p>
                  {report.media.length > 0 && (
                    <p className="mt-1 text-xs text-muted">
                      {report.media.length} evidence file(s) —{' '}
                      <Link
                        to={`/admin/reports/${report.id}`}
                        className="text-brand-blue hover:underline"
                      >
                        preview on details page
                      </Link>
                    </p>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  {report.status === 'SUBMITTED' && (
                    <Button
                      size="sm"
                      disabled={busyId === report.id}
                      onClick={() =>
                        void act(
                          report,
                          () => api.submitReport(report.id),
                          'Claimed for review.',
                        )
                      }
                    >
                      Claim for review
                    </Button>
                  )}
                  {(report.status === 'SUBMITTED' || report.status === 'UNDER_REVIEW') && (
                    <Button
                      size="sm"
                      variant="secondary"
                      disabled={busyId === report.id}
                      onClick={() =>
                        void act(report, () => api.reportVerify(report.id), 'Verified.')
                      }
                    >
                      Verify
                    </Button>
                  )}
                  {report.status === 'UNDER_REVIEW' && (
                    <>
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={busyId === report.id}
                        onClick={() => {
                          setNeedsInfoTarget(report);
                          setNeedsInfoText('');
                        }}
                      >
                        Needs info
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        disabled={busyId === report.id}
                        onClick={() => {
                          setRejectTarget(report);
                          setRejectComment('');
                        }}
                      >
                        Reject
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={busyId === report.id}
                        onClick={() => {
                          setDuplicateTarget(report);
                          setDuplicateRef('');
                        }}
                      >
                        Duplicate
                      </Button>
                    </>
                  )}
                  {report.status === 'RESOLUTION_REVIEW' && (
                    <Button
                      size="sm"
                      disabled={busyId === report.id}
                      onClick={() =>
                        void act(report, () => api.reportResolve(report.id), 'Completion approved.')
                      }
                    >
                      Approve completion
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={needsInfoTarget !== null}
        title={needsInfoTarget ? `Request information — ${needsInfoTarget.code}` : ''}
        onClose={() => setNeedsInfoTarget(null)}
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setNeedsInfoTarget(null)}>
              Cancel
            </Button>
            <Button
              size="sm"
              disabled={!needsInfoText.trim()}
              onClick={() => {
                const target = needsInfoTarget;
                if (!target) return;
                void act(
                  target,
                  () => api.reportNeedsInfo(target.id, needsInfoText.trim()),
                  'Information requested.',
                ).then(() => setNeedsInfoTarget(null));
              }}
            >
              Send
            </Button>
          </>
        }
      >
        <TextAreaField
          label="Clarification message"
          value={needsInfoText}
          onChange={(event) => setNeedsInfoText(event.target.value)}
        />
      </Modal>

      <Modal
        open={rejectTarget !== null}
        title={rejectTarget ? `Reject — ${rejectTarget.code}` : ''}
        onClose={() => setRejectTarget(null)}
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setRejectTarget(null)}>
              Cancel
            </Button>
            <Button
              size="sm"
              variant="danger"
              disabled={!rejectComment.trim()}
              onClick={() => {
                const target = rejectTarget;
                if (!target) return;
                void act(
                  target,
                  () =>
                    api.reportReject(target.id, {
                      reasonCode: rejectReason,
                      comment: rejectComment.trim(),
                    }),
                  'Report rejected.',
                ).then(() => setRejectTarget(null));
              }}
            >
              Reject
            </Button>
          </>
        }
      >
        <label htmlFor="mod-reject-reason" className="mb-1 block text-sm font-medium">
          Reason code
        </label>
        <select
          id="mod-reject-reason"
          className="mb-3 w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm"
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
          label="Comment"
          value={rejectComment}
          onChange={(event) => setRejectComment(event.target.value)}
        />
      </Modal>

      <Modal
        open={duplicateTarget !== null}
        title={duplicateTarget ? `Mark duplicate — ${duplicateTarget.code}` : ''}
        onClose={() => setDuplicateTarget(null)}
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setDuplicateTarget(null)}>
              Cancel
            </Button>
            <Button
              size="sm"
              disabled={!duplicateRef.trim()}
              onClick={() => {
                const target = duplicateTarget;
                if (!target) return;
                void act(
                  target,
                  () => api.reportDuplicate(target.id, duplicateRef.trim()),
                  'Marked as duplicate.',
                ).then(() => setDuplicateTarget(null));
              }}
            >
              Mark duplicate
            </Button>
          </>
        }
      >
        <Field
          label="Canonical report reference"
          placeholder="MTF-…"
          value={duplicateRef}
          onChange={(event) => setDuplicateRef(event.target.value)}
        />
      </Modal>
    </AdminPageShell>
  );
}
