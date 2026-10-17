import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../app/AuthProvider';
import { adminLinksForRole } from './adminNav';

interface AdminPageShellProps {
  title: string;
  description?: string;
  children: ReactNode;
  actions?: ReactNode;
}

const subLinkClass = ({ isActive }: { isActive: boolean }) =>
  `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
    isActive
      ? 'bg-brand-green-deep text-white'
      : 'text-muted hover:bg-canvas hover:text-brand-green-deep'
  }`;

/** Shared chrome for the Phase 9 admin pages: heading + role-filtered sub-navigation. */
export function AdminPageShell({ title, description, children, actions }: AdminPageShellProps) {
  const { user } = useAuth();
  const links = user ? adminLinksForRole(user.role) : [];

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-brand-black">{title}</h1>
          {description && <p className="mt-1 max-w-2xl text-sm text-muted">{description}</p>}
        </div>
        {actions}
      </div>
      {links.length > 0 && (
        <nav
          aria-label="Admin sections"
          className="flex flex-wrap items-center gap-1 border-b border-line pb-2"
        >
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.to === '/admin'} className={subLinkClass}>
              {link.label}
            </NavLink>
          ))}
        </nav>
      )}
      {children}
    </div>
  );
}
