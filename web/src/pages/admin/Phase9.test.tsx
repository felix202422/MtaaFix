import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderRoute } from '../../test/renderWithRouter';

const fetchMock = vi.fn();

const admin = {
  id: 'admin-1',
  email: 'admin@example.com',
  fullName: 'Admin Person',
  mobile: '+255700000002',
  active: true,
  role: 'ADMINISTRATOR',
  organisationId: null,
};

const moderator = { ...admin, id: 'mod-1', email: 'mod@example.com', role: 'MODERATOR' };
const citizen = {
  id: 'u-1',
  email: 'citizen@example.com',
  fullName: 'Asha Juma',
  mobile: '+255712345678',
  active: true,
  role: 'CITIZEN',
  organisationId: null,
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function signIn(user: typeof admin) {
  localStorage.setItem('mtaafix.accessToken', 'access-token');
  localStorage.setItem('mtaafix.refreshToken', 'refresh-token');
  return user;
}

const adminDashboard = {
  totalReports: 4,
  byStatus: { SUBMITTED: 2, VERIFIED: 2 },
  newLast7Days: 3,
  activity14d: [{ date: '2026-10-15', count: 2 }],
  byCategory: [{ categoryId: 'c1', name: 'Potholes', count: 4 }],
  moderationQueue: {
    submitted: 2,
    underReview: 0,
    needsInformation: 0,
    resolutionReview: 0,
    items: [],
  },
  workload: {
    unassigned: 2,
    assigned: 2,
    byOrganisation: [{ organisationId: 'o1', name: 'Roads Dept', assignedCount: 2 }],
    byWorker: [],
  },
  attention: { needsInformation: 0, resolvedAwaitingConfirm: 0, needsInfoItems: [] },
  recent: [],
};

const settingsPayload = {
  general: { applicationName: 'mtaafix', applicationTimeZone: 'Africa/Dar_es_Salaam' },
  reporting: {
    maxUploadBytes: 20971520,
    multipartMaxFileSize: '21MB',
    allowedMediaTypes: ['image/jpeg'],
    notifications: { inAppEnabled: true, emailEnabled: false, emailNote: 'post-MVP' },
  },
  security: {
    accessTokenTtlSeconds: 900,
    refreshTokenTtlSeconds: 604800,
    corsAllowedOrigins: 'http://localhost:5173',
  },
  privacy: { locationFuzzing: 'all public locations', rolesSeeingPreciseLocations: ['MODERATOR'] },
  geographic: {
    fuzzMinMeters: 150,
    fuzzMaxMeters: 600,
    fuzzSecretConfigured: false,
    fuzzSecretIsDefault: true,
  },
  integrations: { mapTiles: 'OpenStreetMap', email: 'none', push: 'none' },
};

const userRow = {
  id: 'u-2',
  email: 'worker@example.com',
  fullName: 'Worker One',
  mobile: '+255700000003',
  active: true,
  role: 'CITIZEN',
  organisationId: null,
};

const paged = (content: unknown[]) => ({
  totalElements: content.length,
  totalPages: 1,
  page: 0,
  size: 20,
  content,
});

const citizenDashboard = {
  initials: 'AJ',
  name: 'Asha Juma',
  role: 'CITIZEN',
  totalReports: 0,
  submittedReports: 0,
  verifiedReports: 0,
  closedReports: 0,
  needsInfoCount: 0,
  resolvedAwaitingConfirmCount: 0,
  activeCount: 0,
  attentionReports: [],
};

function stubFetch(handlers: Record<string, (url: string, init?: RequestInit) => Response>) {
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    for (const [pattern, handler] of Object.entries(handlers)) {
      if (url.includes(pattern)) return Promise.resolve(handler(url, init));
    }
    return Promise.resolve(json({}));
  });
}

