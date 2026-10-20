import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Button, Card, Field } from '../design-system';
import { api, isApiError } from '../lib/api';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const tokenFromUrl = searchParams.get('token') ?? '';
  const [token, setToken] = useState(tokenFromUrl);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!token.trim()) {
      setError('The reset token is required.');
      return;
    }
    if (password.length < 8) {
      setError('New password must be at least 8 characters.');
      return;
    }
    if (password !== confirm) {
      setError('New passwords do not match.');
      return;
    }
    setBusy(true);
    try {
      await api.resetPassword(token.trim(), password);
      setDone(true);
    } catch (err) {
      setError(
        isApiError(err)
          ? err.message
          : 'Could not reset the password. Request a new link and try again.',
      );
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return (
      <div className="mx-auto max-w-md px-4 py-12 sm:px-6">
        <h1 className="text-3xl font-extrabold">Password updated</h1>
        <p className="mt-4 text-muted">
          Your password has been changed. You can now sign in with the new password.
        </p>
        <p className="mt-6 text-sm text-muted">
          <Link to="/login" className="font-medium text-brand-green-deep hover:underline">
            Back to sign in
          </Link>
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-extrabold">Set a new password</h1>
      <p className="mt-4 text-muted">
        Paste the token from your reset email and choose a new password. The token
        expires 30 minutes after the request and works only once.
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
            label="Reset token"
            required
            hint="Picked up from the reset link automatically."
            value={token}
            onChange={(e) => setToken(e.target.value)}
          />
          <Field
            label="New password"
            type="password"
            required
            autoComplete="new-password"
            hint="At least 8 characters."
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Field
            label="Confirm new password"
            type="password"
            required
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
          <Button type="submit" disabled={busy} className="w-full">
            {busy ? 'Saving…' : 'Set new password'}
          </Button>
        </form>
      </Card>

      <p className="mt-6 text-sm text-muted">
        <Link to="/forgot-password" className="font-medium text-brand-green-deep hover:underline">
          Request a new link
        </Link>
      </p>
    </div>
  );
}
