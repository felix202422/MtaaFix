import { useState } from 'react';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Field,
  LoadingState,
  Modal,
  TextAreaField,
  modalConfirmFooter,
  useToast,
} from '../design-system';
import { LandingPreview } from './layouts/LandingPreview';
import { AuthPreview } from './layouts/AuthPreview';
import { DashboardPreview } from './layouts/DashboardPreview';
import { ReportFormPreview } from './layouts/ReportFormPreview';

const swatches = [
  { name: 'Primary Green', hex: '#1EB53A', use: 'Main actions, brand' },
  { name: 'Deep Green', hex: '#15803D', hover: true, use: 'Hover, emphasis' },
  { name: 'Flag Yellow', hex: '#FCD116', use: 'Highlights (sparing)' },
  { name: 'Primary Black', hex: '#111111', use: 'Text, structure' },
  { name: 'Flag Blue', hex: '#00A3DD', use: 'Links, info, maps' },
  { name: 'White', hex: '#FFFFFF', use: 'Cards, surfaces' },
  { name: 'Background Gray', hex: '#F5F7FA', use: 'App background' },
  { name: 'Border Gray', hex: '#E2E8F0', use: 'Borders, dividers' },
  { name: 'Muted Text', hex: '#64748B', use: 'Supporting text' },
  { name: 'Error Red', hex: '#DC2626', use: 'Errors, destructive' },
  { name: 'Warning Amber', hex: '#D97706', use: 'Warnings' },
];

const typeScale = [
  { label: 'Display', cls: 'text-4xl font-extrabold', text: 'See It. Report It.' },
  { label: 'H1', cls: 'text-3xl font-bold', text: 'Community issue reporting' },
  { label: 'H2', cls: 'text-2xl font-bold', text: 'Section heading' },
  { label: 'H3', cls: 'text-xl font-semibold', text: 'Card heading' },
  { label: 'Body', cls: 'text-base', text: 'Body text for descriptions and content.' },
  { label: 'Small', cls: 'text-sm text-muted', text: 'Supporting and caption text.' },
];

const statuses = [
  { tone: 'info' as const, label: 'Submitted' },
  { tone: 'info' as const, label: 'Under review' },
  { tone: 'warning' as const, label: 'Needs information' },
  { tone: 'success' as const, label: 'Verified' },
  { tone: 'info' as const, label: 'Assigned' },
  { tone: 'info' as const, label: 'In progress' },
  { tone: 'warning' as const, label: 'Resolution review' },
  { tone: 'success' as const, label: 'Resolved' },
  { tone: 'error' as const, label: 'Rejected' },
  { tone: 'neutral' as const, label: 'Duplicate' },
  { tone: 'warning' as const, label: 'Reopened' },
  { tone: 'neutral' as const, label: 'Closed' },
];

function Section({ id, title, children }: { id: string; title: string; children: React.ReactNode }) {
  return (
    <section id={id} className="scroll-mt-20 border-t border-line py-10">
      <h2 className="mb-6 text-2xl font-bold">{title}</h2>
      {children}
    </section>
  );
}

