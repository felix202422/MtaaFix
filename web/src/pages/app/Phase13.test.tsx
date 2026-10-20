import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderRoute } from '../../test/renderWithRouter';

vi.mock('../../components/map/ReportMap', () => ({
  ReportMap: ({
    onLocationPick,
  }: {
    onLocationPick?: (latitude: number, longitude: number) => void;
  }) => (
    <button type="button" onClick={() => onLocationPick?.(-6.82, 39.2)}>
      Pick location on map
    </button>
  ),
}));

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

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function seedAuthed(
  handler?: (
    url: string,
    init?: RequestInit,
  ) => Promise<Response> | Response | undefined,
) {
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
    if (url.includes('/notifications/unread-count')) {
      return Promise.resolve(json({ count: 0 }));
    }
    if (
      url.includes('/notifications/preferences') &&
      init?.method !== 'PUT'
    ) {
      return Promise.resolve(
        json({ emailEnabled: true, pushEnabled: false, pushAvailable: false }),
      );
    }
    return Promise.resolve(handler?.(url, init) ?? json({}));
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

describe('password recovery (FR-1.7)', () => {
  it('requests a reset link and always shows the same confirmation', async () => {
    let forgotBody: unknown = null;
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/password/forgot')) {
        forgotBody = JSON.parse(init?.body as string);
        return Promise.resolve(
          json(
            { success: true, message: 'If that email address is registered, a reset link was sent.' },
            202,
          ),
        );
      }
      return Promise.resolve(json({}));
    });

    renderRoute('/forgot-password');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^email/i), 'asha@example.com');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    expect(
      await screen.findByRole('heading', { name: /check your inbox/i }),
    ).toBeInTheDocument();
    expect(forgotBody).toEqual({ email: 'asha@example.com' });
    expect(screen.getByText(/if an account exists/i)).toBeInTheDocument();
  });

  it('completes a password reset with the token from the link', async () => {
    let resetBody: unknown = null;
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/password/reset')) {
        resetBody = JSON.parse(init?.body as string);
        return Promise.resolve(
          json({ success: true, message: 'Password has been reset' }),
        );
      }
      return Promise.resolve(json({}));
    });

    renderRoute('/reset-password?token=tok-123');
    const user = userEvent.setup();
    expect(await screen.findByLabelText(/reset token/i)).toHaveValue('tok-123');
    await user.type(screen.getByLabelText(/^new password/i), 'NewPass123');
    await user.type(screen.getByLabelText(/confirm new password/i), 'NewPass123');
    await user.click(screen.getByRole('button', { name: /set new password/i }));

    expect(
      await screen.findByRole('heading', { name: /password updated/i }),
    ).toBeInTheDocument();
    expect(resetBody).toEqual({ token: 'tok-123', password: 'NewPass123' });
  });

  it('validates the reset form before calling the API', async () => {
    renderRoute('/reset-password?token=tok-123');
    const user = userEvent.setup();
    await screen.findByLabelText(/reset token/i);
    await user.type(screen.getByLabelText(/^new password/i), 'NewPass123');
    await user.type(screen.getByLabelText(/confirm new password/i), 'Different1');
    await user.click(screen.getByRole('button', { name: /set new password/i }));

    expect(await screen.findByText(/do not match/i)).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).includes('/auth/password/reset')),
    ).toBe(false);
  });
});

describe('notification preferences (FR-6.4)', () => {
  it('saves an email opt-out and keeps push disabled when unavailable', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    let prefsPut: unknown = null;
    seedAuthed((url, init) => {
      if (url.includes('/notifications/preferences') && init?.method === 'PUT') {
        prefsPut = JSON.parse(init?.body as string);
        return Promise.resolve(
          json({ emailEnabled: false, pushEnabled: false, pushAvailable: false }),
        );
      }
      return undefined;
    });

    renderRoute('/app/profile');
    const user = userEvent.setup();
    const emailToggle = await screen.findByLabelText(/email notifications/i);
    await waitFor(() => expect(emailToggle).toBeChecked());
    expect(screen.getByLabelText(/push notifications/i)).toBeDisabled();

    await user.click(emailToggle);
    await waitFor(() => {
      expect(prefsPut).toEqual({ emailEnabled: false });
    });
  });

  it('changes the password from the profile page', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    let changeBody: unknown = null;
    seedAuthed((url, init) => {
      if (url.includes('/auth/password/change')) {
        changeBody = JSON.parse(init?.body as string);
        return Promise.resolve(json({ success: true, message: 'Password changed' }));
      }
      return undefined;
    });

    renderRoute('/app/profile');
    const user = userEvent.setup();
    await screen.findByLabelText(/current password/i);
    await user.type(screen.getByLabelText(/current password/i), 'OldPass123');
    await user.type(screen.getByLabelText(/^new password/i), 'NewPass123');
    await user.type(screen.getByLabelText(/confirm new password/i), 'Different1');
    await user.click(screen.getByRole('button', { name: /^change password$/i }));

    expect(await screen.findByText(/do not match/i)).toBeInTheDocument();
    expect(changeBody).toBeNull();

    await user.clear(screen.getByLabelText(/confirm new password/i));
    await user.type(screen.getByLabelText(/confirm new password/i), 'NewPass123');
    await user.click(screen.getByRole('button', { name: /^change password$/i }));

    await waitFor(() => {
      expect(changeBody).toEqual({
        currentPassword: 'OldPass123',
        newPassword: 'NewPass123',
      });
    });
  });
});

