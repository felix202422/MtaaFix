import { NavLink, Outlet } from 'react-router-dom';
import { UserMenu } from './UserMenu';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm font-medium transition-colors hover:text-brand-green-deep ${
    isActive ? 'text-brand-green-deep' : 'text-muted'
  }`;

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col bg-canvas">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-surface focus:px-4 focus:py-2 focus:text-sm focus:font-semibold"
      >
        Skip to main content
      </a>
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <NavLink
            to="/"
            className="text-lg font-extrabold text-brand-green-deep"
            aria-label="MtaaFix home"
          >
            MtaaFix
          </NavLink>
          <nav aria-label="Main" className="flex flex-wrap items-center gap-4 sm:gap-6">
            <NavLink to="/how-it-works" className={navLinkClass}>
              How it works
            </NavLink>
            <NavLink to="/explore" className={navLinkClass}>
              Explore reports
            </NavLink>
            <UserMenu />
          </nav>
        </div>
      </header>
      <main id="main" className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-line bg-surface">
        <div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-6 text-sm text-muted sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <p>© {new Date().getFullYear()} MtaaFix — See it. Report it. Fix it together.</p>
          <nav aria-label="Footer" className="flex gap-4">
            <NavLink to="/how-it-works" className={navLinkClass}>
              How it works
            </NavLink>
            <NavLink to="/explore" className={navLinkClass}>
              Explore
            </NavLink>
          </nav>
        </div>
      </footer>
    </div>
  );
}
