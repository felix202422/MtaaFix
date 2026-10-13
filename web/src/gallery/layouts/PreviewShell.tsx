export function PreviewShell({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <figure aria-label={`Layout preview: ${label} (not interactive)`}>
      <figcaption className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted">
        {label}
      </figcaption>
      <div className="overflow-hidden rounded-card border border-line bg-surface shadow-sm">
        {children}
      </div>
    </figure>
  );
}

export function PreviewButton({
  variant = 'primary',
  children,
}: {
  variant?: 'primary' | 'outline' | 'muted';
  children: React.ReactNode;
}) {
  const styles = {
    primary: 'bg-brand-green text-white',
    outline: 'border border-line bg-surface text-brand-black',
    muted: 'bg-canvas text-muted',
  } as const;
  return (
    <span
      aria-hidden="true"
      className={`inline-flex h-10 items-center rounded-lg px-4 text-sm font-semibold ${styles[variant]}`}
    >
      {children}
    </span>
  );
}
