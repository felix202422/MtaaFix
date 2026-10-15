import { cleanup, screen } from '@testing-library/react';
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
  role: 'CITIZEN',
  organisationId: null,
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const category = { id: 'cat-1', name: 'Potholes', slug: 'potholes', type: 'PRIMARY' };

const createdReport = {
  id: 'r-new',
  code: 'MTF-2026-000099',
  title: 'Deep pothole on Main St',
  description: null,
  location: { latitude: -6.82, longitude: 39.21, srid: 4326 },
  reportedAt: '2026-10-15T10:00:00Z',
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

beforeEach(() => {
  localStorage.clear();
  localStorage.setItem('mtaafix.accessToken', 'access-token');
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function mockBase() {
  fetchMock.mockImplementation((url: string) => {
    if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
    if (url.includes('/categories')) return Promise.resolve(json([category]));
    return Promise.resolve(json({}));
  });
}

describe('report wizard', () => {
  it('requires a category before leaving step 1', async () => {
    mockBase();
    renderRoute('/app/reports/new');
    const user = userEvent.setup();
    await screen.findByRole('option', { name: /potholes/i });
    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
    await user.selectOptions(screen.getByLabelText(/what kind of issue is this/i), 'cat-1');
    expect(screen.getByRole('button', { name: /next/i })).toBeEnabled();
  });

  it('picks a location from the map on the location step', async () => {
    mockBase();
    renderRoute('/app/reports/new');
    const user = userEvent.setup();
    await screen.findByRole('option', { name: /potholes/i });
    await user.selectOptions(screen.getByLabelText(/what kind of issue is this/i), 'cat-1');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.type(screen.getByLabelText(/title/i), 'Deep pothole on Main St');
    await user.click(screen.getByRole('button', { name: /next/i }));

    await user.click(screen.getByRole('button', { name: /pick location on map/i }));
    expect(screen.getByLabelText(/latitude/i)).toHaveValue(-6.82);
    expect(screen.getByLabelText(/longitude/i)).toHaveValue(39.2);
    expect(screen.getByRole('button', { name: /next/i })).toBeEnabled();
  });

  it('requires a title before leaving step 2', async () => {
    mockBase();
    renderRoute('/app/reports/new');
    const user = userEvent.setup();
    await screen.findByRole('option', { name: /potholes/i });
    await user.selectOptions(screen.getByLabelText(/what kind of issue is this/i), 'cat-1');
    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
    await user.type(screen.getByLabelText(/title/i), 'Deep pothole on Main St');
    expect(screen.getByRole('button', { name: /next/i })).toBeEnabled();
  });

  it('submits the report and shows the reference code', async () => {
    let submittedBody: unknown = null;
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      if (url.includes('/categories')) return Promise.resolve(json([category]));
      if (url.includes('/reports') && init?.method === 'POST') {
        submittedBody = JSON.parse(init.body as string);
        return Promise.resolve(json(createdReport, 201));
      }
      return Promise.resolve(json({}));
    });

    renderRoute('/app/reports/new');
    const user = userEvent.setup();

    await screen.findByRole('option', { name: /potholes/i });
    await user.selectOptions(screen.getByLabelText(/what kind of issue is this/i), 'cat-1');
    await user.click(screen.getByRole('button', { name: /next/i }));

    await user.type(screen.getByLabelText(/^title/i), 'Deep pothole on Main St');
    await user.click(screen.getByRole('button', { name: /next/i }));

    // Step 3: manual lat/lng (no geolocation in jsdom).
    await user.type(screen.getByLabelText(/latitude/i), '-6.82');
    await user.type(screen.getByLabelText(/longitude/i), '39.21');
    await user.click(screen.getByRole('button', { name: /next/i }));

    await user.click(screen.getByRole('button', { name: /submit report/i }));

    expect(await screen.findByText(/MTF-2026-000099/i)).toBeInTheDocument();
    expect(submittedBody).toMatchObject({
      categoryId: 'cat-1',
      title: 'Deep pothole on Main St',
      location: { latitude: -6.82, longitude: 39.21 },
    });
  });

  it('rejects an oversized photo', async () => {
    mockBase();
    renderRoute('/app/reports/new');
    const user = userEvent.setup();
    await screen.findByRole('option', { name: /potholes/i });
    await user.selectOptions(screen.getByLabelText(/what kind of issue is this/i), 'cat-1');
    await user.click(screen.getByRole('button', { name: /next/i }));
      await user.type(screen.getByLabelText(/^title/i), 'Big photo');
      await user.click(screen.getByRole('button', { name: /next/i }));
      await user.type(screen.getByLabelText(/latitude/i), '-6.82');
      await user.type(screen.getByLabelText(/longitude/i), '39.21');

      const bigFile = new File(['x'], 'big.jpg', { type: 'image/jpeg' });
    Object.defineProperty(bigFile, 'size', { value: 21 * 1024 * 1024 });
    const input = screen.getByLabelText(/attach a photo/i);
    await user.upload(input, bigFile);

    expect(await screen.findByText(/20 mb or smaller/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.click(screen.getByRole('button', { name: /submit report/i }));

    const photoUploads = fetchMock.mock.calls.filter(([url, init]) =>
      String(url).includes('/media/upload') && init?.method === 'POST',
    );
    expect(photoUploads).toHaveLength(0);
  });

  it('surfaces an ownership error from the server on submit', async () => {
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/auth/me')) return Promise.resolve(json(citizen));
      if (url.includes('/categories')) return Promise.resolve(json([category]));
      if (url.includes('/reports') && init?.method === 'POST') {
        return Promise.resolve(
          json({ code: 'FORBIDDEN', message: 'You do not own this report.' }, 403),
        );
      }
      return Promise.resolve(json({}));
    });

    renderRoute('/app/reports/new');
    const user = userEvent.setup();
    await screen.findByRole('option', { name: /potholes/i });
    await user.selectOptions(screen.getByLabelText(/what kind of issue is this/i), 'cat-1');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.type(screen.getByLabelText(/^title/i), 'Blocked drain');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.type(screen.getByLabelText(/latitude/i), '-6.82');
    await user.type(screen.getByLabelText(/longitude/i), '39.21');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.click(screen.getByRole('button', { name: /submit report/i }));

    expect(await screen.findByText(/you do not own this report/i)).toBeInTheDocument();
  });
});
