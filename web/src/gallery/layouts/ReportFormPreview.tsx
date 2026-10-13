import { Badge, Card } from '../../design-system';
import { PreviewShell, PreviewButton } from './PreviewShell';

const steps = ['1. Issue details', '2. Location', '3. Evidence', '4. Review & submit'];

export function ReportFormPreview() {
  return (
    <PreviewShell label="Page 9 — Report an Issue wizard (Phase 7)">
      <div className="bg-canvas p-6">
        <div className="mb-6 flex flex-wrap gap-2">
          {steps.map((s, i) => (
            <span
              key={s}
              className={`rounded-full px-3 py-1 text-xs font-semibold ${
                i === 0
                  ? 'bg-brand-green text-white'
                  : 'bg-surface text-muted border border-line'
              }`}
            >
              {s}
            </span>
          ))}
        </div>
        <Card>
          <div className="grid gap-4 lg:grid-cols-2">
            <div>
              <span className="mb-1 block text-sm font-medium">Category</span>
              <div className="h-11 rounded-lg border border-line bg-canvas px-3 py-2 text-sm text-muted">
                Select a category…
              </div>
            </div>
            <div>
              <span className="mb-1 block text-sm font-medium">Title</span>
              <div className="h-11 rounded-lg border border-line bg-canvas px-3 py-2 text-sm text-muted">
                Brief summary of the issue
              </div>
            </div>
            <div className="lg:col-span-2">
              <span className="mb-1 block text-sm font-medium">Description</span>
              <div className="min-h-24 rounded-lg border border-line bg-canvas px-3 py-2 text-sm text-muted">
                What happened, when, and any helpful details…
              </div>
            </div>
          </div>
          <div className="mt-4 flex items-center justify-between">
            <Badge tone="info">Step 1 of 4</Badge>
            <PreviewButton>Continue</PreviewButton>
          </div>
        </Card>
      </div>
    </PreviewShell>
  );
}