beforeEach(() => {
  localStorage.clear();
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('admin route guards', () => {
  it('redirects unauthenticated visitors to login', async () => {
    stubFetch({});
    renderRoute('/admin');
    expect(await screen.findByRole('heading', { level: 1, name: /sign in/i })).toBeInTheDocument();
  });

  it('redirects citizens away from /admin and /admin/users', async () => {
    signIn(citizen);
    stubFetch({
      '/auth/me': () => json(citizen),
      '/dashboard/me': () => json(citizenDashboard),
    });
    renderRoute('/admin/users');
    await waitFor(() => {
      expect(screen.queryByRole('heading', { level: 1, name: /user management/i })).toBeNull();
    });
  });

  it('lets a moderator open the admin dashboard but blocks /admin/users', async () => {
    signIn(moderator);
    stubFetch({
      '/auth/me': () => json(moderator),
      '/dashboard/admin': () => json(adminDashboard),
      '/dashboard/me': () => json(citizenDashboard),
    });
    renderRoute('/admin');
    expect(
      await screen.findByRole('heading', { level: 1, name: /admin dashboard/i }),
    ).toBeInTheDocument();

    cleanup();
    renderRoute('/admin/users');
    await waitFor(() => {
      expect(screen.queryByRole('heading', { level: 1, name: /user management/i })).toBeNull();
    });
  });

  it('lets an administrator open user management', async () => {
    signIn(admin);
    stubFetch({
      '/auth/me': () => json(admin),
      '/users': () => json(paged([userRow])),
    });
    renderRoute('/admin/users');
    expect(
      await screen.findByRole('heading', { level: 1, name: /user management/i }),
    ).toBeInTheDocument();
    expect(await screen.findByText('worker@example.com')).toBeInTheDocument();
  });

  it('shows the Admin link in the user menu for staff only', async () => {
    signIn(admin);
    stubFetch({
      '/auth/me': () => json(admin),
      '/stats/public': () =>
        json({ totalReports: 0, submittedReports: 0, resolvedReports: 0, categories: 0 }),
      '/reports/public': () => json(paged([])),
      '/dashboard/admin': () => json(adminDashboard),
    });
    renderRoute('/');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /account menu/i }));
    expect(await screen.findByRole('menuitem', { name: /^admin$/i })).toHaveAttribute(
      'href',
      '/admin',
    );

    cleanup();
    localStorage.clear();
    signIn(citizen);
    stubFetch({
      '/auth/me': () => json(citizen),
      '/stats/public': () =>
        json({ totalReports: 0, submittedReports: 0, resolvedReports: 0, categories: 0 }),
      '/reports/public': () => json(paged([])),
    });
    renderRoute('/');
    await user.click(await screen.findByRole('button', { name: /account menu/i }));
    expect(screen.queryByRole('menuitem', { name: /^admin$/i })).toBeNull();
  });
});

describe('user management (P19)', () => {
  it('changes a user role through the API', async () => {
    signIn(admin);
    let roleCall: unknown = null;
    stubFetch({
      '/users/u-2/role': (_url, init) => {
        roleCall = init?.body;
        return json({ ...userRow, role: 'MODERATOR' });
      },
      '/auth/me': () => json(admin),
      '/users': () => json(paged([userRow])),
    });
    renderRoute('/admin/users');
    const user = userEvent.setup();
    const select = await screen.findByLabelText(/role for worker@example.com/i);
    await user.selectOptions(select, 'MODERATOR');
    await waitFor(() => {
      expect(roleCall).toBe(JSON.stringify({ role: 'MODERATOR' }));
    });
  });

  it('deactivates a user through the API', async () => {
    signIn(admin);
    let activeCall: unknown = null;
    stubFetch({
      '/users/u-2/active': (_url, init) => {
        activeCall = init?.body;
        return json({ ...userRow, active: false });
      },
      '/auth/me': () => json(admin),
      '/users': () => json(paged([userRow])),
    });
    renderRoute('/admin/users');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /deactivate/i }));
    await waitFor(() => {
      expect(activeCall).toBe(JSON.stringify({ active: false }));
    });
  });
});

