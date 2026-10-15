import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Button, EmptyState, ErrorState, LoadingState } from '../../design-system';
import { api, isApiError } from '../../lib/api';
import { formatDate, statusLabel, statusTone } from '../../lib/status';
import type { PagedResponse, Report } from '../../lib/types';

export function MyReportsPage() {
  const [page, setPage] = useState<PagedResponse<Report> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    setError(null);
    try {
      setPage(await api.getMyReports({ page: p, size: 20 }));
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not load your reports.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load(0);
  }, [load]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-extrabold">My reports</h1>
        <Link to="/app/reports/new">
          <Button>Report an issue</Button>
        </Link>
      </div>

      {loading && <LoadingState label="Loading your reports…" />}
      {error && <ErrorState message={error} onRetry={() => void load(page?.page ?? 0)} />}

      {!loading && !error && page && page.content.length === 0 && (
        <div className="mt-6">
          <EmptyState
            title="No reports yet"
            description="When you report an issue it will appear here so you can track its progress."
            action={
              <Link to="/app/reports/new">
                <Button>Report an issue</Button>
              </Link>
            }
          />
        </div>
      )}

      {!loading && !error && page && page.content.length > 0 && (
        <>
          <ul className="mt-6 divide-y divide-line rounded-card border border-line bg-surface">
            {page.content.map((r) => (
              <li key={r.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <div>
                  <Link
                    to={`/app/reports/${r.id}`}
                    className="font-medium text-brand-green-deep hover:underline"
                  >
                    {r.title}
                  </Link>
                  <p className="text-sm text-muted">
                    {r.code} · {formatDate(r.reportedAt)}
                  </p>
                </div>
                <Badge tone={statusTone(r.status)}>{statusLabel(r.status)}</Badge>
              </li>
            ))}
          </ul>

          {page.totalPages > 1 && (
            <nav className="mt-4 flex items-center justify-between text-sm" aria-label="Pagination">
              <Button
                variant="outline"
                size="sm"
                disabled={page.page === 0}
                onClick={() => void load(page.page - 1)}
              >
                Previous
              </Button>
              <span className="text-muted">
                Page {page.page + 1} of {page.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page.page >= page.totalPages - 1}
                onClick={() => void load(page.page + 1)}
              >
                Next
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
