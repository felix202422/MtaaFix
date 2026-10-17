import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthProvider';

export function UserMenu() {
  const { user, isAuthenticated, loading, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  if (loading || !user) return null;

  if (!isAuthenticated) {
    return (
      <div className="flex items-center gap-3">
        <Link
          to="/login"
          className="text-sm font-medium text-muted transition-colors hover:text-brand-green-deep"
        >
          Sign in
        </Link>
        <Link
          to="/register"
          className="rounded-lg bg-brand-green-deep px-3 py-1.5 text-sm font-semibold text-white transition-opacity hover:opacity-90"
        >
          Create account
        </Link>
      </div>
    );
  }

  const initials =
    user.fullName
      .split(/\s+/)
      .map((p) => p.charAt(0))
      .join('')
      .slice(0, 2)
      .toUpperCase() || 'U';

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Account menu"
        className="flex items-center gap-2 rounded-lg border border-line px-2 py-1.5 text-sm font-medium text-fg transition-colors hover:bg-canvas"
      >
        <span
          aria-hidden="true"
          className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-green-deep text-xs font-bold text-white"
        >
          {initials}
        </span>
        <span className="hidden sm:inline">{user.fullName}</span>
      </button>
      {open && (
        <div
          role="menu"
          tabIndex={-1}
          className="absolute right-0 z-40 mt-2 w-48 rounded-lg border border-line bg-surface py-1 shadow-lg"
          onMouseLeave={() => setOpen(false)}
        >
          <Link
            to="/app"
            role="menuitem"
            onClick={() => setOpen(false)}
            className="block px-4 py-2 text-sm text-fg transition-colors hover:bg-canvas"
          >
            Dashboard
          </Link>
          <Link
            to="/app/reports"
            role="menuitem"
            onClick={() => setOpen(false)}
            className="block px-4 py-2 text-sm text-fg transition-colors hover:bg-canvas"
          >
            My reports
          </Link>
          <Link
            to="/app/notifications"
            role="menuitem"
            onClick={() => setOpen(false)}
            className="block px-4 py-2 text-sm text-fg transition-colors hover:bg-canvas"
          >
            Notifications
          </Link>
          <Link
            to="/app/profile"
            role="menuitem"
            onClick={() => setOpen(false)}
            className="block px-4 py-2 text-sm text-fg transition-colors hover:bg-canvas"
          >
            Profile
          </Link>
          {(user.role === 'MODERATOR' ||
            user.role === 'ADMINISTRATOR' ||
            user.role === 'ORGANIZATION_ADMIN') && (
            <Link
              to="/admin"
              role="menuitem"
              onClick={() => setOpen(false)}
              className="block px-4 py-2 text-sm font-semibold text-brand-green-deep transition-colors hover:bg-canvas"
            >
              Admin
            </Link>
          )}
          <button
            type="button"
            role="menuitem"
            onClick={async () => {
              setOpen(false);
              await logout();
              navigate('/');
            }}
            className="block w-full px-4 py-2 text-left text-sm text-fg transition-colors hover:bg-canvas"
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  );
}
