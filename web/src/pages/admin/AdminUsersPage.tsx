import { useCallback, useEffect, useState } from 'react';
import { api, isApiError } from '../../lib/api';
import type { AdminUser, UserRole } from '../../lib/types';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Field,
  LoadingState,
  useToast,
} from '../../design-system';
import { AdminPageShell } from './AdminPageShell';

const ROLES: UserRole[] = [
  'CITIZEN',
  'MODERATOR',
  'ADMINISTRATOR',
  'FIELD_WORKER',
  'ORGANIZATION_ADMIN',
];

const ROLE_LABELS: Record<UserRole, string> = {
  CITIZEN: 'Citizen',
  MODERATOR: 'Moderator',
  ADMINISTRATOR: 'Administrator',
  FIELD_WORKER: 'Field worker',
  ORGANIZATION_ADMIN: 'Organization admin',
};

export function AdminUsersPage() {
  const { push } = useToast();
  const [q, setQ] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [activeFilter, setActiveFilter] = useState('');
  const [users, setUsers] = useState<AdminUser[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(() => {
    setUsers(null);
    setError(null);
    api
      .searchUsers({
        q: q.trim() || undefined,
        role: roleFilter || undefined,
        active: activeFilter === '' ? undefined : activeFilter === 'active',
        size: 50,
      })
      .then((page) => setUsers(page.content))
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load users.'));
  }, [q, roleFilter, activeFilter]);

  useEffect(() => {
    load();
  }, [load]);

  async function mutate(user: AdminUser, action: () => Promise<AdminUser>, message: string) {
    setBusyId(user.id);
    try {
      await action();
      push(message, 'success');
      load();
    } catch (err) {
      push(isApiError(err) ? err.message : 'The action failed.', 'error');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <AdminPageShell
      title="User management"
      description="Search accounts, review status, manage roles and activate or deactivate users — every privileged action is audited (FR-12.1, P19)."
    >
      <Card aria-label="User filters">
        <form
          className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
          onSubmit={(event) => {
            event.preventDefault();
            load();
          }}
        >
          <Field
            label="Search email or name"
            value={q}
            onChange={(event) => setQ(event.target.value)}
          />
          <div>
            <label htmlFor="users-role" className="mb-1 block text-sm font-medium">
              Role
            </label>
            <select
              id="users-role"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={roleFilter}
              onChange={(event) => setRoleFilter(event.target.value)}
            >
              <option value="">All roles</option>
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {ROLE_LABELS[role]}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="users-active" className="mb-1 block text-sm font-medium">
              Account status
            </label>
            <select
              id="users-active"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={activeFilter}
              onChange={(event) => setActiveFilter(event.target.value)}
            >
              <option value="">Any</option>
              <option value="active">Active</option>
              <option value="inactive">Deactivated</option>
            </select>
          </div>
          <div className="flex items-end">
            <Button type="submit" size="sm">
              Search
            </Button>
          </div>
        </form>
      </Card>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !users && <LoadingState label="Loading users…" />}
      {users && users.length === 0 && <EmptyState title="No users match these filters." />}
      {users && users.length > 0 && (
        <Card padded={false} aria-label="Users table">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line bg-canvas text-xs uppercase text-muted">
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Role</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id} className="border-b border-line/60">
                    <td className="px-4 py-3 font-medium text-brand-black">{user.fullName}</td>
                    <td className="px-4 py-3 text-muted">{user.email}</td>
                    <td className="px-4 py-3">
                      <label className="sr-only" htmlFor={`role-${user.id}`}>
                        Role for {user.email}
                      </label>
                      <select
                        id={`role-${user.id}`}
                        className="rounded-lg border border-line bg-surface px-2 py-1 text-sm"
                        value={user.role}
                        disabled={busyId === user.id}
                        onChange={(event) => {
                          const nextRole = event.target.value as UserRole;
                          void mutate(
                            user,
                            () => api.changeUserRole(user.id, nextRole),
                            `Role updated to ${ROLE_LABELS[nextRole]}.`,
                          );
                        }}
                      >
                        {ROLES.map((role) => (
                          <option key={role} value={role}>
                            {ROLE_LABELS[role]}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone={user.active ? 'success' : 'error'}>
                        {user.active ? 'Active' : 'Deactivated'}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      <Button
                        size="sm"
                        variant={user.active ? 'outline' : 'secondary'}
                        disabled={busyId === user.id}
                        onClick={() =>
                          void mutate(
                            user,
                            () => api.setUserActive(user.id, !user.active),
                            user.active ? 'Account deactivated.' : 'Account activated.',
                          )
                        }
                      >
                        {user.active ? 'Deactivate' : 'Activate'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
      <p className="text-xs text-muted">
        Organisation roles (Organization admin, Field worker) must be granted through an
        organization membership on the Organizations page; the API rejects direct grants without
        membership.
      </p>
    </AdminPageShell>
  );
}
