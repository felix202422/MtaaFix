import { Card } from '../design-system';

const LIFECYCLE_STEPS = [
  { name: 'Report', text: 'A citizen describes the issue, assigns a category, and drops a location pin.' },
  { name: 'Review', text: 'A moderator claims the report and checks it for accuracy.' },
  { name: 'Verify', text: 'The report is verified — or rejected with a reason, or marked as a duplicate.' },
  { name: 'Assign', text: 'An administrator routes the report to the responsible organization and field worker.' },
  { name: 'Work', text: 'The field worker starts and records progress on the repair.' },
  { name: 'Resolution review', text: 'Completion evidence is reviewed; the report can return to work if needed.' },
  { name: 'Resolved', text: 'An independent reviewer confirms the fix, and the report is closed after citizen confirmation.' },
];

const ROLES = [
  {
    role: 'Citizens',
    text: 'Report issues, add information when asked, follow progress, confirm resolutions, and dispute fixes that did not happen.',
  },
  {
    role: 'Moderators',
    text: 'Review incoming reports, request more information, verify or reject issues, and sign off on resolution evidence.',
  },
  {
    role: 'Organizations',
    text: 'Receive verified reports, assign field workers, and oversee work completion within their scope.',
  },
  {
    role: 'Field workers',
    text: 'Carry out assigned repairs, record progress, and submit completion evidence for review.',
  },
];

export function HowItWorksPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold">How MtaaFix works</h1>
      <p className="mt-3 max-w-2xl text-muted">
        Every report moves through a transparent lifecycle — from the moment a citizen
        reports it to the moment the fix is confirmed.
      </p>

      <section className="mt-10" aria-labelledby="lifecycle-heading">
        <h2 id="lifecycle-heading" className="text-2xl font-bold">
          The reporting lifecycle
        </h2>
        <ol className="mt-6 space-y-4">
          {LIFECYCLE_STEPS.map((step, index) => (
            <li key={step.name} className="flex gap-4">
              <span
                aria-hidden="true"
                className="flex size-9 shrink-0 items-center justify-center rounded-full bg-brand-green text-sm font-bold text-white"
              >
                {index + 1}
              </span>
              <Card className="flex-1">
                <p className="font-semibold text-brand-green-deep">{step.name}</p>
                <p className="mt-1 text-sm text-muted">{step.text}</p>
              </Card>
            </li>
          ))}
        </ol>
      </section>

      <section className="mt-12" aria-labelledby="roles-heading">
        <h2 id="roles-heading" className="text-2xl font-bold">
          Who does what
        </h2>
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {ROLES.map((item) => (
            <Card key={item.role}>
              <p className="font-semibold text-brand-green-deep">{item.role}</p>
              <p className="mt-1 text-sm text-muted">{item.text}</p>
            </Card>
          ))}
        </div>
      </section>
    </div>
  );
}
