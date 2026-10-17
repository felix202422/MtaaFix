import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, isApiError } from '../../lib/api';
import type { AdminDashboard } from '../../lib/types';
import { Badge, Card, ErrorState, LoadingState } from '../../design-system';
import { statusLabel, statusTone, formatDate } from '../../lib/status';
import { AdminPageShell } from './AdminPageShell';

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <Card aria-label={label}>
      <p className="text-sm text-muted">{label}</p>
      <p className="mt-1 text-3xl font-bold text-brand-black">{value}</p>
    </Card>
  );
}

function BarChart({
  data,
  ariaLabel,
}: {
  data: { date: string; count: number }[];
  ariaLabel: string;
}) {
  const max = Math.max(1, ...data.map((d) => d.count));
  return (
    <div role="img" aria-label={ariaLabel} className="flex items-end gap-1">
      {data.map((day) => (
        <div
          key={day.date}
          title={`${day.date}: ${day.count}`}
          className="w-3 rounded-t bg-brand-green/70"
          style={{ height: `${Math.max(4, (day.count / max) * 72)}px` }}
        />
      ))}
    </div>
  );
}

export function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .getAdminDashboard()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled)
          setError(isApiError(err) ? err.message : 'Could not load the admin dashboard.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <AdminPageShell
      title="Admin dashboard"
      description="Live platform overview: queue, workload and recent activity (FR-7.4)."
    >
      {error && <ErrorState message={error} onRetry={() => window.location.reload()} />}
      {!error && !data && <LoadingState label="Loading dashboard…" />}
      {data && (
        <div className="flex flex-col gap-6">
          <section aria-label="Summary cards" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total reports" value={data.totalReports} />
            <StatCard label="New (last 7 days)" value={data.newLast7Days} />
            <StatCard label="Unassigned" value={data.workload.unassigned} />
            <StatCard
              label="Needs information"
              value={data.moderationQueue.needsInformation}
            />
          </section>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card aria-label="Report activity">
              <h2 className="mb-3 text-base font-semibold text-brand-black">
                Report activity (14 days)
              </h2>
              <BarChart
                data={data.activity14d}
                ariaLabel="Daily report counts for the last 14 days"
              />
              <p className="mt-2 text-xs text-muted">
                One bar per day, Africa/Dar_es_Salaam timezone.
              </p>
            </Card>

            <Card aria-label="Category distribution">
              <h2 className="mb-3 text-base font-semibold text-brand-black">
                Category distribution
              </h2>
              {data.byCategory.length === 0 ? (
                <p className="text-sm text-muted">No reports yet.</p>
              ) : (
                <ul className="flex flex-col gap-2 text-sm">
                  {data.byCategory.slice(0, 8).map((cat) => (
                    <li key={cat.categoryId ?? cat.name} className="flex justify-between gap-3">
                      <span className="text-brand-black">{cat.name}</span>
                      <span className="font-semibold text-muted">{cat.count}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card aria-label="Moderation queue">
              <h2 className="mb-3 text-base font-semibold text-brand-black">Moderation queue</h2>
              <div className="mb-3 flex flex-wrap gap-2 text-sm">
                <Badge tone="neutral">Submitted {data.moderationQueue.submitted}</Badge>
                <Badge tone="info">Under review {data.moderationQueue.underReview}</Badge>
                <Badge tone="warning">Needs info {data.moderationQueue.needsInformation}</Badge>
                <Badge tone="info">Resolution review {data.moderationQueue.resolutionReview}</Badge>
              </div>
              {data.moderationQueue.items.length === 0 ? (
                <p className="text-sm text-muted">The queue is empty.</p>
              ) : (
                <ul className="flex flex-col gap-2 text-sm">
                  {data.moderationQueue.items.map((item) => (
                    <li key={item.id} className="flex items-center justify-between gap-3">
                      <Link
                        to={`/admin/reports/${item.id}`}
                        className="font-medium text-brand-blue hover:underline"
                      >
                        {item.code} — {item.title}
                      </Link>
                      <Badge tone={statusTone(item.status)}>{statusLabel(item.status)}</Badge>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card aria-label="Assignment workload">
              <h2 className="mb-3 text-base font-semibold text-brand-black">
                Assignment workload
              </h2>
              <p className="mb-3 text-sm text-muted">
                {data.workload.assigned} assigned / {data.workload.unassigned} unassigned.
              </p>
              {data.workload.byOrganisation.length === 0 ? (
                <p className="text-sm text-muted">No organisation workload recorded.</p>
              ) : (
                <ul className="flex flex-col gap-1 text-sm">
                  {data.workload.byOrganisation.map((org) => (
                    <li key={org.organisationId ?? org.name} className="flex justify-between">
                      <span>{org.name}</span>
                      <span className="font-semibold">{org.assignedCount}</span>
                    </li>
                  ))}
                </ul>
              )}
              {data.workload.byWorker.length > 0 && (
                <ul className="mt-3 flex flex-col gap-1 text-sm">
                  {data.workload.byWorker.map((worker) => (
                    <li key={worker.userId ?? worker.name ?? 'w'} className="flex justify-between">
                      <span className="text-muted">{worker.name ?? worker.userId}</span>
                      <span className="font-semibold">{worker.activeAssignments}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card aria-label="Attention required">
              <h2 className="mb-3 text-base font-semibold text-brand-black">Attention required</h2>
              <div className="mb-3 flex flex-wrap gap-2 text-sm">
                <Badge tone="warning">Needs info {data.attention.needsInformation}</Badge>
                <Badge tone="success">
                  Awaiting confirmation {data.attention.resolvedAwaitingConfirm}
                </Badge>
              </div>
              {data.attention.needsInfoItems.length === 0 ? (
                <p className="text-sm text-muted">Nothing waiting on citizens.</p>
              ) : (
                <ul className="flex flex-col gap-2 text-sm">
                  {data.attention.needsInfoItems.map((item) => (
                    <li key={item.id}>
                      <Link
                        to={`/admin/reports/${item.id}`}
                        className="font-medium text-brand-blue hover:underline"
                      >
                        {item.code} — {item.title}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card aria-label="Geographic overview">
              <h2 className="mb-3 text-base font-semibold text-brand-black">
                Geographic overview
              </h2>
              {data.workload.byOrganisation.length === 0 ? (
                <p className="text-sm text-muted">
                  No organisation jurisdiction data yet. Spatial bbox filtering is available on
                  the Analytics page.
                </p>
              ) : (
                <>
                  <p className="mb-2 text-sm text-muted">
                    Reports by responsible organisation (jurisdiction view):
                  </p>
                  <ul className="flex flex-col gap-1 text-sm">
                    {data.workload.byOrganisation.map((org) => (
                      <li key={org.organisationId ?? org.name} className="flex justify-between">
                        <span>{org.name}</span>
                        <span className="font-semibold">{org.assignedCount} assigned</span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
              <p className="mt-3 text-sm">
                <Link to="/admin/analytics" className="text-brand-blue hover:underline">
                  Open spatial analytics →
                </Link>
              </p>
            </Card>
          </div>

          <Card aria-label="Recent reports">
            <h2 className="mb-3 text-base font-semibold text-brand-black">Recent reports</h2>
            {data.recent.length === 0 ? (
              <p className="text-sm text-muted">No reports yet.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-line text-xs uppercase text-muted">
                      <th className="py-2 pr-4">Reference</th>
                      <th className="py-2 pr-4">Title</th>
                      <th className="py-2 pr-4">Status</th>
                      <th className="py-2 pr-4">Reported</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.recent.map((report) => (
                      <tr key={report.id} className="border-b border-line/60">
                        <td className="py-2 pr-4">
                          <Link
                            to={`/admin/reports/${report.id}`}
                            className="font-medium text-brand-blue hover:underline"
                          >
                            {report.code}
                          </Link>
                        </td>
                        <td className="py-2 pr-4 text-brand-black">{report.title}</td>
                        <td className="py-2 pr-4">
                          <Badge tone={statusTone(report.status)}>
                            {statusLabel(report.status)}
                          </Badge>
                        </td>
                        <td className="py-2 pr-4 text-muted">{formatDate(report.reportedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>
      )}
    </AdminPageShell>
  );
}
