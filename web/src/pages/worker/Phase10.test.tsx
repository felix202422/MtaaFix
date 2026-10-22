import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderRoute } from '../../test/renderWithRouter';

const fetchMock = vi.fn();

const worker = {
  id: 'w-1',
  email: 'worker@example.com',
  fullName: 'Neema Kilonzo',
  mobile: '+255700000004',
  active: true,
  role: 'FIELD_WORKER',
  organisationId: 'org-1',
};

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

function signIn(user: { id: string; role: string }) {
  localStorage.setItem('mtaafix.accessToken', 'access-token');
  localStorage.setItem('mtaafix.refreshToken', 'refresh-token');
  return user;
}

function stubFetch(handlers: Record<string, (url: string, init?: RequestInit) => Response>) {
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    for (const [pattern, handler] of Object.entries(handlers)) {
      if (url.includes(pattern)) return Promise.resolve(handler(url, init));
    }
    return Promise.resolve(json({}));
  });
}

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

const assignmentPending = {
  id: 'a-1',
  assigneeId: 'w-1',
  organisationId: 'org-1',
  status: 'PENDING',
  reportId: 'r-1',
  reportCode: 'MTF-000123',
  reportTitle: 'Pothole on Nyerere Road',
  assignedAt: '2026-10-16T09:00:00Z',
  completedAt: null,
};

const assignmentInProgress = {
  ...assignmentPending,
  id: 'a-2',
  status: 'IN_PROGRESS',
  reportId: 'r-2',
  reportCode: 'MTF-000124',
  reportTitle: 'Blocked drain at Makonde',
};

const assignmentCompleted = {
  ...assignmentPending,
  id: 'a-3',
  status: 'COMPLETED',
  reportId: 'r-3',
  reportCode: 'MTF-000125',
  reportTitle: 'Broken streetlight',
  completedAt: '2026-10-16T15:00:00Z',
};

function makeReport(overrides: Record<string, unknown> = {}) {
  return {
    id: 'r-1',
    code: 'MTF-000123',
    title: 'Pothole on Nyerere Road',
    description: 'Deep pothole right in front of the market entrance.',
    location: { latitude: -6.8229, longitude: 39.2084 },
    reportedAt: '2026-10-14T08:00:00Z',
    status: 'ASSIGNED',
    score: 5,
    categoryId: 'c-1',
    assignmentId: 'a-1',
    organisationId: 'org-1',
    assigneeId: 'w-1',
    duplicateOfId: null,
    statusHistory: [
      {
        fromStatus: 'VERIFIED',
        toStatus: 'ASSIGNED',
        userId: 'admin-1',
        timestamp: '2026-10-16T09:00:00Z',
        comment: null,
        reasonCode: null,
      },
    ],
    media: [],
    comments: [],
    ...overrides,
  };
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

describe('field-worker route guards', () => {
  it('redirects unauthenticated visitors to login', async () => {
    stubFetch({});
    renderRoute('/worker');
    expect(await screen.findByRole('heading', { level: 1, name: /sign in/i })).toBeInTheDocument();
  });

  it('redirects citizens away from /worker', async () => {
    signIn(citizen);
    stubFetch({
      '/auth/me': () => json(citizen),
      '/dashboard/me': () => json(citizenDashboard),
    });
    renderRoute('/worker');
    await waitFor(() => {
      expect(screen.queryByRole('heading', { level: 1, name: /my tasks/i })).toBeNull();
    });
  });

  it('lets a field worker open the dashboard', async () => {
    signIn(worker);
    stubFetch({
      '/auth/me': () => json(worker),
      '/assignments/me': () =>
        json({ totalElements: 1, totalPages: 1, page: 0, size: 100, content: [assignmentPending] }),
    });
    renderRoute('/worker');
    expect(
      await screen.findByRole('heading', { level: 1, name: /my tasks/i }),
    ).toBeInTheDocument();
  });

  it('shows the My tasks link in the user menu for workers only', async () => {
    signIn(worker);
    stubFetch({
      '/auth/me': () => json(worker),
      '/stats/public': () =>
        json({ totalReports: 0, submittedReports: 0, resolvedReports: 0, categories: 0 }),
      '/reports/public': () => json({ totalElements: 0, totalPages: 1, page: 0, size: 20, content: [] }),
      '/dashboard/me': () => json(citizenDashboard),
    });
    renderRoute('/');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /account menu/i }));
    expect(await screen.findByRole('menuitem', { name: /my tasks/i })).toHaveAttribute(
      'href',
      '/worker',
    );

    cleanup();
    localStorage.clear();
    signIn(citizen);
    stubFetch({
      '/auth/me': () => json(citizen),
      '/stats/public': () =>
        json({ totalReports: 0, submittedReports: 0, resolvedReports: 0, categories: 0 }),
      '/reports/public': () => json({ totalElements: 0, totalPages: 1, page: 0, size: 20, content: [] }),
    });
    renderRoute('/');
    await user.click(await screen.findByRole('button', { name: /account menu/i }));
    expect(screen.queryByRole('menuitem', { name: /my tasks/i })).toBeNull();
  });
});

