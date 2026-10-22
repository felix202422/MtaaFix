import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Button, Card, EmptyState, ErrorState, LoadingState } from '../design-system';
import { ReportMap } from '../components/map/ReportMap';
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
// Phase 17 (NFR-7): the map fetches a wider, separate page so markers are not
// limited to the visible list page. Marker clustering is a later phase — for
// now we cap at 100 points per fetch (documented in PERFORMANCE_REVIEW.md).
const MAP_SIZE = 100;

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

const loadingFeed: FeedState = { kind: 'loading' };

function toFeedState(data: {
  content: PublicReportSummary[];
  totalElements: number;
  totalPages: number;
  page: number;
}): FeedState {
  return {
    kind: 'ready',
    content: data.content,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    page: data.page,
  };
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

export function ExplorePage() {
  const [searchInput, setSearchInput] = useState('');
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [bbox, setBbox] = useState('');
  const [pendingBbox, setPendingBbox] = useState('');
  const [view, setView] = useState<'list' | 'map'>('list');
  const [page, setPage] = useState(0);
  const [categories, setCategories] = useState<PublicCategory[]>([]);
  const [feed, setFeed] = useState<FeedState>(loadingFeed);
  const [mapFeed, setMapFeed] = useState<FeedState>(loadingFeed);
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
    setFeed(loadingFeed);
    api
      .getPublicReports(
        { q, status, categoryId, dateFrom, dateTo, bbox: bbox || undefined, page, size: PAGE_SIZE },
        { signal: controller.signal },
      )
      .then(toFeedState)
      .then(setFeed)
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setFeed({ kind: 'error', message: errorMessage(error, 'Could not load reports.') });
      });
    return () => controller.abort();
  }, [q, status, categoryId, dateFrom, dateTo, bbox, page, retryKey]);

  // Phase 17: map markers use their own wider fetch (MAP_SIZE), so the list
  // page no longer limits what the map shows.
  useEffect(() => {
    if (view !== 'map') return;
    const controller = new AbortController();
    setMapFeed(loadingFeed);
    api
      .getPublicReports(
        { q, status, categoryId, dateFrom, dateTo, bbox: bbox || undefined, page: 0, size: MAP_SIZE },
        { signal: controller.signal },
      )
      .then(toFeedState)
      .then(setMapFeed)
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setMapFeed({ kind: 'error', message: errorMessage(error, 'Could not load map points.') });
      });
    return () => controller.abort();
  }, [view, q, status, categoryId, dateFrom, dateTo, bbox, retryKey]);

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
    setBbox('');
    setPendingBbox('');
    setPage(0);
  }, []);

  const applyThisArea = useCallback(() => {
    setPage(0);
    setBbox(pendingBbox);
  }, [pendingBbox]);

  const hasFilters =
    q !== '' ||
    status !== '' ||
    categoryId !== '' ||
    dateFrom !== '' ||
    dateTo !== '' ||
    bbox !== '';

  const mapPoints = view === 'map' && mapFeed.kind === 'ready'
    ? mapFeed.content
        .filter((report) => report.latitude !== null && report.longitude !== null)
        .map((report) => ({
          code: report.code,
          title: report.title,
          latitude: report.latitude as number,
          longitude: report.longitude as number,
        }))
    : [];

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
          <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-muted" role="status">
              {feed.totalElements} report{feed.totalElements === 1 ? '' : 's'} found
            </p>
            <div
              className="flex items-center gap-2"
              role="group"
              aria-label="Result view"
            >
              <Button
                size="sm"
                variant={view === 'list' ? 'primary' : 'outline'}
                aria-pressed={view === 'list'}
                onClick={() => setView('list')}
              >
                List view
              </Button>
              <Button
                size="sm"
                variant={view === 'map' ? 'primary' : 'outline'}
                aria-pressed={view === 'map'}
                onClick={() => setView('map')}
              >
                Map view
              </Button>
            </div>
          </div>
          {view === 'map' && (
            <div className="mt-4">
              {pendingBbox !== '' && pendingBbox !== bbox && (
                <div className="mb-3">
                  <Button size="sm" onClick={applyThisArea}>
                    Search this area
                  </Button>
                </div>
              )}
              {mapFeed.kind === 'loading' && <LoadingState label="Loading map points…" />}
              {mapFeed.kind === 'error' && (
                <ErrorState
                  message={mapFeed.message}
                  onRetry={() => setRetryKey((key) => key + 1)}
                />
              )}
              {mapFeed.kind === 'ready' && (
                <>
                  <ReportMap
                    points={mapPoints}
                    onBoundsChange={setPendingBbox}
                    ariaLabel="Map of community reports (generalized locations)"
                  />
                  <p className="mt-2 text-xs text-muted" role="status">
                    Showing {mapPoints.length} marker{mapPoints.length === 1 ? '' : 's'}
                    {mapFeed.totalElements > mapPoints.length &&
                      ` of ${mapFeed.totalElements} reports (first ${MAP_SIZE} by newest first)`}
                    .
                  </p>
                </>
              )}
              <p className="mt-2 text-xs text-muted">
                Marker positions are generalized for privacy (FR-11.4) and may be up to
                several hundred metres from the actual location. Pan or zoom, then use
                &ldquo;Search this area&rdquo; to filter by the visible map extent.
              </p>
            </div>
          )}
          {view === 'list' &&
            (feed.content.length === 0 ? (
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
          ))}
          {feed.totalPages > 1 && view === 'list' && (
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
