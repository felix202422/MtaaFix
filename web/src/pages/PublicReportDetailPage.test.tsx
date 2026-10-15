import { cleanup, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderRoute } from '../test/renderWithRouter';

const fetchMock = vi.fn();

const detail = {
  code: 'MTF-2026-000042',
  title: 'Pothole on Nyerere Road',
  description: 'Deep pothole near the bus stop.',
  categoryId: 'cat-a',
  categoryName: 'Roads',
  categorySlug: 'roads',
  status: 'RESOLVED',
  reportedAt: '2026-09-01T09:00:00Z',
  latitude: -6.82,
  longitude: 39.21,
  resolvedAt: '2026-09-10T12:00:00Z',
  closedAt: null,
  duplicateOfCode: null,
  timeline: [
    { status: 'SUBMITTED', timestamp: '2026-09-01T09:00:00Z' },
    { status: 'VERIFIED', timestamp: '2026-09-02T09:00:00Z' },
    { status: 'RESOLVED', timestamp: '2026-09-10T12:00:00Z' },
  ],
};

beforeEach(() => {
  fetchMock.mockReset();
  fetchMock.mockResolvedValue(
    new Response(JSON.stringify(detail), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
  vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('PublicReportDetailPage', () => {
  it('renders public fields, generalized location, and high-level timeline', async () => {
    renderRoute('/reports/MTF-2026-000042');

    expect(
      await screen.findByRole('heading', { level: 1, name: /pothole on nyerere road/i }),
    ).toBeInTheDocument();
    expect(screen.getByText('MTF-2026-000042')).toBeInTheDocument();
    expect(screen.getByText('Roads')).toBeInTheDocument();
    expect(screen.getByText('-6.82, 39.21')).toBeInTheDocument();
    expect(screen.getByText(/none is available for this report yet/i)).toBeInTheDocument();

    const timeline = screen.getByRole('list');
    expect(within(timeline).getByText('Submitted')).toBeInTheDocument();
    expect(within(timeline).getByText('Verified')).toBeInTheDocument();
    expect(within(timeline).getByText('Resolved')).toBeInTheDocument();
  });

  it('shows the not-found state for an unknown code', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({ code: 'RESOURCE_NOT_FOUND', message: 'Report not found' }),
        { status: 404, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    renderRoute('/reports/MTF-2099-999999');

    expect(await screen.findByRole('heading', { name: /report not found/i })).toBeInTheDocument();
    const main = screen.getByRole('main');
    expect(within(main).getByRole('link', { name: /explore reports/i })).toHaveAttribute(
      'href',
      '/explore',
    );
  });
});
