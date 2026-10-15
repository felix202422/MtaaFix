import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../app/AuthProvider';
import { Button, Card, Field, useToast } from '../design-system';
import { isApiError } from '../lib/api';

export function RegisterPage() {
  const { register, login } = useAuth();
  const navigate = useNavigate();
  const { push } = useToast();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    setBusy(true);
    try {
      await register({
        email: email.trim(),
        password,
        fullName: fullName.trim(),
        mobile: mobile.trim() || undefined,
      });
      await login(email.trim(), password);
      push('Account created. Welcome to MtaaFix.', 'success');
      navigate('/app', { replace: true });
    } catch (err) {
      setError(
        isApiError(err) ? err.message : 'Registration failed. Please try again.',
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-extrabold">Create your account</h1>
      <p className="mt-2 text-muted">
        A free citizen account lets you report issues and track how they get fixed.
      </p>
      <Card className="mt-6">
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          {error && (
            <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error">
              {error}
            </p>
          )}
          <Field
            label="Full name"
            autoComplete="name"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
          <Field
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Field
            label="Mobile (optional)"
            type="tel"
            autoComplete="tel"
            hint="e.g. +255712345678"
            value={mobile}
            onChange={(e) => setMobile(e.target.value)}
          />
          <Field
            label="Password"
            type="password"
            autoComplete="new-password"
            required
            hint="At least 8 characters."
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button type="submit" disabled={busy} className="w-full">
            {busy ? 'Creating account…' : 'Create account'}
          </Button>
        </form>
      </Card>
      <p className="mt-4 text-sm text-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-brand-green-deep hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