export function DesignGallery() {
  const [modalOpen, setModalOpen] = useState(false);
  const { push } = useToast();

  return (
    <div className="mx-auto max-w-5xl px-4 pb-24">
      <header className="sticky top-0 z-40 border-b border-line bg-surface/95 py-4 backdrop-blur">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-extrabold text-brand-green-deep">MtaaFix Design System</h1>
            <p className="text-sm text-muted">Phase 2 foundation — tokens, components, layout previews</p>
          </div>
          <nav aria-label="Design system sections" className="flex flex-wrap gap-4 text-sm font-medium">
            <a className="hover:text-brand-green-deep" href="#colors">Colors</a>
            <a className="hover:text-brand-green-deep" href="#typography">Typography</a>
            <a className="hover:text-brand-green-deep" href="#components">Components</a>
            <a className="hover:text-brand-green-deep" href="#status">Status</a>
            <a className="hover:text-brand-green-deep" href="#layouts">Layouts</a>
          </nav>
        </div>
      </header>

      <Section id="colors" title="Color palette (spec §9.1)">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {swatches.map((s) => (
            <div key={s.name} className="overflow-hidden rounded-card border border-line bg-surface">
              <div className="h-16 border-b border-line" style={{ backgroundColor: s.hex }} />
              <div className="p-3">
                <p className="text-sm font-semibold">{s.name}</p>
                <p className="font-mono text-xs text-muted">{s.hex}</p>
                <p className="mt-1 text-xs text-muted">{s.use}</p>
              </div>
            </div>
          ))}
        </div>
        <p className="mt-4 text-sm text-muted">
          Usage: predominantly neutral surfaces; green as brand primary; blue for links and
          maps; yellow sparingly; never all-green chrome (spec §9.2).
        </p>
      </Section>

      <Section id="typography" title="Typography — Inter">
        <Card>
          <div className="space-y-5">
            {typeScale.map((t) => (
              <div key={t.label} className="flex flex-col gap-1 border-b border-line pb-3 last:border-0 last:pb-0">
                <span className="text-xs font-semibold uppercase tracking-wide text-muted">{t.label}</span>
                <p className={t.cls}>{t.text}</p>
              </div>
            ))}
          </div>
        </Card>
      </Section>

      <Section id="components" title="Components">
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <h3 className="mb-4 text-lg font-semibold">Buttons</h3>
            <div className="flex flex-wrap items-center gap-3">
              <Button>Report an issue</Button>
              <Button variant="secondary">Continue</Button>
              <Button variant="outline">Cancel</Button>
              <Button variant="danger">Delete</Button>
              <Button disabled>Disabled</Button>
              <Button size="sm">Small</Button>
            </div>
          </Card>

          <Card>
            <h3 className="mb-4 text-lg font-semibold">Form fields</h3>
            <div className="space-y-4">
              <Field label="Email" type="email" placeholder="you@example.com" hint="We never share your email." />
              <Field label="Title" defaultValue="Pothole on Main Rd" />
              <Field label="Reference" defaultValue="MTF-2026-000042" error="Reference already exists." />
              <TextAreaField label="Description" placeholder="Describe the issue…" />
            </div>
          </Card>

          <Card>
            <h3 className="mb-4 text-lg font-semibold">Feedback</h3>
            <div className="flex flex-wrap gap-3">
              <Button onClick={() => push('Report submitted — MTF-2026-000001', 'success')}>
                Toast: success
              </Button>
              <Button variant="outline" onClick={() => push('Upload failed — try again.', 'error')}>
                Toast: error
              </Button>
              <Button variant="secondary" onClick={() => setModalOpen(true)}>
                Open modal
              </Button>
            </div>
          </Card>

          <Card>
            <h3 className="mb-4 text-lg font-semibold">Badges &amp; cards</h3>
            <div className="flex flex-wrap gap-2">
              <Badge tone="success">Resolved</Badge>
              <Badge tone="info">Under review</Badge>
              <Badge tone="warning">Needs info</Badge>
              <Badge tone="error">Rejected</Badge>
              <Badge>Archived</Badge>
            </div>
            <p className="mt-3 text-sm text-muted">
              Badges always pair color with a text label (never color alone, spec §9.7).
            </p>
          </Card>

          <Card className="lg:col-span-2">
            <h3 className="mb-4 text-lg font-semibold">UI states</h3>
            <div className="grid gap-4 lg:grid-cols-3">
              <LoadingState />
              <EmptyState
                title="No reports yet"
                description="Report an issue to help your community."
                action={<Button size="sm">Report an issue</Button>}
              />
              <ErrorState message="Could not load reports. Check your connection." onRetry={() => push('Retrying…', 'info')} />
            </div>
          </Card>
        </div>
      </Section>

      <Section id="status" title="Report status indicators (12 states, ADR-0002)">
        <div className="flex flex-wrap gap-2">
          {statuses.map((s) => (
            <Badge key={s.label} tone={s.tone}>
              {s.label}
            </Badge>
          ))}
        </div>
      </Section>

      <Section id="layouts" title="Representative layout previews">
        <p className="mb-6 rounded-card border border-brand-yellow/40 bg-amber-50 px-4 py-3 text-sm text-warning">
          Design validation previews only — presentational, not the product UI. Product
          pages are built in Phases 6–7 against real APIs (no dead buttons in product
          surfaces, spec §10).
        </p>
        <div className="space-y-10">
          <LandingPreview />
          <AuthPreview />
          <DashboardPreview />
          <ReportFormPreview />
        </div>
      </Section>

      <Modal
        open={modalOpen}
        title="Verify this report?"
        onClose={() => setModalOpen(false)}
        footer={modalConfirmFooter(
          () => {
            setModalOpen(false);
            push('Report verified.', 'success');
          },
          () => setModalOpen(false),
        )}
      >
        <p>
          Verifying moves the report to <strong>VERIFIED</strong> and makes it eligible
          for assignment. This action is recorded in the report history.
        </p>
      </Modal>
    </div>
  );
}
