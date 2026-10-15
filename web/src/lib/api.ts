import type {
  PagedResponse,
  PublicCategory,
  PublicReportDetail,
  PublicReportFilters,
  PublicReportSummary,
  PublicStats,
} from './types';

const BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

const DEFAULT_TIMEOUT_MS = 15_000;

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: Record<string, string>;

  constructor(
    status: number,
    code: string,
    message: string,
    details: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

function messageForStatus(status: number, fallback: string): string {
  switch (status) {
    case 400:
      return 'The request was invalid. Check the filters and try again.';
    case 401:
      return 'Your session has expired. Please sign in again.';
    case 403:
      return 'You do not have permission to view this resource.';
    case 404:
      return 'The requested resource was not found.';
    case 409:
      return 'The resource changed in an unexpected way. Reload and try again.';
    case 500:
    case 502:
    case 503:
    case 504:
      return 'The server is having trouble. Please try again shortly.';
    default:
      return fallback;
  }
}

async function parseError(response: Response): Promise<ApiError> {
  let code = 'HTTP_' + response.status;
  let message = messageForStatus(response.status, `Request failed (${response.status}).`);
  let details: Record<string, string> = {};
  try {
    const body = (await response.json()) as {
      code?: string;
      message?: string;
      details?: Record<string, string>;
    };
    if (body.code) code = body.code;
    if (body.message) message = body.message;
    if (body.details) details = body.details;
  } catch {
    // Non-JSON error body — keep defaults.
  }
  return new ApiError(response.status, code, message, details);
}

export interface RequestOptions {
  signal?: AbortSignal;
  timeoutMs?: number;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  if (options.signal) {
    if (options.signal.aborted) controller.abort();
    else options.signal.addEventListener('abort', () => controller.abort(), { once: true });
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: { Accept: 'application/json' },
      signal: controller.signal,
    });
  } catch (error) {
    if (controller.signal.aborted) {
      throw new ApiError(0, 'TIMEOUT', 'The request took too long. Please try again.');
    }
    throw new ApiError(
      0,
      'NETWORK_ERROR',
      'Could not reach the server. Check your connection and try again.',
      { cause: error instanceof Error ? error.message : String(error) },
    );
  } finally {
    clearTimeout(timer);
  }

  if (!response.ok) {
    throw await parseError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  try {
    return (await response.json()) as T;
  } catch {
    throw new ApiError(500, 'INVALID_RESPONSE', 'The server returned an unreadable response.');
  }
}

function toQuery(filters: PublicReportFilters): string {
  const params = new URLSearchParams();
  if (filters.q) params.set('q', filters.q);
  if (filters.status) params.set('status', filters.status);
  if (filters.categoryId) params.set('categoryId', filters.categoryId);
  if (filters.dateFrom) params.set('dateFrom', filters.dateFrom);
  if (filters.dateTo) params.set('dateTo', filters.dateTo);
  if (filters.page !== undefined) params.set('page', String(filters.page));
  if (filters.size !== undefined) params.set('size', String(filters.size));
  const query = params.toString();
  return query ? `?${query}` : '';
}

export const api = {
  getPublicReports(
    filters: PublicReportFilters = {},
    options: RequestOptions = {},
  ): Promise<PagedResponse<PublicReportSummary>> {
    return request<PagedResponse<PublicReportSummary>>(`/reports/public${toQuery(filters)}`, options);
  },
  getPublicReport(
    code: string,
    options: RequestOptions = {},
  ): Promise<PublicReportDetail> {
    return request<PublicReportDetail>(`/reports/public/${encodeURIComponent(code)}`, options);
  },
  getPublicCategories(options: RequestOptions = {}): Promise<PublicCategory[]> {
    return request<PublicCategory[]>('/categories/public', options);
  },
  getPublicStats(options: RequestOptions = {}): Promise<PublicStats> {
    return request<PublicStats>('/stats/public', options);
  },
};
