import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, Field } from '../design-system';
import { api, isApiError } from '../lib/api';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email.trim()) {
      setError('Email is required.');
      return;
    }
    setBusy(true);
    try {
      await api.forgotPassword(email.trim());
      setSent(true);
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not request a reset link.');
    } finally {
      setBusy(false);
    }
  }

  if (sent) {
    return (
      <div className="mx-auto max-w-md px-4 py-12 sm:px-6">
        <h1 className="text-3xl font-extrabold">Check your inbox</h1>
        <p className="mt-4 text-muted">
          If an account exists for{' '}
          <span className="font-medium text-brand-black">{email.trim()}</span>, a password
          reset link is on its way. The link expires in 30 minutes and can only be used
          once.
        </p>
        <p className="mt-3 text-sm text-muted">
          Didn&apos;t receive it? Check your spam folder, or try again in a few minutes.
        </p>
        <p className="mt-6 text-sm text-muted">
          Remembered it?{' '}
          <Link to="/login" className="font-medium text-brand-green-deep hover:underline">
            Back to sign in
          </Link>
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-extrabold">Forgot password</h1>
      <p className="mt-4 text-muted">
        Enter the email you signed up with and we&apos;ll send you a link to set a new
        password.
      </p>

      <Card className="mt-6">
        <form onSubmit={(e) => void onSubmit(e)} className="space-y-4" noValidate>
          {error && (
            <p
              role="alert"
              className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error"
            >
              {error}
            </p>
          )}
          <Field
            label="Email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Button type="submit" disabled={busy} className="w-full">
            {busy ? 'Sending…' : 'Send reset link'}
          </Button>
        </form>
      </Card>

      <p className="mt-6 text-sm text-muted">
        Remembered it?{' '}
        <Link to="/login" className="font-medium text-brand-green-deep hover:underline">
          Back to sign in
        </Link>
      </p>
    </div>
  );
}
