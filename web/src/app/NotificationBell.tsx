import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuth } from './AuthProvider';

export function NotificationBell() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const location = useLocation();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (authLoading || !isAuthenticated) {
      setUnread(0);
      return;
    }
    let cancelled = false;
    api
      .getUnreadNotificationCount()
      .then((r) => {
        if (!cancelled) setUnread(Number(r.count) || 0);
      })
      .catch(() => {
        // Keep the last known count; a failed badge refresh is not fatal.
      });
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, authLoading, location.pathname]);

  if (authLoading || !isAuthenticated) return null;

  const label =
    unread > 0 ? `Notifications, ${unread} unread` : 'Notifications';

  return (
    <Link
      to="/app/notifications"
      aria-label={label}
      className="relative inline-flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:text-brand-green-deep focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-green"
    >
      <svg
        className="h-5 w-5"
        fill="none"
        viewBox="0 0 24 24"
        strokeWidth={1.5}
        stroke="currentColor"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0"
        />
      </svg>
      {unread > 0 && (
        <span
          aria-hidden="true"
          className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-error px-1 text-[10px] font-bold leading-none text-white"
        >
          {unread > 99 ? '99+' : unread}
        </span>
      )}
    </Link>
  );
}
