import type {
  AuthResponse,
  Category,
  Dashboard,
  Notification,
  PagedResponse,
  PublicCategory,
  PublicReportDetail,
  PublicReportFilters,
  PublicReportSummary,
  PublicStats,
  Report,
  UpdateProfileInput,
  User,
} from './types';

const BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

const DEFAULT_TIMEOUT_MS = 15_000;

// Access + refresh tokens live in localStorage (Phase 7 decision 5). The
// strategy and its Phase 13 hardening notes are documented in B-7.1.
const ACCESS_TOKEN_KEY = 'mtaafix.accessToken';
const REFRESH_TOKEN_KEY = 'mtaafix.refreshToken';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setAuthTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearAuthTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

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

// Set by AuthProvider so a failed silent refresh can clear the session.
let onSessionExpired: (() => void) | null = null;

export function setSessionExpiredHandler(handler: (() => void) | null): void {
  onSessionExpired = handler;
}

let refreshInFlight: Promise<string | null> | null = null;

async function tryRefreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await fetch(`${BASE_URL}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });
        if (!response.ok) return null;
        const data = (await response.json()) as AuthResponse;
        setAuthTokens(data.accessToken, data.refreshToken);
        return data.accessToken;
      } catch {
        return null;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

async function request<T>(
  path: string,
  options: RequestOptions & { method?: string; body?: unknown; auth?: boolean } = {},
): Promise<T> {
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  if (options.signal) {
    if (options.signal.aborted) controller.abort();
    else options.signal.addEventListener('abort', () => controller.abort(), { once: true });
  }

  let response: Response;
  try {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (options.body !== undefined) headers['Content-Type'] = 'application/json';
    if (options.auth !== false) {
      const token = getAccessToken();
      if (token) headers['Authorization'] = `Bearer ${token}`;
    }
    response = await fetch(`${BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
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
    // Silent refresh: retry once on 401 if a refresh token is available.
    if (response.status === 401 && options.auth !== false && getRefreshToken()) {
      const refreshed = await tryRefreshAccessToken();
      if (refreshed) {
        const retryHeaders: Record<string, string> = { Accept: 'application/json' };
        if (options.body !== undefined) retryHeaders['Content-Type'] = 'application/json';
        retryHeaders['Authorization'] = `Bearer ${refreshed}`;
        let retry: Response;
        try {
          retry = await fetch(`${BASE_URL}${path}`, {
            method: options.method ?? 'GET',
            headers: retryHeaders,
            body: options.body === undefined ? undefined : JSON.stringify(options.body),
            signal: controller.signal,
          });
        } catch (error) {
          throw new ApiError(
            0,
            'NETWORK_ERROR',
            'Could not reach the server. Check your connection and try again.',
            { cause: error instanceof Error ? error.message : String(error) },
          );
        }
        if (retry.ok) {
          if (retry.status === 204) return undefined as T;
          try {
            return (await retry.json()) as T;
          } catch {
            throw new ApiError(500, 'INVALID_RESPONSE', 'The server returned an unreadable response.');
          }
        }
        response = retry;
      } else {
        // Refresh failed — session is over.
        clearAuthTokens();
        onSessionExpired?.();
      }
    }
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
  if (filters.bbox) params.set('bbox', filters.bbox);
  if (filters.near) params.set('near', filters.near);
  if (filters.page !== undefined) params.set('page', String(filters.page));
  if (filters.size !== undefined) params.set('size', String(filters.size));
  const query = params.toString();
  return query ? `?${query}` : '';
}

function authHeader(): Record<string, string> {
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function uploadPhotoRequest(
  id: string,
  file: File,
  options: RequestOptions = {},
): Promise<Report> {
  const form = new FormData();
  form.append('file', file);
  const response = await fetch(
    `${BASE_URL}/reports/${encodeURIComponent(id)}/media/upload`,
    { method: 'POST', headers: authHeader(), body: form, signal: options.signal },
  );
  if (!response.ok) throw await parseError(response);
  return (await response.json()) as Report;
}

async function requestBlob(path: string, options: RequestOptions = {}): Promise<Blob> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: authHeader(),
    signal: options.signal,
  });
  if (!response.ok) throw await parseError(response);
  return response.blob();
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

  // --- Auth ---
  login(email: string, password: string, options: RequestOptions = {}): Promise<AuthResponse> {
    return request<AuthResponse>('/auth/login', {
      ...options,
      method: 'POST',
      body: { email, password },
      auth: false,
    });
  },
  register(
    input: { email: string; password: string; fullName: string; mobile?: string },
    options: RequestOptions = {},
  ): Promise<User> {
    return request<User>('/auth/register', { ...options, method: 'POST', body: input, auth: false });
  },
  getMe(options: RequestOptions = {}): Promise<User> {
    return request<User>('/auth/me', options);
  },
  updateProfile(input: UpdateProfileInput, options: RequestOptions = {}): Promise<User> {
    return request<User>('/auth/me', { ...options, method: 'PUT', body: input });
  },
  logout(options: RequestOptions = {}): Promise<void> {
    return request<void>('/auth/logout', { ...options, method: 'POST' });
  },

  // --- Dashboard ---
  getDashboard(options: RequestOptions = {}): Promise<Dashboard> {
    return request<Dashboard>('/dashboard/me', options);
  },

  // --- Reports (authenticated) ---
  getMyReports(
    filters: { page?: number; size?: number } = {},
    options: RequestOptions = {},
  ): Promise<PagedResponse<Report>> {
    const params = new URLSearchParams();
    if (filters.page !== undefined) params.set('page', String(filters.page));
    if (filters.size !== undefined) params.set('size', String(filters.size));
    const query = params.toString();
    return request<PagedResponse<Report>>(`/reports${query ? `?${query}` : ''}`, options);
  },
  getReport(id: string, options: RequestOptions = {}): Promise<Report> {
    return request<Report>(`/reports/${encodeURIComponent(id)}`, options);
  },
  createReport(
    input: {
      categoryId: string;
      title: string;
      description?: string;
      location: { latitude: number; longitude: number; srid?: number };
    },
    options: RequestOptions = {},
  ): Promise<Report> {
    return request<Report>('/reports', { ...options, method: 'POST', body: input });
  },
  cancelReport(id: string, options: RequestOptions = {}): Promise<Report> {
    return request<Report>(`/reports/${encodeURIComponent(id)}/cancel`, {
      ...options,
      method: 'POST',
    });
  },
  confirmReport(
    id: string,
    feedback?: string,
    options: RequestOptions = {},
  ): Promise<Report> {
    return request<Report>(`/reports/${encodeURIComponent(id)}/confirm`, {
      ...options,
      method: 'POST',
      body: feedback ? { feedback } : {},
    });
  },
  disputeReport(
    id: string,
    disputeReason: string,
    options: RequestOptions = {},
  ): Promise<Report> {
    return request<Report>(`/reports/${encodeURIComponent(id)}/dispute`, {
      ...options,
      method: 'POST',
      body: { disputeReason },
    });
  },
  resubmitReport(id: string, options: RequestOptions = {}): Promise<Report> {
    return request<Report>(`/reports/${encodeURIComponent(id)}/resubmit`, {
      ...options,
      method: 'POST',
    });
  },
  addComment(
    id: string,
    content: string,
    options: RequestOptions = {},
  ): Promise<Report> {
    return request<Report>(`/reports/${encodeURIComponent(id)}/comments`, {
      ...options,
      method: 'POST',
      body: { content },
    });
  },
  uploadPhoto(
    id: string,
    file: File,
    options: RequestOptions = {},
  ): Promise<Report> {
    return uploadPhotoRequest(id, file, options);
  },
  deleteMedia(
    mediaId: string,
    options: RequestOptions = {},
  ): Promise<void> {
    return request<void>(`/media/${encodeURIComponent(mediaId)}`, {
      ...options,
      method: 'DELETE',
    });
  },
  getMediaUrl(mediaId: string): string {
    return `${BASE_URL}/media/${encodeURIComponent(mediaId)}/content`;
  },
  fetchMediaBlob(mediaId: string, options: RequestOptions = {}): Promise<Blob> {
    return requestBlob(`/media/${encodeURIComponent(mediaId)}/content`, options);
  },

  // --- Categories ---
  getCategories(options: RequestOptions = {}): Promise<Category[]> {
    return request<Category[]>('/categories', options);
  },

  // --- Notifications ---
  getNotifications(
    options: RequestOptions & { page?: number } = {},
  ): Promise<PagedResponse<Notification>> {
    const page = options.page ?? 0;
    return request<PagedResponse<Notification>>(`/notifications?page=${page}&size=20`, options);
  },
  markNotificationRead(id: string, options: RequestOptions = {}): Promise<void> {
    return request<void>(`/notifications/${encodeURIComponent(id)}/read`, {
      ...options,
      method: 'POST',
    });
  },
  markAllNotificationsRead(options: RequestOptions = {}): Promise<void> {
    return request<void>('/notifications/read-all', { ...options, method: 'POST' });
  },
};
