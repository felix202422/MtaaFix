import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, isApiError } from '../../lib/api';
import type { AdminDashboard, Organisation, OrganisationMember, Report } from '../../lib/types';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  useToast,
} from '../../design-system';
import { statusLabel, statusTone, formatDate } from '../../lib/status';
import { AdminPageShell } from './AdminPageShell';

export function AdminAssignmentsPage() {
  const { push } = useToast();
  const [unassigned, setUnassigned] = useState<Report[] | null>(null);
  const [assigned, setAssigned] = useState<Report[] | null>(null);
  const [workload, setWorkload] = useState<AdminDashboard['workload'] | null>(null);
  const [organisations, setOrganisations] = useState<Organisation[]>([]);
  const [selectedOrg, setSelectedOrg] = useState<Record<string, string>>({});
  const [members, setMembers] = useState<Record<string, OrganisationMember[]>>({});
  const [selectedWorker, setSelectedWorker] = useState<Record<string, string>>({});
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setError(null);
    Promise.all([
      api.listStaffReports({ status: 'VERIFIED', assignment: 'unassigned', size: 50 }),
      api.listStaffReports({ assignment: 'assigned', size: 50 }),
      api.getAdminDashboard(),
    ])
      .then(([unassignedPage, assignedPage, dashboard]) => {
        setUnassigned(unassignedPage.content);
        setAssigned(
          assignedPage.content.filter((report) =>
            ['ASSIGNED', 'IN_PROGRESS', 'RESOLUTION_REVIEW'].includes(report.status),
          ),
        );
        setWorkload(dashboard.workload);
      })
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load assignments.'));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    api
      .listOrganisations()
      .then(setOrganisations)
      .catch(() => setOrganisations([]));
  }, []);

  function orgFor(reportId: string) {
    return selectedOrg[reportId] ?? '';
  }

  function workerFor(reportId: string) {
    return selectedWorker[reportId] ?? '';
  }

  useEffect(() => {
    const pending = Object.entries(selectedOrg).filter(
      ([reportId, orgId]) => orgId && !members[reportId],
    );
    pending.forEach(([reportId, orgId]) => {
      api
        .listOrganisationMembers(orgId)
        .then((list) => setMembers((prev) => ({ ...prev, [reportId]: list })))
        .catch(() => setMembers((prev) => ({ ...prev, [reportId]: [] })));
    });
  }, [selectedOrg, members]);

  async function assign(report: Report) {
    const orgId = orgFor(report.id);
    const workerId = workerFor(report.id);
    if (!orgId || !workerId) return;
    setBusyId(report.id);
    try {
      await api.reportAssign(report.id, { assigneeId: workerId, organisationId: orgId });
      push(`Assigned ${report.code}.`, 'success');
      load();
    } catch (err) {
      push(isApiError(err) ? err.message : 'Assignment failed.', 'error');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <AdminPageShell
      title="Assignment management"
      description="Unassigned verified reports, organisation and worker selection, reassignment and workload (P18)."
    >
      {error && <ErrorState message={error} onRetry={load} />}

      <Card aria-label="Workload">
        <h2 className="mb-3 text-base font-semibold text-brand-black">Workload</h2>
        {!workload ? (
          <LoadingState label="Loading workload…" />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <p className="text-sm text-muted">
              <span className="text-2xl font-bold text-brand-black">{workload.unassigned}</span>{' '}
              unassigned
            </p>
            <p className="text-sm text-muted">
              <span className="text-2xl font-bold text-brand-black">{workload.assigned}</span>{' '}
              assigned
            </p>
            <div>
              <p className="mb-1 text-sm font-medium">By organisation</p>
              {workload.byOrganisation.length === 0 ? (
                <p className="text-sm text-muted">None yet.</p>
              ) : (
                <ul className="text-sm">
                  {workload.byOrganisation.map((org) => (
                    <li key={org.organisationId ?? org.name} className="flex justify-between">
                      <span>{org.name}</span>
                      <span className="font-semibold">{org.assignedCount}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </Card>

      <section aria-label="Unassigned verified reports">
        <h2 className="mb-3 text-lg font-semibold text-brand-black">
          Unassigned verified reports
        </h2>
        {!unassigned && <LoadingState />}
        {unassigned && unassigned.length === 0 && (
          <EmptyState title="Every verified report is assigned." />
        )}
        {unassigned && unassigned.length > 0 && (
          <div className="flex flex-col gap-4">
            {unassigned.map((report) => (
              <Card key={report.id} aria-label={`Assign ${report.code}`}>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                  <div>
                    <Link
                      to={`/admin/reports/${report.id}`}
                      className="font-semibold text-brand-blue hover:underline"
                    >
                      {report.code}
                    </Link>{' '}
                    <span className="font-semibold text-brand-black">{report.title}</span>
                    <div className="mt-1 flex items-center gap-2 text-sm">
                      <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
                      <span className="text-muted">{formatDate(report.reportedAt)}</span>
                    </div>
                  </div>
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
                    <div>
                      <label
                        htmlFor={`org-${report.id}`}
                        className="mb-1 block text-sm font-medium"
                      >
                        Organisation
                      </label>
                      <select
                        id={`org-${report.id}`}
                        className="rounded-lg border border-line bg-surface px-3 py-2 text-sm"
                        value={orgFor(report.id)}
                        onChange={(event) => {
                          const value = event.target.value;
                          setSelectedOrg((prev) => ({ ...prev, [report.id]: value }));
                          setSelectedWorker((prev) => ({ ...prev, [report.id]: '' }));
                        }}
                      >
                        <option value="">Select…</option>
                        {organisations
                          .filter((org) => org.active)
                          .map((org) => (
                            <option key={org.id} value={org.id}>
                              {org.name}
                            </option>
                          ))}
                      </select>
                    </div>
                    <div>
                      <label
                        htmlFor={`worker-${report.id}`}
                        className="mb-1 block text-sm font-medium"
                      >
                        Field worker
                      </label>
                      <select
                        id={`worker-${report.id}`}
                        className="rounded-lg border border-line bg-surface px-3 py-2 text-sm"
                        value={workerFor(report.id)}
                        onChange={(event) =>
                          setSelectedWorker((prev) => ({
                            ...prev,
                            [report.id]: event.target.value,
                          }))
                        }
                      >
                        <option value="">Select…</option>
                        {(members[report.id] ?? [])
                          .filter((member) => member.role === 'FIELD_WORKER' && member.active)
                          .map((member) => (
                            <option key={member.userId} value={member.userId}>
                              {member.fullName}
                            </option>
                          ))}
                      </select>
                    </div>
                    <Button
                      size="sm"
                      disabled={busyId === report.id || !orgFor(report.id) || !workerFor(report.id)}
                      onClick={() => void assign(report)}
                    >
                      Assign
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </section>

      <section aria-label="Active assignments">
        <h2 className="mb-3 text-lg font-semibold text-brand-black">Active assignments</h2>
        {!assigned && <LoadingState />}
        {assigned && assigned.length === 0 && <EmptyState title="No active assignments." />}
        {assigned && assigned.length > 0 && (
          <Card padded={false}>
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line bg-canvas text-xs uppercase text-muted">
                  <th className="px-4 py-3">Reference</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Worker</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {assigned.map((report) => (
                  <tr key={report.id} className="border-b border-line/60">
                    <td className="px-4 py-3 font-medium">{report.code}</td>
                    <td className="px-4 py-3">
                      <Badge tone={statusTone(report.status)}>{statusLabel(report.status)}</Badge>
                    </td>
                    <td className="px-4 py-3 text-muted">{report.assigneeId ?? '—'}</td>
                    <td className="px-4 py-3">
                      <Link
                        to={`/admin/reports/${report.id}`}
                        className="text-brand-blue hover:underline"
                      >
                        Open / reassign
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}
      </section>
    </AdminPageShell>
  );
}
