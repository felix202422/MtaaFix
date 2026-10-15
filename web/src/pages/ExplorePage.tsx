import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Button, Card, EmptyState, ErrorState, LoadingState } from '../design-system';
import { api } from '../lib/api';
import { formatDate, statusLabel, statusTone } from '../lib/status';
import type { PublicCategory, PublicReportSummary, ReportStatus } from '../lib/types';

const STATUS_OPTIONS: ReportStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'NEEDS_INFORMATION',
  'VERIFIED',
  'ASSIGNED',
  'IN_PROGRESS',
  'RESOLUTION_REVIEW',
  'RESOLVED',
  'REJECTED',
  'DUPLICATE',
  'REOPENED',
  'CLOSED',
];

const PAGE_SIZE = 10;

type FeedState =
  | { kind: 'loading' }
  | {
      kind: 'ready';
      content: PublicReportSummary[];
      totalElements: number;
      totalPages: number;
      page: number;
    }
  | { kind: 'error'; message: string };

export function ExplorePage() {
  const [searchInput, setSearchInput] = useState('');
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const [categories, setCategories] = useState<PublicCategory[]>([]);
  const [feed, setFeed] = useState<FeedState>({ kind: 'loading' });
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    api
      .getPublicCategories({ signal: controller.signal })
      .then(setCategories)
      .catch(() => setCategories([]));
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setFeed({ kind: 'loading' });
    api
      .getPublicReports({ q, status, categoryId, dateFrom, dateTo, page, size: PAGE_SIZE }, {
        signal: controller.signal,
      })
      .then((data) =>
        setFeed({
          kind: 'ready',
          content: data.content,
          totalElements: data.totalElements,
          totalPages: data.totalPages,
          page: data.page,
        }),
      )
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setFeed({
          kind: 'error',
          message: error instanceof Error ? error.message : 'Could not load reports.',
        });
      });
    return () => controller.abort();
  }, [q, status, categoryId, dateFrom, dateTo, page, retryKey]);

  const submitSearch = useCallback(
    (event: FormEvent) => {
      event.preventDefault();
      setPage(0);
      setQ(searchInput.trim());
    },
    [searchInput],
  );

  const clearFilters = useCallback(() => {
    setSearchInput('');
    setQ('');
    setStatus('');
    setCategoryId('');
    setDateFrom('');
    setDateTo('');
    setPage(0);
  }, []);

  const hasFilters = q !== '' || status !== '' || categoryId !== '' || dateFrom !== '' || dateTo !== '';

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold">Explore community reports</h1>
      <p className="mt-2 text-muted">
        Browse publicly visible issue reports across the community.
      </p>

      <form
        onSubmit={submitSearch}
        className="mt-6 grid gap-4 rounded-card border border-line bg-surface p-4 sm:grid-cols-2 lg:grid-cols-4"
        role="search"
        aria-label="Filter reports"
      >
        <div className="sm:col-span-2 lg:col-span-4">
          <label htmlFor="explore-search" className="text-sm font-semibold">
            Search
          </label>
          <input
            id="explore-search"
            type="search"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search title or description…"
            className="mt-1 h-11 w-full rounded-lg border border-line bg-canvas px-3 text-base"
          />
        </div>
        <div>
          <label htmlFor="explore-status" className="text-sm font-semibold">
            Status
          </label>
          <select
            id="explore-status"
            value={status}
            onChange={(event) => {
              setPage(0);
              setStatus(event.target.value);
            }}
            className="mt-1 h-11 w-full rounded-lg border border-line bg-canvas px-3 text-base"
          >
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((value) => (
              <option key={value} value={value}>
                {statusLabel(value)}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="explore-category" className="text-sm font-semibold">
            Category
          </label>
          <select
            id="explore-category"
            value={categoryId}
            onChange={(event) => {
              setPage(0);
              setCategoryId(event.target.value);
            }}
            className="mt-1 h-11 w-full rounded-lg border border-line bg-canvas px-3 text-base"
          >
            <option value="">All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="explore-from" className="text-sm font-semibold">
            From
          </label>
          <input
            id="explore-from"
            type="date"
            value={dateFrom}
            onChange={(event) => {
              setPage(0);
              setDateFrom(event.target.value);
            }}
            className="mt-1 h-11 w-full rounded-lg border border-line bg-canvas px-3 text-base"
          />
        </div>
        <div>
          <label htmlFor="explore-to" className="text-sm font-semibold">
            To
          </label>
          <input
            id="explore-to"
            type="date"
            value={dateTo}
            onChange={(event) => {
              setPage(0);
              setDateTo(event.target.value);
            }}
            className="mt-1 h-11 w-full rounded-lg border border-line bg-canvas px-3 text-base"
          />
        </div>
        <div className="flex items-end gap-2 sm:col-span-2 lg:col-span-4">
          <Button type="submit">Apply filters</Button>
          {hasFilters && (
            <Button type="button" variant="outline" onClick={clearFilters}>
              Clear filters
            </Button>
          )}
        </div>
      </form>

      <div aria-live="polite" aria-atomic="true" className="sr-only">
        {feed.kind === 'ready'
          ? `${feed.totalElements} reports found.`
          : feed.kind === 'error'
            ? feed.message
            : 'Loading reports…'}
      </div>

      {feed.kind === 'loading' && <LoadingState label="Loading reports…" />}
      {feed.kind === 'error' && (
        <ErrorState
          message={feed.message}
          onRetry={() => setRetryKey((key) => key + 1)}
        />
      )}
      {feed.kind === 'ready' && (
        <>
          <p className="mt-6 text-sm text-muted" role="status">
            {feed.totalElements} report{feed.totalElements === 1 ? '' : 's'} found
          </p>
          {feed.content.length === 0 ? (
            <EmptyState
              title="No reports match these filters"
              description="Try broadening the search or clearing the filters."
              action={
                hasFilters ? (
                  <Button variant="outline" size="sm" onClick={clearFilters}>
                    Clear filters
                  </Button>
                ) : undefined
              }
            />
          ) : (
            <ul className="mt-4 grid gap-4 sm:grid-cols-2">
              {feed.content.map((report) => (
                <li key={report.code}>
                  <Card className="h-full">
                    <div className="flex items-start justify-between gap-3">
                      <p className="font-mono text-xs text-muted">{report.code}</p>
                      <Badge tone={statusTone(report.status)}>
                        {statusLabel(report.status)}
                      </Badge>
                    </div>
                    <p className="mt-2 font-semibold">{report.title}</p>
                    {report.description && (
                      <p className="mt-1 line-clamp-2 text-sm text-muted">{report.description}</p>
                    )}
                    <p className="mt-2 text-xs text-muted">
                      {[report.categoryName, formatDate(report.reportedAt)]
                        .filter(Boolean)
                        .join(' · ')}
                    </p>
                    <p className="mt-3">
                      <Link
                        to={`/reports/${report.code}`}
                        className="text-sm font-semibold text-brand-green-deep hover:underline"
                        aria-label={`View details for ${report.title} (${report.code})`}
                      >
                        View details →
                      </Link>
                    </p>
                  </Card>
                </li>
              ))}
            </ul>
          )}
          {feed.totalPages > 1 && (
            <nav
              aria-label="Pagination"
              className="mt-8 flex items-center justify-between gap-4"
            >
              <Button
                variant="outline"
                size="sm"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                ← Previous
              </Button>
              <p className="text-sm text-muted">
                Page {feed.page + 1} of {feed.totalPages}
              </p>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= feed.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next →
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
