import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../app/AuthProvider';
import { Button, Card, Field, useToast } from '../design-system';
import { isApiError } from '../lib/api';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { push } = useToast();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const from =
    (location.state as { from?: string } | null)?.from ?? '/app';

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email.trim(), password);
      push('Signed in.', 'success');
      navigate(from, { replace: true });
    } catch (err) {
      setError(
        isApiError(err) ? err.message : 'Sign in failed. Please try again.',
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-extrabold">Sign in</h1>
      <p className="mt-2 text-muted">
        Access your reports, notifications, and profile.
      </p>
      <Card className="mt-6">
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          {error && (
            <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error">
              {error}
            </p>
          )}
          <Field
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Field
            label="Password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button type="submit" disabled={busy} className="w-full">
            {busy ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>
      </Card>
      <div className="mt-4 flex flex-col gap-2 text-sm text-muted">
        <Link to="/register" className="font-medium text-brand-green-deep hover:underline">
          Create a citizen account
        </Link>
        <Link to="/forgot-password" className="hover:underline">
          Forgot your password?
        </Link>
      </div>
    </div>
  );
}
