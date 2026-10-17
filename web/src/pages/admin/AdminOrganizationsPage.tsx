import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../../app/AuthProvider';
import { api, isApiError } from '../../lib/api';
import type { Organisation, OrganisationMember } from '../../lib/types';
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

export function AdminOrganizationsPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const isAdmin = user?.role === 'ADMINISTRATOR';
  const [organisations, setOrganisations] = useState<Organisation[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(user?.organisationId ?? null);
  const [members, setMembers] = useState<OrganisationMember[] | null>(null);

  const [newName, setNewName] = useState('');
  const [newSlug, setNewSlug] = useState('');
  const [memberEmail, setMemberEmail] = useState('');
  const [memberRole, setMemberRole] = useState<'FIELD_WORKER' | 'ORGANIZATION_ADMIN'>(
    'FIELD_WORKER',
  );
  const [foundUser, setFoundUser] = useState<{ id: string; email: string; fullName: string } | null>(
    null,
  );

  const load = useCallback(() => {
    setOrganisations(null);
    setError(null);
    api
      .listOrganisations()
      .then((list) => {
        setOrganisations(list);
        setSelectedOrgId((prev) => prev ?? list[0]?.id ?? null);
      })
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load organisations.'));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!selectedOrgId) {
      setMembers(null);
      return;
    }
    setMembers(null);
    api
      .listOrganisationMembers(selectedOrgId)
      .then(setMembers)
      .catch((err) => {
        setMembers([]);
        push(isApiError(err) ? err.message : 'Could not load members.', 'error');
      });
  }, [selectedOrgId, push]);

  async function create(event: FormEvent) {
    event.preventDefault();
    try {
      const created = await api.createOrganisation({
        name: newName.trim(),
        slug: newSlug.trim(),
      });
      push('Organisation created.', 'success');
      setNewName('');
      setNewSlug('');
      setSelectedOrgId(created.id);
      load();
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not create the organisation.', 'error');
    }
  }

  async function findMember(event: FormEvent) {
    event.preventDefault();
    setFoundUser(null);
    try {
      const page = await api.searchUsers({ q: memberEmail.trim(), size: 5 });
      const match = page.content.find((candidate) => candidate.email === memberEmail.trim());
      if (!match) {
        push('No user with that email was found.', 'error');
        return;
      }
      setFoundUser({ id: match.id, email: match.email, fullName: match.fullName });
    } catch (err) {
      push(isApiError(err) ? err.message : 'Search failed.', 'error');
    }
  }

  async function addMember() {
    if (!selectedOrgId || !foundUser) return;
    try {
      await api.addOrganisationMember(selectedOrgId, {
        userId: foundUser.id,
        role: memberRole,
      });
      push(`${foundUser.fullName} added as ${memberRole === 'FIELD_WORKER' ? 'field worker' : 'organization admin'}.`, 'success');
      setMemberEmail('');
      setFoundUser(null);
      setMembers(await api.listOrganisationMembers(selectedOrgId));
      load();
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not add the member.', 'error');
    }
  }

  return (
    <AdminPageShell
      title="Organizations and field workers"
      description="Organisation management, team membership, eligibility and account status (FR-5.3, B-X.3, P21)."
    >
      {isAdmin && (
        <Card aria-label="Create organisation">
          <h2 className="mb-3 text-base font-semibold text-brand-black">New organisation</h2>
          <form className="grid gap-4 sm:grid-cols-3" onSubmit={create}>
            <Field
              label="Name"
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
            />
            <Field
              label="Slug"
              hint="lowercase letters, digits, single dashes"
              value={newSlug}
              onChange={(event) => setNewSlug(event.target.value)}
            />
            <div className="flex items-end">
              <Button type="submit" disabled={!newName.trim() || !newSlug.trim()}>
                Create
              </Button>
            </div>
          </form>
        </Card>
      )}

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !organisations && <LoadingState label="Loading organisations…" />}
      {organisations && organisations.length === 0 && (
        <EmptyState title="No organisations yet." />
      )}
      {organisations && organisations.length > 0 && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card aria-label="Organisations">
            <h2 className="mb-3 text-base font-semibold text-brand-black">Organisations</h2>
            <ul className="flex flex-col gap-2">
              {organisations.map((org) => (
                <li key={org.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedOrgId(org.id)}
                    className={`flex w-full items-center justify-between rounded-lg border px-3 py-2 text-left text-sm transition-colors ${
                      selectedOrgId === org.id
                        ? 'border-brand-green-deep bg-canvas'
                        : 'border-line hover:bg-canvas'
                    }`}
                  >
                    <span>
                      <span className="font-semibold text-brand-black">{org.name}</span>{' '}
                      <span className="text-muted">({org.slug})</span>
                      {org.memberCount > 0 && (
                        <span className="ml-2 text-xs text-muted">{org.memberCount} members</span>
                      )}
                    </span>
                    <Badge tone={org.active ? 'success' : 'error'}>
                      {org.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </button>
                  {isAdmin && (
                    <div className="mt-1 flex gap-2 pl-3">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          void api
                            .setOrganisationActive(org.id, !org.active)
                            .then(() => {
                              push(
                                org.active
                                  ? 'Organisation deactivated.'
                                  : 'Organisation activated.',
                                'success',
                              );
                              load();
                            })
                            .catch((err) =>
                              push(
                                isApiError(err) ? err.message : 'Could not update status.',
                                'error',
                              ),
                            );
                        }}
                      >
                        {org.active ? 'Deactivate' : 'Activate'}
                      </Button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          </Card>

          <Card aria-label="Members">
            <h2 className="mb-3 text-base font-semibold text-brand-black">Team membership</h2>
            {!selectedOrgId && <p className="text-sm text-muted">Select an organisation.</p>}
            {selectedOrgId && !members && <LoadingState label="Loading members…" />}
            {selectedOrgId && members && (
              <>
                {members.length === 0 ? (
                  <p className="mb-3 text-sm text-muted">No members yet.</p>
                ) : (
                  <ul className="mb-4 flex flex-col gap-2 text-sm">
                    {members.map((member) => (
                      <li
                        key={member.userId}
                        className="flex items-center justify-between gap-3 rounded-lg bg-canvas px-3 py-2"
                      >
                        <span>
                          <span className="font-medium text-brand-black">{member.fullName}</span>{' '}
                          <span className="text-muted">({member.email})</span>
                          <span className="ml-2 text-xs text-muted">
                            {member.role === 'FIELD_WORKER' ? 'Field worker' : 'Org admin'}
                            {member.active ? '' : ' · deactivated'}
                          </span>
                        </span>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            void api
                              .removeOrganisationMember(selectedOrgId, member.userId)
                              .then(() => {
                                push('Member removed.', 'success');
                                return api.listOrganisationMembers(selectedOrgId);
                              })
                              .then(setMembers)
                              .catch((err) =>
                                push(
                                  isApiError(err) ? err.message : 'Could not remove the member.',
                                  'error',
                                ),
                              );
                          }}
                        >
                          Remove
                        </Button>
                      </li>
                    ))}
                  </ul>
                )}
                <form className="flex flex-col gap-3" onSubmit={findMember}>
                  <Field
                    label="Add member by email"
                    hint="Find the user, then add them with a role"
                    value={memberEmail}
                    onChange={(event) => {
                      setMemberEmail(event.target.value);
                      setFoundUser(null);
                    }}
                  />
                  <div className="flex flex-wrap items-center gap-2">
                    <Button size="sm" variant="secondary" type="submit">
                      Find user
                    </Button>
                    <label className="sr-only" htmlFor="member-role">
                      Member role
                    </label>
                    <select
                      id="member-role"
                      className="rounded-lg border border-line bg-surface px-2 py-1.5 text-sm"
                      value={memberRole}
                      onChange={(event) =>
                        setMemberRole(event.target.value as 'FIELD_WORKER' | 'ORGANIZATION_ADMIN')
                      }
                    >
                      <option value="FIELD_WORKER">Field worker</option>
                      <option value="ORGANIZATION_ADMIN">Organization admin</option>
                    </select>
                    <Button
                      size="sm"
                      disabled={!foundUser}
                      onClick={() => void addMember()}
                      type="button"
                    >
                      Add member
                    </Button>
                  </div>
                  {foundUser && (
                    <p className="text-sm text-brand-green-deep">
                      Ready to add {foundUser.fullName} ({foundUser.email}) as {memberRole}.
                    </p>
                  )}
                </form>
              </>
            )}
          </Card>
        </div>
      )}
    </AdminPageShell>
  );
}
