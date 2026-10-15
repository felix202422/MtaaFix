import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Badge, Card, ErrorState, LinkButton, LoadingState } from '../design-system';
import { api, isApiError } from '../lib/api';
import { formatDate, statusLabel, statusTone } from '../lib/status';
import type { PublicReportDetail } from '../lib/types';

type DetailState =
  | { kind: 'loading' }
  | { kind: 'ready'; report: PublicReportDetail }
  | { kind: 'error'; message: string; notFound: boolean };

export function PublicReportDetailPage() {
  const { code } = useParams<{ code: string }>();
  const [state, setState] = useState<DetailState>({ kind: 'loading' });
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    if (!code) return;
    const controller = new AbortController();
    setState({ kind: 'loading' });
    api
      .getPublicReport(code, { signal: controller.signal })
      .then((report) => setState({ kind: 'ready', report }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setState({
          kind: 'error',
          message: error instanceof Error ? error.message : 'Could not load this report.',
          notFound: isApiError(error) && error.status === 404,
        });
      });
    return () => controller.abort();
  }, [code, retryKey]);

  if (state.kind === 'loading') {
    return <LoadingState label="Loading report…" />;
  }

  if (state.kind === 'error') {
    if (state.notFound) {
      return (
        <div className="mx-auto max-w-3xl px-4 py-16 text-center sm:px-6">
          <h1 className="text-2xl font-extrabold">Report not found</h1>
          <p className="mt-3 text-muted">
            No public report matches the reference <span className="font-mono">{code}</span>.
            It may have been removed, or the reference may be mistyped.
          </p>
          <div className="mt-6 flex justify-center gap-3">
            <LinkButton to="/explore">Explore reports</LinkButton>
            <LinkButton to="/" variant="outline">
              Back to home
            </LinkButton>
          </div>
        </div>
      );
    }
    return (
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
        <ErrorState message={state.message} onRetry={() => setRetryKey((k) => k + 1)} />
      </div>
    );
  }

  const report = state.report;

  return (
    <article className="mx-auto max-w-3xl px-4 py-10 sm:px-6" aria-labelledby="report-title">
      <p className="font-mono text-sm text-muted">{report.code}</p>
      <div className="mt-2 flex flex-wrap items-center gap-3">
        <h1 id="report-title" className="text-3xl font-extrabold">
          {report.title}
        </h1>
        <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
      </div>

      <dl className="mt-6 grid gap-4 sm:grid-cols-2">
        <Card>
          <dt className="text-sm font-semibold text-muted">Category</dt>
          <dd className="mt-1">{report.categoryName ?? '—'}</dd>
        </Card>
        <Card>
          <dt className="text-sm font-semibold text-muted">Submitted</dt>
          <dd className="mt-1">{formatDate(report.reportedAt)}</dd>
        </Card>
        <Card>
          <dt className="text-sm font-semibold text-muted">Approximate location</dt>
          <dd className="mt-1">
            {report.latitude !== null && report.longitude !== null
              ? `${report.latitude.toFixed(2)}, ${report.longitude.toFixed(2)}`
              : 'Not shared'}
          </dd>
        </Card>
        {report.resolvedAt && (
          <Card>
            <dt className="text-sm font-semibold text-muted">Resolved</dt>
            <dd className="mt-1">{formatDate(report.resolvedAt)}</dd>
          </Card>
        )}
      </dl>

      {report.description && (
        <section className="mt-8" aria-labelledby="description-heading">
          <h2 id="description-heading" className="text-xl font-bold">
            Description
          </h2>
          <p className="mt-2 whitespace-pre-wrap text-muted">{report.description}</p>
        </section>
      )}

      {report.duplicateOfCode && (
        <p className="mt-6 rounded-card border border-line bg-surface p-4 text-sm">
          This report was marked as a duplicate of{' '}
          <Link
            to={`/reports/${report.duplicateOfCode}`}
            className="font-mono font-semibold text-brand-green-deep hover:underline"
          >
            {report.duplicateOfCode}
          </Link>
          .
        </p>
      )}

      <section className="mt-8" aria-labelledby="evidence-heading">
        <h2 id="evidence-heading" className="text-xl font-bold">
          Public evidence
        </h2>
        <p className="mt-2 text-sm text-muted">
          Photo evidence is only shared publicly once it has been classified for public
          display. None is available for this report yet.
        </p>
      </section>

      <section className="mt-8" aria-labelledby="timeline-heading">
        <h2 id="timeline-heading" className="text-xl font-bold">
          Status timeline
        </h2>
        <ol className="mt-4 space-y-3">
          {report.timeline.map((entry, index) => (
            <li key={`${entry.status}-${entry.timestamp ?? index}`} className="flex gap-3">
              <span
                aria-hidden="true"
                className="mt-1.5 size-2.5 shrink-0 rounded-full bg-brand-green"
              />
              <div>
                <p className="text-sm font-semibold">{statusLabel(entry.status)}</p>
                <p className="text-xs text-muted">{formatDate(entry.timestamp)}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <p className="mt-10">
        <Link to="/explore" className="font-semibold text-brand-green-deep hover:underline">
          ← Back to explore
        </Link>
      </p>
    </article>
  );
}
