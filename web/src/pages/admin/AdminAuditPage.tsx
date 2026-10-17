import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../app/AuthProvider';
import { api, isApiError } from '../../lib/api';
import type { AuditLogEntry, AuditLogFilters } from '../../lib/types';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Field,
  LoadingState,
} from '../../design-system';
import { formatDate } from '../../lib/status';
import { AdminPageShell } from './AdminPageShell';

const OUTCOMES = ['SUCCESS', 'DENIED'];

export function AdminAuditPage() {
  const { user } = useAuth();
  const isGlobal = user?.role === 'ADMINISTRATOR';
  const [filters, setFilters] = useState<AuditLogFilters>({ page: 0, size: 50 });
  const [entries, setEntries] = useState<AuditLogEntry[] | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setEntries(null);
    setError(null);
    const call = isGlobal
      ? api.getAuditLog(filters)
      : api.getOrganisationAuditLog(user?.organisationId ?? '', filters);
    call
      .then((page) => {
        setEntries(page.content);
        setTotalElements(page.totalElements);
      })
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load audit logs.'));
  }, [filters, isGlobal, user]);

  useEffect(() => {
    load();
  }, [load]);

  function apply(patch: Partial<AuditLogFilters>) {
    setFilters((prev) => ({ ...prev, ...patch, page: patch.page ?? 0 }));
  }

  return (
    <AdminPageShell
      title="Audit logs"
      description={
        isGlobal
          ? 'Authorized global event history: actor, action, timestamp, target and outcome, with filters (FR-10.x, P22).'
          : 'Event history for your organization: membership changes, assignments and reports (FR-10.x, P22).'
      }
    >
      <Card aria-label="Audit filters">
        <form
          className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
          onSubmit={(event) => {
            event.preventDefault();
          }}
        >
          <Field
            label="Action (exact)"
            placeholder="USER_ROLE_CHANGED"
            value={filters.action ?? ''}
            onChange={(event) => apply({ action: event.target.value || undefined })}
          />
          {isGlobal && (
            <Field
              label="Actor user id"
              value={filters.actorId ?? ''}
              onChange={(event) => apply({ actorId: event.target.value || undefined })}
            />
          )}
          <Field
            label="Entity type"
            placeholder="Report / User / Organisation"
            value={filters.entityType ?? ''}
            onChange={(event) => apply({ entityType: event.target.value || undefined })}
          />
          <div>
            <label htmlFor="audit-outcome" className="mb-1 block text-sm font-medium">
              Outcome
            </label>
            <select
              id="audit-outcome"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={filters.outcome ?? ''}
              onChange={(event) => apply({ outcome: event.target.value || undefined })}
            >
              <option value="">Any</option>
              {OUTCOMES.map((outcome) => (
                <option key={outcome} value={outcome}>
                  {outcome}
                </option>
              ))}
            </select>
          </div>
          <Field
            label="From (ISO-8601)"
            placeholder="2026-01-01T00:00:00Z"
            value={filters.from ?? ''}
            onChange={(event) => apply({ from: event.target.value || undefined })}
          />
          <Field
            label="To (ISO-8601)"
            placeholder="2026-12-31T23:59:59Z"
            value={filters.to ?? ''}
            onChange={(event) => apply({ to: event.target.value || undefined })}
          />
          <div className="flex items-end gap-2">
            <Button
              size="sm"
              variant="outline"
              type="button"
              onClick={() =>
                setFilters({ page: 0, size: 50 })
              }
            >
              Reset
            </Button>
          </div>
        </form>
      </Card>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !entries && <LoadingState label="Loading audit log…" />}
      {entries && entries.length === 0 && (
        <EmptyState title="No audit events match these filters." />
      )}
      {entries && entries.length > 0 && (
        <Card padded={false} aria-label="Audit entries">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line bg-canvas text-xs uppercase text-muted">
                  <th className="px-4 py-3">Timestamp</th>
                  <th className="px-4 py-3">Actor</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Target</th>
                  <th className="px-4 py-3">Outcome</th>
                  <th className="px-4 py-3">Details</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => (
                  <tr key={entry.id} className="border-b border-line/60 align-top">
                    <td className="px-4 py-3 whitespace-nowrap text-muted">
                      {formatDate(entry.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      {entry.actorEmail ?? entry.actorId ?? '—'}
                    </td>
                    <td className="px-4 py-3 font-medium text-brand-black">{entry.action}</td>
                    <td className="px-4 py-3 text-muted">
                      {entry.entityType ?? '—'}
                      {entry.entityId ? ` · ${entry.entityId.slice(0, 8)}…` : ''}
                    </td>
                    <td className="px-4 py-3">
                      {entry.outcome ? (
                        <Badge tone={entry.outcome === 'SUCCESS' ? 'success' : 'warning'}>
                          {entry.outcome}
                        </Badge>
                      ) : (
                        <span className="text-xs text-muted">legacy</span>
                      )}
                    </td>
                    <td className="max-w-xs truncate px-4 py-3 text-muted" title={entry.details ?? ''}>
                      {entry.details ?? '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
      {entries && entries.length > 0 && (
        <div className="flex items-center justify-between text-sm text-muted">
          <span>
            Page {(filters.page ?? 0) + 1} · {totalElements} events
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
              disabled={(filters.page ?? 0) + 1 >= Math.max(1, Math.ceil(totalElements / (filters.size ?? 50)))}
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
