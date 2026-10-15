import { useState, type FormEvent } from 'react';
import { useAuth } from '../../app/AuthProvider';
import { Button, Card, Field, useToast } from '../../design-system';
import { api, isApiError } from '../../lib/api';

export function ProfilePage() {
  const { user, updateUser } = useAuth();
  const { push } = useToast();
  const [fullName, setFullName] = useState(user?.fullName ?? '');
  const [mobile, setMobile] = useState(user?.mobile ?? '');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!fullName.trim()) {
      setError('Full name is required.');
      return;
    }
    setBusy(true);
    try {
      const updated = await api.updateProfile({
        fullName: fullName.trim(),
        mobile: mobile.trim() || undefined,
      });
      updateUser(updated);
      push('Profile updated.', 'success');
    } catch (err) {
      setError(isApiError(err) ? err.message : 'Could not update your profile.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold">Profile</h1>
      <p className="mt-2 text-muted">
        Update your name and mobile number. Your email cannot be changed here.
      </p>

      <Card className="mt-6">
        <form onSubmit={(e) => void onSubmit(e)} className="space-y-4" noValidate>
          {error && (
            <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error">
              {error}
            </p>
          )}
          <Field label="Email" value={user?.email ?? ''} readOnly disabled />
          <Field
            label="Full name"
            required
            autoComplete="name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
          <Field
            label="Mobile"
            type="tel"
            autoComplete="tel"
            hint="e.g. +255712345678"
            value={mobile}
            onChange={(e) => setMobile(e.target.value)}
          />
          <Button type="submit" disabled={busy} className="w-full">
            {busy ? 'Saving…' : 'Save changes'}
          </Button>
        </form>
      </Card>

      <Card className="mt-6">
        <p className="text-sm text-muted">Role</p>
        <p className="font-medium">{user?.role ?? '—'}</p>
        <p className="mt-3 text-sm text-muted">
          Password changes are not available yet. Contact support if you need to reset
          your password.
        </p>
      </Card>
    </div>
  );
}
