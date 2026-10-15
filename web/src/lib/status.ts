import type { ReportStatus } from './types';
import type { Tone } from '../design-system';

const STATUS_META: Record<ReportStatus, { label: string; tone: Tone }> = {
  SUBMITTED: { label: 'Submitted', tone: 'neutral' },
  UNDER_REVIEW: { label: 'Under review', tone: 'info' },
  NEEDS_INFORMATION: { label: 'Needs information', tone: 'warning' },
  VERIFIED: { label: 'Verified', tone: 'info' },
  ASSIGNED: { label: 'Assigned', tone: 'info' },
  IN_PROGRESS: { label: 'In progress', tone: 'info' },
  RESOLUTION_REVIEW: { label: 'Resolution review', tone: 'info' },
  RESOLVED: { label: 'Resolved', tone: 'success' },
  REJECTED: { label: 'Rejected', tone: 'error' },
  DUPLICATE: { label: 'Duplicate', tone: 'neutral' },
  REOPENED: { label: 'Reopened', tone: 'warning' },
  CLOSED: { label: 'Closed', tone: 'success' },
};

export function statusLabel(status: ReportStatus): string {
  return STATUS_META[status]?.label ?? status;
}

export function statusTone(status: ReportStatus): Tone {
  return STATUS_META[status]?.tone ?? 'neutral';
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}
