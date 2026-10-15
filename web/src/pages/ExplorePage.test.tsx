import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderRoute } from '../test/renderWithRouter';

vi.mock('../components/map/ReportMap', () => ({
  ReportMap: ({
    onBoundsChange,
  }: {
    onBoundsChange?: (bbox: string) => void;
  }) => (
    <div>
      <button type="button" onClick={() => onBoundsChange?.('39.1,-6.9,39.3,-6.7')}>
        Simulate map bounds change
      </button>
    </div>
  ),
}));

const fetchMock = vi.fn();

const reportA = {
  code: 'MTF-2026-000010',
  title: 'Alpha flooding near river',
  description: 'Water blocks the road after rain.',
  categoryId: 'cat-a',
  categoryName: 'Drainage',
  categorySlug: 'drainage',
  status: 'SUBMITTED',
  reportedAt: '2026-10-01T09:00:00Z',
  latitude: -6.82,
  longitude: 39.21,
};

const reportB = {
  code: 'MTF-2026-000011',
  title: 'Beta broken sidewalk',
  description: null,
  categoryId: 'cat-b',
  categoryName: 'Sidewalks',
  categorySlug: 'sidewalks',
  status: 'RESOLVED',
  reportedAt: '2026-10-02T09:00:00Z',
  latitude: null,
  longitude: null,
};

const emptyCategories = new Response(JSON.stringify([]), {
  status: 200,
  headers: { 'Content-Type': 'application/json' },
});

function pageOf(content: unknown[], total = content.length) {
  return new Response(
    JSON.stringify({ totalElements: total, totalPages: 1, page: 0, size: 10, content }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  );
}

function renderExplore() {
  renderRoute('/explore');
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('ExplorePage', () => {
  it('renders reports from the public API', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/categories/public')) {
        return Promise.resolve(emptyCategories);
      }
      return Promise.resolve(pageOf([reportA, reportB]));
    });

    renderExplore();

    expect(await screen.findByText(reportA.title)).toBeInTheDocument();
    expect(screen.getByText(reportB.title)).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('2 reports found');
    expect(screen.getByRole('link', { name: /view details for alpha/i })).toHaveAttribute(
      'href',
      '/reports/MTF-2026-000010',
    );
  });

  it('sends the status filter to the API when changed', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/categories/public')) {
        return Promise.resolve(emptyCategories);
      }
      if (url.includes('status=RESOLVED')) {
        return Promise.resolve(pageOf([reportB]));
      }
      return Promise.resolve(pageOf([reportA, reportB]));
    });

    const user = userEvent.setup();
    renderExplore();
    await screen.findByText(reportA.title);

    await user.selectOptions(screen.getByLabelText(/status/i), 'RESOLVED');

    await waitFor(() => {
      const reportCall = fetchMock.mock.calls
        .map((call) => call[0] as string)
        .find((url) => url.includes('/reports/public') && url.includes('status=RESOLVED'));
      expect(reportCall).toBeDefined();
    });
    await waitFor(() => {
      expect(screen.queryByText(reportA.title)).not.toBeInTheDocument();
    });
    expect(screen.getByText(reportB.title)).toBeInTheDocument();
  });

  it('toggles to map view and shows the privacy note', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/categories/public')) {
        return Promise.resolve(emptyCategories);
      }
      return Promise.resolve(pageOf([reportA]));
    });

    const user = userEvent.setup();
    renderExplore();
    await screen.findByText(reportA.title);

    expect(screen.queryByText(/generalized for privacy/i)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /map view/i }));

    expect(screen.getByText(/generalized for privacy/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /map view/i }),
    ).toHaveAttribute('aria-pressed', 'true');
    expect(screen.queryByText(reportA.title)).not.toBeInTheDocument();
  });

  it('sends the bbox filter to the API when Search this area is clicked', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/categories/public')) {
        return Promise.resolve(emptyCategories);
      }
      return Promise.resolve(pageOf([reportA]));
    });

    const user = userEvent.setup();
    renderExplore();
    await screen.findByText(reportA.title);

    await user.click(screen.getByRole('button', { name: /map view/i }));
    await user.click(screen.getByRole('button', { name: /simulate map bounds change/i }));
    await user.click(screen.getByRole('button', { name: /search this area/i }));

    await waitFor(() => {
      const bboxCall = fetchMock.mock.calls
        .map((call) => call[0] as string)
        .find(
          (url) =>
            url.includes('/reports/public') && decodeURIComponent(url).includes('bbox=39.1,-6.9,39.3,-6.7'),
        );
      expect(bboxCall).toBeDefined();
    });
  });

  it('shows the empty state when nothing matches', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/categories/public')) {
        return Promise.resolve(emptyCategories);
      }
      return Promise.resolve(pageOf([]));
    });

    renderExplore();
    expect(await screen.findByText(/no reports match these filters/i)).toBeInTheDocument();
  });

  it('shows the error state with retry when the API fails', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes('/categories/public')) {
        return Promise.resolve(emptyCategories);
      }
      return Promise.resolve(
        new Response(JSON.stringify({ code: 'INTERNAL_ERROR', message: 'boom' }), {
          status: 500,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });

    const user = userEvent.setup();
    renderExplore();

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    const retriesBefore = fetchMock.mock.calls.filter((c) =>
      (c[0] as string).includes('/reports/public'),
    ).length;
    await user.click(screen.getByRole('button', { name: /try again/i }));
    await waitFor(() => {
      const retriesAfter = fetchMock.mock.calls.filter((c) =>
        (c[0] as string).includes('/reports/public'),
      ).length;
      expect(retriesAfter).toBeGreaterThan(retriesBefore);
    });
  });
});