describe('notification bell', () => {
  function seedLanding(count?: number) {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      if (url.includes('/notifications/unread-count')) {
        return Promise.resolve(json({ count: count ?? 0 }));
      }
      if (url.includes('/stats/public')) {
        return Promise.resolve(
          json({
            totalReports: 3,
            submittedReports: 1,
            resolvedReports: 1,
            categories: 2,
          }),
        );
      }
      return Promise.resolve(
        json({ totalElements: 0, totalPages: 0, page: 0, size: 5, content: [] }),
      );
    });
  }

  it('shows the unread badge for signed-in users', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    seedLanding(3);
    renderRoute('/');

    expect(
      await screen.findByRole('link', { name: /notifications, 3 unread/i }),
    ).toBeInTheDocument();
  });

  it('stays hidden for anonymous visitors and never fetches the count', async () => {
    seedLanding();
    renderRoute('/');

    expect(
      await screen.findByRole('heading', { level: 1, name: /see a problem/i }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: /notifications/i }),
    ).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).includes('unread-count')),
    ).toBe(false);
  });
});

describe('report wizard media (FR-8.5)', () => {
  const category = { id: 'cat-1', name: 'Potholes', slug: 'potholes' };
  const createdReport = {
    id: 'r-99',
    code: 'MTF-2026-000099',
    title: 'Deep pothole on Main St',
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
    statusHistory: [],
    media: [],
    comments: [],
  };

  async function walkToLocationStep(user: ReturnType<typeof userEvent.setup>) {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    renderRoute('/app/reports/new');
    await screen.findByRole('option', { name: /potholes/i });
    await user.selectOptions(
      screen.getByLabelText(/what kind of issue is this/i),
      'cat-1',
    );
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.type(screen.getByLabelText(/^title/i), 'Deep pothole on Main St');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.type(screen.getByLabelText(/latitude/i), '-6.82');
    await user.type(screen.getByLabelText(/longitude/i), '39.21');
  }

  function seedWizard(): { uploadCalls: () => number } {
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      if (url.includes('/categories')) return Promise.resolve(json([category]));
      if (url.includes('/media/upload')) return Promise.resolve(json(createdReport));
      if (url.includes('/reports') && init?.method === 'POST') {
        return Promise.resolve(json(createdReport, 201));
      }
      return Promise.resolve(json({}));
    });
    return {
      uploadCalls: () =>
        fetchMock.mock.calls.filter(
          ([url, init]) =>
            String(url).includes('/media/upload') && init?.method === 'POST',
        ).length,
    };
  }

  it('uploads every attached photo after submit', async () => {
    const { uploadCalls } = seedWizard();
    const user = userEvent.setup();
    await walkToLocationStep(user);

    const photoA = new File(['a'], 'a.jpg', { type: 'image/jpeg' });
    const photoB = new File(['b'], 'b.png', { type: 'image/png' });
    await user.upload(screen.getByLabelText(/attach a photo/i), [photoA, photoB]);

    expect(await screen.findByText(/a\.jpg/)).toBeInTheDocument();
    expect(screen.getByText(/b\.png/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(await screen.findByText(/a\.jpg, b\.png/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /submit report/i }));
    expect(await screen.findByText(/MTF-2026-000099/i)).toBeInTheDocument();
    await waitFor(() => expect(uploadCalls()).toBe(2));
  });

  it('accepts at most five photos', async () => {
    seedWizard();
    const user = userEvent.setup();
    await walkToLocationStep(user);

    const files = Array.from({ length: 6 }, (_, i) =>
      new File(['x'], `p${i}.jpg`, { type: 'image/jpeg' }),
    );
    await user.upload(screen.getByLabelText(/attach a photo/i), files);

    expect(await screen.findByText(/up to 5 photos/i)).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /remove/i })).toHaveLength(5);
  });

  it('rejects files that are not supported images', async () => {
    seedWizard();
    const user = userEvent.setup({ applyAccept: false });
    await walkToLocationStep(user);

    await user.upload(
      screen.getByLabelText(/attach a photo/i),
      new File(['x'], 'note.txt', { type: 'text/plain' }),
    );

    expect(
      await screen.findByText(/only jpeg, png, or webp photos are allowed/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /remove/i }),
    ).not.toBeInTheDocument();
  });
});

describe('report evidence upload (FR-9.1)', () => {
  it('rejects non-image files on the report page', async () => {
    localStorage.setItem('mtaafix.accessToken', 'access-token');
    seedAuthed((url) => {
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
            statusHistory: [],
            media: [],
            comments: [],
          }),
        );
      }
      return undefined;
    });

    renderRoute('/app/reports/r-1');
    const user = userEvent.setup({ applyAccept: false });
    const input = await screen.findByLabelText(/attach a photo/i);
    await user.upload(input, new File(['x'], 'note.txt', { type: 'text/plain' }));

    expect(
      await screen.findByText(/only jpeg, png, or webp photos are allowed/i),
    ).toBeInTheDocument();
  });
});
