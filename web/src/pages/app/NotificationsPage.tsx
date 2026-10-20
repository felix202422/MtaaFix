import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, EmptyState, ErrorState, LoadingState } from '../../design-system';
import { api, isApiError } from '../../lib/api';
import { formatDate } from '../../lib/status';
import type { Notification, PagedResponse } from '../../lib/types';

export function NotificationsPage() {
  const [page, setPage] = useState<PagedResponse<Notification> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    setError(null);
    setCurrentPage(p);
    try {
      setPage(await api.getNotifications({ page: p }));
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not load notifications.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load(0);
  }, [load]);

  async function markRead(id: string) {
    setBusy(true);
    try {
      await api.markNotificationRead(id);
      setPage((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((n) =>
                n.id === id ? { ...n, isRead: true } : n,
              ),
            }
          : prev,
      );
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not mark as read.');
    } finally {
      setBusy(false);
    }
  }

  async function markAllRead() {
    setBusy(true);
    try {
      await api.markAllNotificationsRead();
      setPage((prev) =>
        prev
          ? { ...prev, content: prev.content.map((n) => ({ ...n, isRead: true })) }
          : prev,
      );
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not mark all as read.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-extrabold">Notifications</h1>
        {page && page.content.some((n) => !n.isRead) && (
          <Button variant="outline" size="sm" onClick={() => void markAllRead()} disabled={busy}>
            Mark all as read
          </Button>
        )}
      </div>

      {loading && <LoadingState label="Loading notifications…" />}
      {error && <ErrorState message={error} onRetry={() => void load(currentPage)} />}

      {!loading && !error && page && page.content.length === 0 && (
        <div className="mt-6">
          <EmptyState
            title="No notifications"
            description="Updates about your reports will show up here."
          />
        </div>
      )}

      {!loading && !error && page && page.content.length > 0 && (
        <ul className="mt-6 divide-y divide-line rounded-card border border-line bg-surface">
          {page.content.map((n) => (
            <li
              key={n.id}
              className={`flex flex-wrap items-start justify-between gap-3 px-4 py-3 ${
                n.isRead ? 'opacity-70' : ''
              }`}
            >
              <div>
                <p className="font-medium">
                  {!n.isRead && (
                    <span
                      aria-label="Unread"
                      className="mr-2 inline-block h-2 w-2 rounded-full bg-brand-green align-middle"
                    />
                  )}
                  {n.title}
                </p>
                <p className="mt-1 text-sm text-muted">{n.body}</p>
                <p className="mt-1 text-xs text-muted">{formatDate(n.timestamp)}</p>
                {n.reportId && (
                  <Link
                    to={`/app/reports/${n.reportId}`}
                    className="mt-1 inline-block text-sm font-medium text-brand-green-deep hover:underline"
                  >
                    View report
                  </Link>
                )}
              </div>
              {!n.isRead && (
                <Button
                  variant="outline"
                  size="sm"
                  disabled={busy}
                  onClick={() => void markRead(n.id)}
                >
                  Mark read
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}

      {!loading && !error && page && page.totalPages > 1 && (
        <nav
          aria-label="Notifications pages"
          className="mt-4 flex items-center justify-between gap-3"
        >
          <Button
            variant="outline"
            size="sm"
            disabled={currentPage === 0}
            onClick={() => void load(currentPage - 1)}
          >
            ← Newer
          </Button>
          <span className="text-sm text-muted" aria-live="polite">
            Page {currentPage + 1} of {Math.max(page.totalPages, 1)}
            {typeof page.totalElements === 'number' &&
              ` · ${page.totalElements} total`}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={currentPage >= page.totalPages - 1}
            onClick={() => void load(currentPage + 1)}
          >
            Older →
          </Button>
        </nav>
      )}
    </div>
  );
}
