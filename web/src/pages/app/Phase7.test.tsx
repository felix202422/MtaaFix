import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderRoute } from '../../test/renderWithRouter';

const fetchMock = vi.fn();

const citizen = {
  id: 'u-1',
  email: 'citizen@example.com',
  fullName: 'Asha Juma',
  mobile: '+255712345678',
  active: true,
  role: 'CITIZEN',
  organisationId: null,
};

function authOk(user = citizen) {
  return new Response(
    JSON.stringify({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      accessTokenExpiresIn: 900000,
      user,
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  );
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
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

describe('login flow', () => {
  it('signs in and stores tokens, then lands on the dashboard', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/auth/login')) return Promise.resolve(authOk());
      if (url.includes('/dashboard/me')) {
        return Promise.resolve(
          json({
            initials: 'AJ',
            name: 'Asha Juma',
            role: 'CITIZEN',
            totalReports: 1,
            submittedReports: 1,
            verifiedReports: 0,
            closedReports: 0,
            needsInfoCount: 0,
            resolvedAwaitingConfirmCount: 0,
            activeCount: 1,
            attentionReports: [],
          }),
        );
      }
      return Promise.resolve(json({}));
    });

    renderRoute('/login');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/email/i), 'citizen@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(localStorage.getItem('mtaafix.accessToken')).toBe('access-token');
      expect(localStorage.getItem('mtaafix.refreshToken')).toBe('refresh-token');
    });
    expect(
      await screen.findByRole('heading', { level: 1, name: /asha juma/i }),
    ).toBeInTheDocument();
  });

  it('shows the API error message when credentials are rejected', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(
        json({ code: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' }, 401),
      ),
    );

    renderRoute('/login');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/email/i), 'wrong@example.com');
    await user.type(screen.getByLabelText(/password/i), 'badpassword');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText(/invalid email or password/i)).toBeInTheDocument();
  });
});

describe('profile page', () => {
  it('updates name and mobile', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/me') && init?.method === 'PUT') {
        return Promise.resolve(
          json({ ...citizen, fullName: 'Asha Updated', mobile: '+255700000001' }),
        );
      }
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      return Promise.resolve(json({}));
    });

    renderRoute('/app/profile');
    const user = userEvent.setup();
    const nameInput = await screen.findByLabelText(/full name/i);
    await user.clear(nameInput);
    await user.type(nameInput, 'Asha Updated');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/full name/i)).toHaveValue('Asha Updated');
    });
  });

  it('rejects a blank name', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      return Promise.resolve(json({}));
    });

    renderRoute('/app/profile');
    const user = userEvent.setup();
    const nameInput = await screen.findByLabelText(/full name/i);
    await user.clear(nameInput);
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(await screen.findByText(/full name is required/i)).toBeInTheDocument();
  });
});

describe('notifications page', () => {
  it('marks a single notification as read', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    let readCalled = false;
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      if (url.includes('/notifications') && init?.method === 'POST') {
        readCalled = true;
        return Promise.resolve(json({ success: true }));
      }
      if (url.includes('/notifications')) {
        return Promise.resolve(
          json({
            totalElements: 1,
            totalPages: 1,
            page: 0,
            size: 20,
            content: [
              {
                id: 'n-1',
                title: 'Report verified',
                body: 'Your report MTF-2026-000001 was verified.',
                type: 'REPORT_VERIFIED',
                isRead: false,
                timestamp: '2026-10-01T09:00:00Z',
                reportId: 'r-1',
              },
            ],
          }),
        );
      }
      return Promise.resolve(json({}));
    });

    renderRoute('/app/notifications');
    const user = userEvent.setup();
    await screen.findByText(/report verified/i);
    await user.click(screen.getByRole('button', { name: /mark read/i }));

    await waitFor(() => {
      expect(readCalled).toBe(true);
    });
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /mark read/i })).not.toBeInTheDocument();
    });
  });
});

describe('ownership errors', () => {
  it('surfaces a 403 from a lifecycle action', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      if (url.includes('/cancel') && init?.method === 'POST') {
        return Promise.resolve(
          json({ code: 'FORBIDDEN', message: 'You do not own this report.' }, 403),
        );
      }
      if (url.includes('/reports/r-1')) {
        return Promise.resolve(
          json({
            id: 'r-1',
            code: 'MTF-2026-000001',
            title: 'Broken streetlight',
            description: null,
            location: { latitude: -6.82, longitude: 39.21 },
            reportedAt: '2026-10-01T09:00:00Z',
            status: 'SUBMITTED',
            score: null,
            categoryId: 'cat-1',
            assignmentId: null,
            organisationId: null,
            assigneeId: null,
            duplicateOfId: null,
            statusHistory: [
              {
                fromStatus: null,
                toStatus: 'SUBMITTED',
                userId: 'u-1',
                timestamp: '2026-10-01T09:00:00Z',
                comment: null,
                reasonCode: null,
              },
            ],
            media: [],
            comments: [],
          }),
        );
      }
      return Promise.resolve(json({}));
    });

    renderRoute('/app/reports/r-1');
    const user = userEvent.setup();
    const cancelBtn = await screen.findByRole('button', { name: /cancel report/i });
    await user.click(cancelBtn);

    expect(await screen.findByText(/you do not own this report/i)).toBeInTheDocument();
  });
});