describe('worker dashboard (P25)', () => {
  it('groups assignments into new, in progress and completed with task links', async () => {
    signIn(worker);
    stubFetch({
      '/auth/me': () => json(worker),
      '/assignments/me': () =>
        json({
          totalElements: 3,
          totalPages: 1,
          page: 0,
          size: 100,
          content: [assignmentPending, assignmentInProgress, assignmentCompleted],
        }),
    });
    renderRoute('/worker');
    expect(await screen.findByRole('heading', { name: /new assignments/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /work in progress/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /completed tasks/i })).toBeInTheDocument();
    expect(screen.getByText('Pothole on Nyerere Road')).toBeInTheDocument();
    expect(screen.getByText('Blocked drain at Makonde')).toBeInTheDocument();
    expect(screen.getByText('Broken streetlight')).toBeInTheDocument();
    const links = await screen.findAllByRole('link', { name: /open task/i });
    expect(links[0]).toHaveAttribute('href', '/worker/report/r-1');
    expect(screen.getByText(/MTF-000123 · Assigned .* Oct 2026/)).toBeInTheDocument();
  });

  it('shows an empty state when the worker has no assignments', async () => {
    signIn(worker);
    stubFetch({
      '/auth/me': () => json(worker),
      '/assignments/me': () =>
        json({ totalElements: 0, totalPages: 1, page: 0, size: 100, content: [] }),
    });
    renderRoute('/worker');
    expect(await screen.findByText(/no assignments yet/i)).toBeInTheDocument();
  });
});

describe('worker task details (P26)', () => {
  it('starts an assigned task and then shows the completion form', async () => {
    signIn(worker);
    let startMethod: string | undefined;
    stubFetch({
      '/auth/me': () => json(worker),
      '/categories': () => json([]),
      '/reports/r-1/start': (_url, init) => {
        startMethod = init?.method;
        return json(makeReport({ status: 'IN_PROGRESS' }));
      },
      '/reports/r-1': () => json(makeReport()),
    });
    renderRoute('/worker/report/r-1');
    const user = userEvent.setup();
    const start = await screen.findByRole('button', { name: /start work/i });
    await user.click(start);
    await waitFor(() => {
      expect(startMethod).toBe('POST');
    });
    expect(await screen.findByText(/submit completion claim/i)).toBeInTheDocument();
    expect(screen.getByText(/no photo attached yet/i)).toBeInTheDocument();
  });

  it('surfaces the server rejection when a claim has no photo', async () => {
    signIn(worker);
    stubFetch({
      '/auth/me': () => json(worker),
      '/categories': () => json([]),
      '/reports/r-2/complete': () =>
        json(
          {
            code: 'VALIDATION_ERROR',
            message: 'A completion claim requires at least one attached photo (T11 evidence).',
          },
          400,
        ),
      '/reports/r-2': () => json(makeReport({ id: 'r-2', status: 'IN_PROGRESS' })),
    });
    renderRoute('/worker/report/r-2');
    const user = userEvent.setup();
    const submit = await screen.findByRole('button', { name: /submit completion/i });
    await user.click(submit);
    expect(await screen.findByText(/T11 evidence/i)).toBeInTheDocument();
  });

  it('never offers review-only controls to the worker', async () => {
    signIn(worker);
    stubFetch({
      '/auth/me': () => json(worker),
      '/categories': () => json([]),
      '/reports/r-1': () => json(makeReport({ status: 'RESOLUTION_REVIEW' })),
    });
    renderRoute('/worker/report/r-1');
    expect(
      await screen.findByText(/completion claim submitted/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/reviewer is checking your claim/i)).toBeInTheDocument();
    for (const name of [/approve completion/i, /verify/i, /reject/i, /return to work/i]) {
      expect(screen.queryByRole('button', { name })).toBeNull();
    }
    expect(screen.queryByRole('button', { name: /start work/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /submit completion/i })).toBeNull();
  });

  it('adds a progress update through the comments API', async () => {
    signIn(worker);
    let commentBody: unknown = null;
    stubFetch({
      '/auth/me': () => json(worker),
      '/categories': () => json([]),
      '/reports/r-1/comments': (_url, init) => {
        commentBody = init?.body;
        return json(
          makeReport({
            comments: [
              {
                id: 'cm-1',
                content: 'Materials ordered, arriving tomorrow.',
                userId: 'w-1',
                timestamp: '2026-10-16T12:00:00Z',
              },
            ],
          }),
        );
      },
      '/reports/r-1': () => json(makeReport({ status: 'IN_PROGRESS' })),
    });
    renderRoute('/worker/report/r-1');
    const user = userEvent.setup();
    const textarea = await screen.findByLabelText(/add a progress update/i);
    await user.type(textarea, 'Materials ordered, arriving tomorrow.');
    await user.click(screen.getByRole('button', { name: /post update/i }));
    await waitFor(() => {
      expect(commentBody).toBe(
        JSON.stringify({ content: 'Materials ordered, arriving tomorrow.' }),
      );
    });
  });
});
