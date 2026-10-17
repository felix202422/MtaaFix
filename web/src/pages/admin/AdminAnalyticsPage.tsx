import { useEffect, useState } from 'react';
import { useAuth } from '../../app/AuthProvider';
import { api, isApiError } from '../../lib/api';
import type { AnalyticsSummary, Category, Organisation } from '../../lib/types';
import {
  Button,
  Card,
  ErrorState,
  Field,
  LoadingState,
} from '../../design-system';
import { statusLabel } from '../../lib/status';
import { AdminPageShell } from './AdminPageShell';

const STATUSES = [
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

export function AdminAnalyticsPage() {
  const { user } = useAuth();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [status, setStatus] = useState('');
  const [orgId, setOrgId] = useState('');
  const [bbox, setBbox] = useState('');
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [organisations, setOrganisations] = useState<Organisation[]>([]);

  useEffect(() => {
    api.getCategories().then(setCategories).catch(() => setCategories([]));
    api.listOrganisations().then(setOrganisations).catch(() => setOrganisations([]));
  }, []);

  useEffect(() => {
    setSummary(null);
    setError(null);
    api
      .getAnalyticsSummary({
        from: from ? new Date(from).toISOString() : undefined,
        to: to ? new Date(to).toISOString() : undefined,
        categoryId: categoryId || undefined,
        status: status || undefined,
        orgId: orgId || undefined,
        bbox: bbox.trim() || undefined,
      })
      .then(setSummary)
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load analytics.'));
  }, [from, to, categoryId, status, orgId, bbox]);

  return (
    <AdminPageShell
      title="Analytics and reports"
      description="Metrics computed from real report data with date, category, status, organization and geographic filters (FR-7.3, P23)."
    >
      <Card aria-label="Analytics filters">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field
            label="From"
            type="datetime-local"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
          />
          <Field
            label="To"
            type="datetime-local"
            value={to}
            onChange={(event) => setTo(event.target.value)}
          />
          <div>
            <label htmlFor="analytics-category" className="mb-1 block text-sm font-medium">
              Category
            </label>
            <select
              id="analytics-category"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
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
            <label htmlFor="analytics-status" className="mb-1 block text-sm font-medium">
              Status
            </label>
            <select
              id="analytics-status"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="">All statuses</option>
              {STATUSES.map((value) => (
                <option key={value} value={value}>
                  {statusLabel(value as never)}
                </option>
              ))}
            </select>
          </div>
          {user?.role === 'ADMINISTRATOR' && (
            <div>
              <label htmlFor="analytics-org" className="mb-1 block text-sm font-medium">
                Organization
              </label>
              <select
                id="analytics-org"
                className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
                value={orgId}
                onChange={(event) => setOrgId(event.target.value)}
              >
                <option value="">All organizations</option>
                {organisations.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
            </div>
          )}
          <Field
            label="Bounding box"
            hint="minLon,minLat,maxLon,maxLat (e.g. 39.0,-7.0,40.0,-6.0)"
            value={bbox}
            onChange={(event) => setBbox(event.target.value)}
          />
        </div>
        <div className="mt-4">
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setFrom('');
              setTo('');
              setCategoryId('');
              setStatus('');
              setOrgId('');
              setBbox('');
            }}
          >
            Clear filters
          </Button>
        </div>
      </Card>

      {error && <ErrorState message={error} />}
      {!error && !summary && <LoadingState label="Computing analytics…" />}
      {summary && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card aria-label="Totals">
            <h2 className="mb-3 text-base font-semibold text-brand-black">Totals</h2>
            <p className="text-sm text-muted">
              <span className="text-3xl font-bold text-brand-black">{summary.total}</span> matching
              reports · {summary.unassigned} unassigned
              {summary.avgResolutionHours !== null && (
                <> · avg resolution {summary.avgResolutionHours} h</>
              )}
            </p>
            <h3 className="mb-2 mt-4 text-sm font-semibold">By status</h3>
            <ul className="grid grid-cols-2 gap-1 text-sm">
              {Object.entries(summary.byStatus)
                .filter(([, count]) => count > 0)
                .map(([key, count]) => (
                  <li key={key} className="flex justify-between">
                    <span>{statusLabel(key as never)}</span>
                    <span className="font-semibold">{count}</span>
                  </li>
                ))}
            </ul>
          </Card>

          <Card aria-label="Category breakdown">
            <h2 className="mb-3 text-base font-semibold text-brand-black">By category</h2>
            {summary.byCategory.length === 0 ? (
              <p className="text-sm text-muted">No matching reports.</p>
            ) : (
              <ul className="flex flex-col gap-1 text-sm">
                {summary.byCategory.map((row) => (
                  <li key={row.categoryId ?? row.name} className="flex justify-between">
                    <span>{row.name}</span>
                    <span className="font-semibold">{row.count}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card aria-label="Weekly submissions">
            <h2 className="mb-3 text-base font-semibold text-brand-black">
              Weekly submissions
            </h2>
            <p className="mb-2 text-xs text-muted">Monday week buckets, Africa/Dar_es_Salaam.</p>
            {summary.weekly.length === 0 ? (
              <p className="text-sm text-muted">No matching reports.</p>
            ) : (
              <ul className="flex flex-col gap-1 text-sm">
                {summary.weekly.map((week) => (
                  <li key={week.weekStart} className="flex justify-between">
                    <span>Week of {week.weekStart}</span>
                    <span className="font-semibold">{week.count}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card aria-label="Organisation leaderboard">
            <h2 className="mb-3 text-base font-semibold text-brand-black">
              Top organizations (assigned)
            </h2>
            {summary.organisations.length === 0 ? (
              <p className="text-sm text-muted">No assigned reports in this range.</p>
            ) : (
              <ul className="flex flex-col gap-1 text-sm">
                {summary.organisations.map((org) => (
                  <li key={org.organisationId ?? org.name} className="flex justify-between">
                    <span>{org.name}</span>
                    <span className="font-semibold">{org.assignedReports}</span>
                  </li>
                ))}
              </ul>
            )}
            <p className="mt-4 text-xs text-muted">
              Geographic filtering uses a PostGIS bounding box over precise staff-visible
              coordinates. Public maps always use generalized locations (FR-11.4).
            </p>
          </Card>
        </div>
      )}
    </AdminPageShell>
  );
}
