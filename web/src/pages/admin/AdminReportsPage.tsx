import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, isApiError } from '../../lib/api';
import type { AdminReportFilters, Category, Report } from '../../lib/types';
import { Badge, Button, Card, EmptyState, ErrorState, Field, LoadingState } from '../../design-system';
import { statusLabel, statusTone, formatDate } from '../../lib/status';
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

export function AdminReportsPage() {
  const [filters, setFilters] = useState<AdminReportFilters>({ page: 0, size: 20 });
  const [reports, setReports] = useState<Report[] | null>(null);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [q, setQ] = useState('');

  const load = useCallback((next: AdminReportFilters) => {
    setError(null);
    setReports(null);
    api
      .listStaffReports(next)
      .then((page) => {
        setReports(page.content);
        setTotalPages(page.totalPages);
        setTotalElements(page.totalElements);
      })
      .catch((err) =>
        setError(isApiError(err) ? err.message : 'Could not load reports.'),
      );
  }, []);

  useEffect(() => {
    load(filters);
  }, [filters, load]);

  useEffect(() => {
    api
      .getCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  function apply(patch: Partial<AdminReportFilters>) {
    setFilters((prev) => ({ ...prev, ...patch, page: patch.page ?? 0 }));
  }

  return (
    <AdminPageShell
      title="Report management"
      description="Searchable, filterable table across every report your role may see (P15)."
    >
      <Card aria-label="Filters">
        <form
          className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5"
          onSubmit={(event) => {
            event.preventDefault();
            apply({ q: q.trim() || undefined });
          }}
        >
          <Field
            label="Search reference or text"
            placeholder="MTF-… or keyword"
            value={q}
            onChange={(event) => setQ(event.target.value)}
          />
          <div>
            <label htmlFor="reports-status" className="mb-1 block text-sm font-medium">
              Status
            </label>
            <select
              id="reports-status"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={filters.status ?? ''}
              onChange={(event) => apply({ status: event.target.value || undefined })}
            >
              <option value="">All statuses</option>
              {STATUSES.map((status) => (
                <option key={status} value={status}>
                  {statusLabel(status as Report['status'])}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="reports-category" className="mb-1 block text-sm font-medium">
              Category
            </label>
            <select
              id="reports-category"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={filters.categoryId ?? ''}
              onChange={(event) => apply({ categoryId: event.target.value || undefined })}
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
            <label htmlFor="reports-assignment" className="mb-1 block text-sm font-medium">
              Assignment
            </label>
            <select
              id="reports-assignment"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={filters.assignment ?? ''}
              onChange={(event) =>
                apply({
                  assignment:
                    (event.target.value as 'assigned' | 'unassigned' | '') || undefined,
                })
              }
            >
              <option value="">Any</option>
              <option value="assigned">Assigned</option>
              <option value="unassigned">Unassigned</option>
            </select>
          </div>
          <div className="flex items-end gap-2">
            <Button type="submit" size="sm">
              Apply
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => {
                setQ('');
                setFilters({ page: 0, size: 20 });
              }}
            >
              Reset
            </Button>
          </div>
        </form>
      </Card>

      {error && <ErrorState message={error} onRetry={() => load(filters)} />}
      {!error && !reports && <LoadingState label="Loading reports…" />}
      {reports && reports.length === 0 && (
        <EmptyState title="No reports match these filters." />
      )}
      {reports && reports.length > 0 && (
        <Card padded={false} aria-label="Reports table">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line bg-canvas text-xs uppercase text-muted">
                  <th className="px-4 py-3">Reference</th>
                  <th className="px-4 py-3">Title</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Assignment</th>
                  <th className="px-4 py-3">Reported</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((report) => (
                  <tr key={report.id} className="border-b border-line/60">
                    <td className="px-4 py-3 font-medium text-brand-black">{report.code}</td>
                    <td className="px-4 py-3 text-brand-black">{report.title}</td>
                    <td className="px-4 py-3">
                      <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
                    </td>
                    <td className="px-4 py-3 text-muted">
                      {report.assigneeId ? 'Assigned' : 'Unassigned'}
                      {report.organisationId ? ' · org' : ''}
                    </td>
                    <td className="px-4 py-3 text-muted">{formatDate(report.reportedAt)}</td>
                    <td className="px-4 py-3">
                      <Link
                        to={`/admin/reports/${report.id}`}
                        className="font-medium text-brand-blue hover:underline"
                      >
                        Open
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {reports && totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted">
          <span>
            Page {(filters.page ?? 0) + 1} of {Math.max(totalPages, 1)} · {totalElements} reports
          </span>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={(filters.page ?? 0) <= 0}
              onClick={() => apply({ page: (filters.page ?? 0) - 1 })}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={(filters.page ?? 0) + 1 >= totalPages}
              onClick={() => apply({ page: (filters.page ?? 0) + 1 })}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </AdminPageShell>
  );
}
