import { useId } from 'react';
import type { InputHTMLAttributes, TextareaHTMLAttributes } from 'react';

const fieldBase =
  'w-full rounded-lg border border-line bg-surface px-3 py-2 text-base text-brand-black placeholder:text-muted focus:border-brand-blue focus:outline-none';

const labelClass = 'mb-1 block text-sm font-medium text-brand-black';
const hintClass = 'mt-1 block text-sm text-muted';
const errorClass = 'mt-1 block text-sm font-medium text-error';

export interface FieldProps
  extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  hint?: string;
  error?: string;
}

export function Field({ label, hint, error, id, className = '', ...rest }: FieldProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const hintId = `${fieldId}-hint`;
  const errorId = `${fieldId}-error`;
  const describedBy = error ? errorId : hint ? hintId : undefined;

  return (
    <div>
      <label htmlFor={fieldId} className={labelClass}>
        {label}
      </label>
      <input
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`${fieldBase} ${error ? 'border-error' : ''} ${className}`}
        {...rest}
      />
      {hint && !error && <span id={hintId} className={hintClass}>{hint}</span>}
      {error && <span id={errorId} className={errorClass}>{error}</span>}
    </div>
  );
}

export interface TextAreaFieldProps
  extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  hint?: string;
  error?: string;
}

export function TextAreaField({
  label,
  hint,
  error,
  id,
  className = '',
  ...rest
}: TextAreaFieldProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const hintId = `${fieldId}-hint`;
  const errorId = `${fieldId}-error`;
  const describedBy = error ? errorId : hint ? hintId : undefined;

  return (
    <div>
      <label htmlFor={fieldId} className={labelClass}>
        {label}
      </label>
      <textarea
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`${fieldBase} min-h-28 ${error ? 'border-error' : ''} ${className}`}
        {...rest}
      />
      {hint && !error && <span id={hintId} className={hintClass}>{hint}</span>}
      {error && <span id={errorId} className={errorClass}>{error}</span>}
    </div>
  );
}
