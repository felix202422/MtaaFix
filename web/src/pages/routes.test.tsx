import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderRoute } from '../test/renderWithRouter';

function stubLandingApi() {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockImplementation((url: string) => {
      if (url.includes('/stats/public')) {
        return Promise.resolve(
          new Response(
            JSON.stringify({ totalReports: 3, submittedReports: 1, resolvedReports: 1, categories: 2 }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        );
      }
      return Promise.resolve(
        new Response(
          JSON.stringify({ totalElements: 0, totalPages: 0, page: 0, size: 5, content: [] }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      );
    }),
  );
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('routes', () => {
  it('renders the landing page at /', async () => {
    stubLandingApi();
    renderRoute('/');
    expect(
      await screen.findByRole('heading', { level: 1, name: /see a problem/i }),
    ).toBeInTheDocument();
    const main = screen.getByRole('main');
    expect(within(main).getByRole('link', { name: /report an issue/i })).toHaveAttribute(
      'href',
      '/report',
    );
    expect(within(main).getByRole('link', { name: /explore reports/i })).toHaveAttribute(
      'href',
      '/explore',
    );
  });

  it('shows the 404 page for unknown routes', async () => {
    renderRoute('/definitely-not-a-page');
    expect(await screen.findByRole('heading', { name: /page not found/i })).toBeInTheDocument();
    const main = screen.getByRole('main');
    expect(within(main).getByRole('link', { name: /back to home/i })).toHaveAttribute('href', '/');
  });

  it('navigates from landing to how-it-works via header', async () => {
    stubLandingApi();
    const user = userEvent.setup();
    renderRoute('/');
    const banner = screen.getByRole('banner');
    await user.click(within(banner).getByRole('link', { name: /how it works/i }));
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: /how mtaafix works/i }),
      ).toBeInTheDocument();
    });
  });

  it('shows the /report info route with working next actions', async () => {
    renderRoute('/report');
    expect(
      await screen.findByRole('heading', { level: 1, name: /report an issue/i }),
    ).toBeInTheDocument();
    expect(screen.getByText(/sign in to start/i)).toBeInTheDocument();
    const main = screen.getByRole('main');
    expect(within(main).getByRole('link', { name: /sign in to report/i })).toBeInTheDocument();
  });
});
