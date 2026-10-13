import type { ReactNode } from 'react';

export type Tone = 'success' | 'info' | 'warning' | 'error' | 'neutral';

const tones: Record<Tone, string> = {
  success: 'bg-green-50 text-brand-green-deep border-green-200',
  info: 'bg-sky-50 text-brand-blue border-sky-200',
  warning: 'bg-amber-50 text-warning border-amber-200',
  error: 'bg-red-50 text-error border-red-200',
  neutral: 'bg-canvas text-muted border-line',
};

export interface BadgeProps {
  tone?: Tone;
  children: ReactNode;
}

export function Badge({ tone = 'neutral', children }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${tones[tone]}`}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {children}
    </span>
  );
}
