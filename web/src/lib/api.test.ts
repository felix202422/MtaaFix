import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from './api';

const fetchMock = vi.fn();

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  fetchMock.mockReset();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('api client', () => {
  it('returns parsed public reports page', async () => {
    const page = {
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
      content: [
        {
          code: 'MTF-2026-000001',
          title: 'Pothole',
          description: null,
          categoryId: 'c1',
          categoryName: 'Roads',
          categorySlug: 'roads',
          status: 'SUBMITTED',
          reportedAt: '2026-10-01T10:00:00Z',
          latitude: -6.82,
          longitude: 39.21,
        },
      ],
    };
    fetchMock.mockResolvedValue(jsonResponse(page));

    const result = await api.getPublicReports({ q: 'pothole', page: 0, size: 20 });

    expect(result.totalElements).toBe(1);
    expect(result.content[0].code).toBe('MTF-2026-000001');
    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/reports/public?');
    expect(url).toContain('q=pothole');
    expect(url).toContain('page=0');
    expect(url).toContain('size=20');
  });

  it('encodes the report code in detail URL', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ code: 'MTF-2026-000001', timeline: [] }),
    );
    await api.getPublicReport('MTF-2026-000001');
    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/reports/public/MTF-2026-000001');
  });

  it('maps 404 to ApiError with RESOURCE_NOT_FOUND', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        {
          timestamp: '2026-10-15T00:00:00Z',
          status: 404,
          code: 'RESOURCE_NOT_FOUND',
          message: 'Report not found: nope',
          path: '/api/v1/reports/public/nope',
        },
        404,
      ),
    );

    await expect(api.getPublicReport('nope')).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
      code: 'RESOURCE_NOT_FOUND',
    });
  });

  it('maps validation failures to ApiError 400', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ code: 'BAD_REQUEST', message: 'Unknown status filter: X' }, 400),
    );

    const error = await api.getPublicReports({ status: 'X' }).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).message).toContain('Unknown status filter');
  });

  it('maps 401 to session-expired message', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ code: 'INVALID_TOKEN' }, 401));
    const error = (await api.getPublicStats().catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(401);
    expect(error.message).toContain('session has expired');
  });

  it('maps 5xx to server-error message', async () => {
    fetchMock.mockResolvedValue(new Response('oops', { status: 503 }));
    const error = (await api.getPublicCategories().catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(503);
    expect(error.message).toContain('server is having trouble');
  });

  it('maps fetch rejection to NETWORK_ERROR', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));
    const error = (await api.getPublicStats().catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('NETWORK_ERROR');
    expect(error.status).toBe(0);
  });

  it('maps abort/timeout to TIMEOUT', async () => {
    fetchMock.mockImplementation(
      (_url: string, init?: RequestInit) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener('abort', () =>
            reject(new DOMException('Aborted', 'AbortError')),
          );
        }),
    );
    const error = (await api.getPublicStats({ timeoutMs: 10 }).catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('TIMEOUT');
  });

  it('skips undefined filters in the query string', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ totalElements: 0, totalPages: 0, page: 0, size: 20, content: [] }),
    );
    await api.getPublicReports({ q: 'x' });
    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).not.toContain('status=');
    expect(url).not.toContain('categoryId=');
    expect(url).not.toContain('dateFrom=');
  });
});
