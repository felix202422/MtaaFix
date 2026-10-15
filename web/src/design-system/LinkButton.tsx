import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

type Variant = 'primary' | 'secondary' | 'outline';
type Size = 'sm' | 'md';

const base =
  'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors';

const variants: Record<Variant, string> = {
  primary: 'bg-brand-green text-white hover:bg-brand-green-deep active:bg-brand-green-deep',
  secondary: 'bg-brand-black text-white hover:bg-black',
  outline:
    'border border-line bg-surface text-brand-black hover:border-brand-green hover:text-brand-green-deep',
};

const sizes: Record<Size, string> = {
  sm: 'h-9 px-3 text-sm',
  md: 'h-11 px-5 text-base',
};

export interface LinkButtonProps {
  to: string;
  variant?: Variant;
  size?: Size;
  children: ReactNode;
  className?: string;
  'aria-label'?: string;
}

export function LinkButton({
  to,
  variant = 'primary',
  size = 'md',
  children,
  className = '',
  ...rest
}: LinkButtonProps) {
  return (
    <Link
      to={to}
      className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
      {...rest}
    >
      {children}
    </Link>
  );
}
