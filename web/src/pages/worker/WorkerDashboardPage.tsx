import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Badge,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  type Tone,
} from '../../design-system';
import { api, isApiError } from '../../lib/api';
import { formatDate } from '../../lib/status';
import type { Assignment, AssignmentStatus } from '../../lib/types';

const ASSIGNMENT_META: Record<AssignmentStatus, { label: string; tone: Tone }> = {
  PENDING: { label: 'New', tone: 'info' },
  IN_PROGRESS: { label: 'In progress', tone: 'warning' },
  COMPLETED: { label: 'Completed', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'neutral' },
};

const SECTIONS: { status: AssignmentStatus; title: string; empty: string }[] = [
  { status: 'PENDING', title: 'New assignments', empty: 'No new assignments.' },
  { status: 'IN_PROGRESS', title: 'Work in progress', empty: 'No tasks in progress.' },
  { status: 'COMPLETED', title: 'Completed tasks', empty: 'No completed tasks yet.' },
];

function AssignmentCard({ assignment }: { assignment: Assignment }) {
  const meta = ASSIGNMENT_META[assignment.status];
  return (
    <li className="rounded-card border border-line bg-surface p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link
            to={`/worker/report/${assignment.reportId}`}
            className="font-medium text-brand-green-deep hover:underline"
          >
            {assignment.reportTitle}
          </Link>
          <p className="text-sm text-muted">
            {assignment.reportCode} · Assigned {formatDate(assignment.assignedAt)}
            {assignment.completedAt && ` · Done ${formatDate(assignment.completedAt)}`}
          </p>
        </div>
        <Badge tone={meta.tone}>{meta.label}</Badge>
      </div>
      <Link
        to={`/worker/report/${assignment.reportId}`}
        className="mt-2 inline-block text-sm font-medium text-brand-green-deep hover:underline"
      >
        Open task →
      </Link>
    </li>
  );
}

export function WorkerDashboardPage() {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setAssignments(await api.getMyAssignments());
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not load your tasks.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const countFor = (status: AssignmentStatus) =>
    assignments.filter((a) => a.status === status).length;

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold">My tasks</h1>
          <p className="mt-1 text-sm text-muted">
            Work the reports assigned to you. Completion claims need at least one photo.
          </p>
        </div>
        <Link to="/explore">
          <Badge tone="info">Open map</Badge>
        </Link>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        <Card>
          <p className="text-sm text-muted">New</p>
          <p className="text-2xl font-bold">{countFor('PENDING')}</p>
        </Card>
        <Card>
          <p className="text-sm text-muted">In progress</p>
          <p className="text-2xl font-bold">{countFor('IN_PROGRESS')}</p>
        </Card>
        <Card>
          <p className="text-sm text-muted">Completed</p>
          <p className="text-2xl font-bold">{countFor('COMPLETED')}</p>
        </Card>
      </div>

      {loading && <LoadingState label="Loading your tasks…" />}
      {error && <ErrorState message={error} onRetry={() => void load()} />}

      {!loading && !error && assignments.length === 0 && (
        <div className="mt-6">
          <EmptyState
            title="No assignments yet"
            description="When a moderator or organisation admin assigns a report to you, it will appear here."
          />
        </div>
      )}

      {!loading && !error &&
        assignments.length > 0 &&
        SECTIONS.map((section) => {
          const items = assignments.filter((a) => a.status === section.status);
          return (
            <section key={section.status} className="mt-8" aria-labelledby={`sec-${section.status}`}>
              <h2 id={`sec-${section.status}`} className="text-xl font-bold">
                {section.title}{' '}
                <span className="text-sm font-normal text-muted">({items.length})</span>
              </h2>
              {items.length === 0 ? (
                <p className="mt-2 text-sm text-muted">{section.empty}</p>
              ) : (
                <ul className="mt-3 space-y-3">
                  {items.map((a) => (
                    <AssignmentCard key={a.id} assignment={a} />
                  ))}
                </ul>
              )}
            </section>
          );
        })}
    </div>
  );
}
