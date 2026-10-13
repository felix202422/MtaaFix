import { Badge, Card } from '../../design-system';
import { PreviewShell, PreviewButton } from './PreviewShell';

export function LandingPreview() {
  return (
    <PreviewShell label="Page 1 — Landing (Phase 6)">
      <div className="bg-canvas">
        <div className="flex items-center justify-between border-b border-line bg-surface px-6 py-3">
          <span className="text-lg font-extrabold text-brand-green-deep">MtaaFix</span>
          <div className="flex items-center gap-4 text-sm font-medium text-muted">
            <span>How it works</span>
            <span>Explore reports</span>
            <PreviewButton>Sign in</PreviewButton>
          </div>
        </div>
        <div className="grid gap-8 px-6 py-12 lg:grid-cols-2">
          <div>
            <h1 className="text-4xl font-extrabold leading-tight">
              See a Problem? <span className="text-brand-green">Let&apos;s Fix It Together.</span>
            </h1>
            <p className="mt-4 max-w-md text-muted">
              Report potholes, broken streetlights, waste, and drainage issues in your
              community — and track how they get fixed.
            </p>
            <div className="mt-6 flex gap-3">
              <PreviewButton>Report an issue</PreviewButton>
              <PreviewButton variant="outline">Explore reports</PreviewButton>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            {[
              { label: 'Reports submitted', value: '1,284' },
              { label: 'Resolved this month', value: '312' },
              { label: 'Avg. resolution time', value: '4.2 days' },
              { label: 'Active organizations', value: '18' },
            ].map((s) => (
              <Card key={s.label}>
                <p className="text-2xl font-extrabold text-brand-green-deep">{s.value}</p>
                <p className="text-sm text-muted">{s.label}</p>
              </Card>
            ))}
          </div>
        </div>
        <div className="border-t border-line bg-surface px-6 py-4 text-xs text-muted">
          Footer — privacy · help · contact · © MtaaFix
        </div>
      </div>
    </PreviewShell>
  );
}

export function RecentReportsPreview() {
  return (
    <PreviewShell label="Recent public reports strip">
      <div className="space-y-3 p-6">
        {[
          { code: 'MTF-2026-000121', title: 'Blocked drainage, Kinondoni', status: 'In progress' as const },
          { code: 'MTF-2026-000118', title: 'Broken streetlight, Mbagala', status: 'Resolved' as const },
          { code: 'MTF-2026-000115', title: 'Pothole on Nyerere Road', status: 'Under review' as const },
        ].map((r) => (
          <div key={r.code} className="flex items-center justify-between rounded-lg border border-line px-4 py-3">
            <div>
              <p className="text-sm font-semibold">{r.title}</p>
              <p className="font-mono text-xs text-muted">{r.code}</p>
            </div>
            <Badge tone={r.status === 'Resolved' ? 'success' : r.status === 'In progress' ? 'info' : 'info'}>
              {r.status}
            </Badge>
          </div>
        ))}
      </div>
    </PreviewShell>
  );
}
