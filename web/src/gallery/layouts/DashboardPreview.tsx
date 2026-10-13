import { Badge, Card } from '../../design-system';
import { PreviewShell, PreviewButton } from './PreviewShell';

export function DashboardPreview() {
  return (
    <PreviewShell label="Page 8 — Citizen Dashboard (Phase 7)">
      <div className="bg-canvas p-6">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold">Karibu, Jane</h2>
            <p className="text-sm text-muted">Here&apos;s what&apos;s happening with your reports.</p>
          </div>
          <PreviewButton>Report an issue</PreviewButton>
        </div>
        <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
          {[
            { label: 'Total reports', value: '12', tone: 'info' as const },
            { label: 'In progress', value: '4', tone: 'info' as const },
            { label: 'Resolved', value: '7', tone: 'success' as const },
            { label: 'Needs your input', value: '1', tone: 'warning' as const },
          ].map((s) => (
            <Card key={s.label}>
              <div className="mb-2">
                <Badge tone={s.tone}>{s.label}</Badge>
              </div>
              <p className="text-3xl font-extrabold">{s.value}</p>
            </Card>
          ))}
        </div>
        <Card padded={false}>
          <div className="border-b border-line px-5 py-3 text-sm font-semibold">
            Recent reports
          </div>
          {[
            { code: 'MTF-2026-000121', title: 'Blocked drainage near market', status: 'In progress' as const, tone: 'info' as const },
            { code: 'MTF-2026-000115', title: 'Broken streetlight, Mbagala', status: 'Under review' as const, tone: 'info' as const },
            { code: 'MTF-2026-000110', title: 'Pothole on Nyerere Road', status: 'Resolved' as const, tone: 'success' as const },
          ].map((r) => (
            <div
              key={r.code}
              className="flex items-center justify-between border-b border-line px-5 py-3 last:border-0"
            >
              <div>
                <p className="text-sm font-semibold">{r.title}</p>
                <p className="font-mono text-xs text-muted">{r.code}</p>
              </div>
              <Badge tone={r.tone}>{r.status}</Badge>
            </div>
          ))}
        </Card>
      </div>
    </PreviewShell>
  );
}
