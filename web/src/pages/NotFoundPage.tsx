import { LinkButton } from '../design-system';

export function NotFoundPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
      <p className="text-sm font-semibold text-brand-green-deep">404</p>
      <h1 className="mt-2 text-3xl font-extrabold">Page not found</h1>
      <p className="mt-4 text-muted">
        The page you are looking for does not exist or may have moved.
      </p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <LinkButton to="/">Back to home</LinkButton>
        <LinkButton to="/explore" variant="outline">
          Explore reports
        </LinkButton>
      </div>
    </div>
  );
}
