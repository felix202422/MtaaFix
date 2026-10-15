import { LinkButton } from '../design-system';

export function ReportInfoPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
      <h1 className="text-3xl font-extrabold">Report an issue</h1>
      <p className="mt-4 text-muted">
        Creating a report requires a citizen account. Sign-in and registration arrive
        with the citizen release — until then you can still browse and follow public
        reports.
      </p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <LinkButton to="/explore">Explore reports</LinkButton>
        <LinkButton to="/how-it-works" variant="outline">
          How it works
        </LinkButton>
      </div>
    </div>
  );
}
