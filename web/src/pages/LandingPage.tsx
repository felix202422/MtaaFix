import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Card, ErrorState, LinkButton, LoadingState } from '../design-system';
import { api } from '../lib/api';
import { formatDate, statusLabel, statusTone } from '../lib/status';
import type { PublicReportSummary, PublicStats } from '../lib/types';

type LoadState<T> =
  | { kind: 'loading' }
  | { kind: 'ready'; data: T }
  | { kind: 'error'; message: string };

export function LandingPage() {
  const [stats, setStats] = useState<LoadState<PublicStats>>({ kind: 'loading' });
  const [recent, setRecent] = useState<LoadState<PublicReportSummary[]>>({ kind: 'loading' });
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setStats({ kind: 'loading' });
    api
      .getPublicStats({ signal: controller.signal })
      .then((data) => setStats({ kind: 'ready', data }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setStats({
          kind: 'error',
          message: error instanceof Error ? error.message : 'Could not load statistics.',
        });
      });
    return () => controller.abort();
  }, [retryKey]);

  useEffect(() => {
    const controller = new AbortController();
    setRecent({ kind: 'loading' });
    api
      .getPublicReports({ page: 0, size: 5 }, { signal: controller.signal })
      .then((page) => setRecent({ kind: 'ready', data: page.content }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setRecent({
          kind: 'error',
          message: error instanceof Error ? error.message : 'Could not load recent reports.',
        });
      });
    return () => controller.abort();
  }, [retryKey]);

  const retry = () => setRetryKey((key) => key + 1);

  return (
    <div className="mx-auto max-w-6xl px-4 sm:px-6">
      <section className="grid gap-8 py-12 lg:grid-cols-2 lg:py-16" aria-labelledby="hero-heading">
        <div>
          <h1 id="hero-heading" className="text-4xl font-extrabold leading-tight sm:text-5xl">
            See a Problem?{' '}
            <span className="text-brand-green">Let&apos;s Fix It Together.</span>
          </h1>
          <p className="mt-4 max-w-md text-muted">
            Report potholes, broken streetlights, waste, and drainage issues in your
            community — and follow how they get resolved.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <LinkButton to="/report">Report an Issue</LinkButton>
            <LinkButton to="/explore" variant="outline">
              Explore Reports
            </LinkButton>
          </div>
        </div>
        <div aria-labelledby="stats-heading">
          <h2 id="stats-heading" className="sr-only">
            Platform statistics
          </h2>
          {stats.kind === 'loading' && <LoadingState label="Loading statistics…" />}
          {stats.kind === 'error' && (
            <ErrorState message={stats.message} onRetry={retry} />
          )}
          {stats.kind === 'ready' && (
            <div className="grid grid-cols-2 gap-4">
              <Card>
                <p className="text-2xl font-extrabold text-brand-green-deep">
                  {stats.data.totalReports.toLocaleString()}
                </p>
                <p className="text-sm text-muted">Reports shared</p>
              </Card>
              <Card>
                <p className="text-2xl font-extrabold text-brand-green-deep">
                  {stats.data.resolvedReports.toLocaleString()}
                </p>
                <p className="text-sm text-muted">Issues resolved</p>
              </Card>
              <Card>
                <p className="text-2xl font-extrabold text-brand-green-deep">
                  {stats.data.submittedReports.toLocaleString()}
                </p>
                <p className="text-sm text-muted">Awaiting review</p>
              </Card>
              <Card>
                <p className="text-2xl font-extrabold text-brand-green-deep">
                  {stats.data.categories.toLocaleString()}
                </p>
                <p className="text-sm text-muted">Issue categories</p>
              </Card>
            </div>
          )}
        </div>
      </section>

      <section
        className="border-t border-line py-10"
        aria-labelledby="how-summary-heading"
      >
        <h2 id="how-summary-heading" className="text-2xl font-bold">
          How it works
        </h2>
        <ol className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { step: '1. Report', text: 'Describe the problem, pick a category, drop a pin.' },
            { step: '2. Review', text: 'Moderators verify the issue and route it to the right team.' },
            { step: '3. Fix', text: 'Organizations and field workers complete the repair.' },
            { step: '4. Confirm', text: 'You review the result and confirm the issue is resolved.' },
          ].map((item) => (
            <li key={item.step}>
              <Card>
                <p className="font-semibold text-brand-green-deep">{item.step}</p>
                <p className="mt-1 text-sm text-muted">{item.text}</p>
              </Card>
            </li>
          ))}
        </ol>
        <p className="mt-4">
          <Link to="/how-it-works" className="font-semibold text-brand-green-deep hover:underline">
            See the full lifecycle →
          </Link>
        </p>
      </section>

      <section className="border-t border-line py-10" aria-labelledby="recent-heading">
        <div className="flex items-center justify-between gap-4">
          <h2 id="recent-heading" className="text-2xl font-bold">
            Recent public reports
          </h2>
          <Link to="/explore" className="text-sm font-semibold text-brand-green-deep hover:underline">
            View all →
          </Link>
        </div>
        {recent.kind === 'loading' && <LoadingState label="Loading recent reports…" />}
        {recent.kind === 'error' && (
          <ErrorState message={recent.message} onRetry={retry} />
        )}
        {recent.kind === 'ready' &&
          (recent.data.length === 0 ? (
            <p className="mt-4 text-sm text-muted">
              No public reports yet. Be the first to share one.
            </p>
          ) : (
            <ul className="mt-4 space-y-3">
              {recent.data.map((report) => (
                <li key={report.code}>
                  <Link
                    to={`/reports/${report.code}`}
                    className="flex items-center justify-between gap-4 rounded-lg border border-line bg-surface px-4 py-3 transition-colors hover:border-brand-green"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">{report.title}</p>
                      <p className="font-mono text-xs text-muted">
                        {report.code} · {formatDate(report.reportedAt)}
                      </p>
                    </div>
                    <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
                  </Link>
                </li>
              ))}
            </ul>
          ))}
      </section>
    </div>
  );
}