describe('category management (P20)', () => {
  it('deactivates a category', async () => {
    signIn(admin);
    let deactivateCalled = false;
    stubFetch({
      '/auth/me': () => json(admin),
      '/categories?includeInactive=true': () =>
        json([{ id: 'c1', name: 'Potholes', slug: 'potholes', type: 'PRIMARY', active: true }]),
      '/categories/c1/active': () => {
        deactivateCalled = true;
        return json({ id: 'c1', name: 'Potholes', slug: 'potholes', type: 'PRIMARY', active: false });
      },
    });
    renderRoute('/admin/categories');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /deactivate/i }));
    await waitFor(() => {
      expect(deactivateCalled).toBe(true);
    });
  });
});

describe('audit logs (P22)', () => {
  it('renders filtered entries with actor and outcome', async () => {
    signIn(admin);
    stubFetch({
      '/auth/me': () => json(admin),
      '/audit': () =>
        json(
          paged([
            {
              id: 'a1',
              action: 'USER_ROLE_CHANGED',
              actorId: 'admin-1',
              actorEmail: 'admin@example.com',
              entityType: 'User',
              entityId: 'u-2',
              outcome: 'SUCCESS',
              details: null,
              createdAt: '2026-10-15T10:00:00Z',
            },
          ]),
        ),
    });
    renderRoute('/admin/audit');
    expect(
      await screen.findByRole('heading', { level: 1, name: /audit logs/i }),
    ).toBeInTheDocument();
    expect(await screen.findByText('USER_ROLE_CHANGED')).toBeInTheDocument();
    expect(await screen.findByText('admin@example.com')).toBeInTheDocument();
    expect(screen.getAllByText('SUCCESS').length).toBeGreaterThan(0);
  });
});

describe('system settings (P24)', () => {
  it('shows read-only sections and never leaks secrets', async () => {
    signIn(admin);
    stubFetch({
      '/auth/me': () => json(admin),
      '/settings': () => json(settingsPayload),
    });
    renderRoute('/admin/settings');
    expect(
      await screen.findByRole('heading', { level: 1, name: /system settings/i }),
    ).toBeInTheDocument();
    const main = screen.getByRole('main');
    expect((await within(main).findAllByText(/read-only/i)).length).toBeGreaterThan(0);
    expect(within(main).getByText('Reporting rules')).toBeInTheDocument();
    expect(within(main).getByText('Security')).toBeInTheDocument();
    expect(within(main).getByText('Privacy')).toBeInTheDocument();
    expect(within(main).getByText('Geographic configuration')).toBeInTheDocument();
    expect(within(main).getByText('Integrations')).toBeInTheDocument();
    expect(within(main).getByText(/change in production/)).toBeInTheDocument();
    expect(document.body.textContent).not.toContain('mtaafix-local-dev-geo-fuzz-secret');
  });
});

describe('report management (P15)', () => {
  it('loads staff reports and links to details', async () => {
    signIn(admin);
    stubFetch({
      '/auth/me': () => json(admin),
      '/reports?': () =>
        json(
          paged([
            {
              id: 'r-1',
              code: 'MTF-2026-000001',
              title: 'Pothole on Main St',
              description: null,
              location: { latitude: -6.82, longitude: 39.21, srid: 4326 },
              reportedAt: '2026-10-15T10:00:00Z',
              status: 'SUBMITTED',
              score: null,
              categoryId: 'c1',
              assignmentId: null,
              organisationId: null,
              assigneeId: null,
              duplicateOfId: null,
              statusHistory: [],
              media: [],
              comments: [],
            },
          ]),
        ),
      '/categories': () => json([{ id: 'c1', name: 'Potholes', slug: 'potholes', type: 'PRIMARY' }]),
    });
    renderRoute('/admin/reports');
    expect(
      await screen.findByRole('heading', { level: 1, name: /report management/i }),
    ).toBeInTheDocument();
    const open = await screen.findByRole('link', { name: /open/i });
    expect(open).toHaveAttribute('href', '/admin/reports/r-1');
  });
});
