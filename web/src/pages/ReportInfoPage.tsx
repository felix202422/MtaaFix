import { LinkButton } from '../design-system';
import { useAuth } from '../app/AuthProvider';

export function ReportInfoPage() {
  const { isAuthenticated } = useAuth();
  return (
    <div className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
      <h1 className="text-3xl font-extrabold">Report an issue</h1>
      <p className="mt-4 text-muted">
        Creating a report takes about two minutes. You&apos;ll describe what&apos;s wrong,
        add a photo, and drop a pin so the right team can find it. Sign in to start.
      </p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        {isAuthenticated ? (
          <LinkButton to="/app/reports/new">Start a report</LinkButton>
        ) : (
          <LinkButton to="/login">Sign in to report</LinkButton>
        )}
        <LinkButton to="/how-it-works" variant="outline">
          How it works
        </LinkButton>
      </div>
    </div>
  );
}
