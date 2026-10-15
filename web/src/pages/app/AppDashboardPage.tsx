import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, EmptyState, ErrorState, LoadingState, Badge } from '../../design-system';
import { api, isApiError } from '../../lib/api';
import { statusLabel, statusTone } from '../../lib/status';
import type { Dashboard } from '../../lib/types';

export function AppDashboardPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setDashboard(await api.getDashboard());
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not load your dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) return <LoadingState label="Loading your dashboard…" />;
  if (error) return <ErrorState message={error} onRetry={() => void load()} />;

  if (!dashboard) {
    return (
      <EmptyState
        title="Nothing here yet"
        description="Once you report an issue it will show up here."
        action={
          <Link to="/app/reports/new">
            <Button>Report an issue</Button>
          </Link>
        }
      />
    );
  }

  const stats = [
    { label: 'Total reports', value: dashboard.totalReports },
    { label: 'Submitted', value: dashboard.submittedReports },
    { label: 'Verified', value: dashboard.verifiedReports },
    { label: 'Closed', value: dashboard.closedReports },
  ];

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold">Hello, {dashboard.name}</h1>
          <p className="mt-1 text-muted">Here is where your reports stand today.</p>
        </div>
        <Link to="/app/reports/new">
          <Button>Report an issue</Button>
        </Link>
      </div>

      <div className="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-4">
        {stats.map((s) => (
          <Card key={s.label}>
            <p className="text-sm text-muted">{s.label}</p>
            <p className="mt-1 text-3xl font-extrabold text-brand-green-deep">{s.value}</p>
          </Card>
        ))}
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <Card>
          <p className="text-sm text-muted">Need your input</p>
          <p className="mt-1 text-3xl font-extrabold text-amber-600">{dashboard.needsInfoCount}</p>
          <p className="mt-1 text-sm text-muted">
            Reports waiting on more information from you.
          </p>
        </Card>
        <Card>
          <p className="text-sm text-muted">Awaiting your confirmation</p>
          <p className="mt-1 text-3xl font-extrabold text-brand-green-deep">
            {dashboard.resolvedAwaitingConfirmCount}
          </p>
          <p className="mt-1 text-sm text-muted">
            Resolved reports you can confirm or dispute.
          </p>
        </Card>
      </div>

      <section className="mt-10" aria-labelledby="attention-heading">
        <div className="flex items-center justify-between">
          <h2 id="attention-heading" className="text-xl font-bold">
            Needs attention
          </h2>
          <Link to="/app/reports" className="text-sm font-medium text-brand-green-deep hover:underline">
            View all reports
          </Link>
        </div>
        {dashboard.attentionReports.length === 0 ? (
          <div className="mt-4">
            <EmptyState
              title="Nothing needs your attention"
              description="Reports that need your input or confirmation will appear here."
            />
          </div>
        ) : (
          <ul className="mt-4 divide-y divide-line rounded-card border border-line bg-surface">
            {dashboard.attentionReports.map((r) => (
              <li key={r.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <div>
                  <Link
                    to={`/app/reports/${r.id}`}
                    className="font-medium text-brand-green-deep hover:underline"
                  >
                    {r.title}
                  </Link>
                  <p className="text-sm text-muted">{r.code}</p>
                </div>
                <Badge tone={statusTone(r.status)}>{statusLabel(r.status)}</Badge>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
