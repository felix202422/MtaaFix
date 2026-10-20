import { useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../../app/AuthProvider';
import { Button, Card, Field, useToast } from '../../design-system';
import { api, isApiError } from '../../lib/api';
import type { NotificationPreference } from '../../lib/types';

export function ProfilePage() {
  const { user, updateUser } = useAuth();
  const { push } = useToast();
  const [fullName, setFullName] = useState(user?.fullName ?? '');
  const [mobile, setMobile] = useState(user?.mobile ?? '');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // FR-6.4: notification channel preferences.
  const [prefs, setPrefs] = useState<NotificationPreference | null>(null);
  const [prefsError, setPrefsError] = useState<string | null>(null);
  const [prefsBusy, setPrefsBusy] = useState(false);

  // Change own password while authenticated (FR-1.7).
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwBusy, setPwBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    api
      .getNotificationPreferences()
      .then((p) => {
        if (!cancelled) setPrefs(p);
      })
      .catch(() => {
        if (!cancelled) setPrefsError('Could not load notification preferences.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

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

  async function savePrefs(patch: {
    emailEnabled?: boolean;
    pushEnabled?: boolean;
  }) {
    if (!prefs) return;
    const previous = prefs;
    setPrefsBusy(true);
    setPrefsError(null);
    setPrefs({ ...prefs, ...patch });
    try {
      setPrefs(await api.updateNotificationPreferences(patch));
      push('Notification preferences saved.', 'success');
    } catch (err) {
      setPrefs(previous);
      setPrefsError(
        isApiError(err) ? err.message : 'Could not save notification preferences.',
      );
    } finally {
      setPrefsBusy(false);
    }
  }

  async function onChangePassword(e: FormEvent) {
    e.preventDefault();
    setPwError(null);
    if (newPassword.length < 8) {
      setPwError('New password must be at least 8 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPwError('New passwords do not match.');
      return;
    }
    setPwBusy(true);
    try {
      await api.changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      push('Password changed.', 'success');
    } catch (err) {
      setPwError(isApiError(err) ? err.message : 'Could not change your password.');
    } finally {
      setPwBusy(false);
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

      <Card className="mt-6" aria-label="Notification preferences">
        <h2 className="text-base font-semibold text-brand-black">
          Notification preferences
        </h2>
        {prefsError && (
          <p
            role="alert"
            className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error"
          >
            {prefsError}
          </p>
        )}
        {!prefs && !prefsError && (
          <p className="mt-3 text-sm text-muted">Loading preferences…</p>
        )}
        {prefs && (
          <div className="mt-4 space-y-4">
            <label className="flex items-start gap-3 text-sm">
              <input
                type="checkbox"
                checked
                readOnly
                disabled
                className="mt-0.5 h-4 w-4 shrink-0 accent-brand-green"
              />
              <span>
                <span className="font-medium text-brand-black">In-app notifications</span>{' '}
                <span className="block text-muted">
                  Always on — updates appear in your notifications feed.
                </span>
              </span>
            </label>
            <label className="flex items-start gap-3 text-sm">
              <input
                type="checkbox"
                checked={prefs.emailEnabled}
                disabled={prefsBusy}
                onChange={(e) => void savePrefs({ emailEnabled: e.target.checked })}
                className="mt-0.5 h-4 w-4 shrink-0 accent-brand-green"
              />
              <span>
                <span className="font-medium text-brand-black">Email notifications</span>{' '}
                <span className="block text-muted">
                  Get an email when your report status changes.
                </span>
              </span>
            </label>
            <label className="flex items-start gap-3 text-sm">
              <input
                type="checkbox"
                checked={prefs.pushEnabled}
                disabled={prefsBusy || !prefs.pushAvailable}
                onChange={(e) => void savePrefs({ pushEnabled: e.target.checked })}
                className="mt-0.5 h-4 w-4 shrink-0 accent-brand-green"
              />
              <span>
                <span className="font-medium text-brand-black">Push notifications</span>{' '}
                <span className="block text-muted">
                  {prefs.pushAvailable
                    ? 'Receive push notifications on your devices.'
                    : 'No push provider is configured for this deployment yet.'}
                </span>
              </span>
            </label>
          </div>
        )}
      </Card>

      <Card className="mt-6">
        <h2 className="text-base font-semibold text-brand-black">Change password</h2>
        <form onSubmit={(e) => void onChangePassword(e)} className="mt-4 space-y-4" noValidate>
          {pwError && (
            <p
              role="alert"
              className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-error"
            >
              {pwError}
            </p>
          )}
          <Field
            label="Current password"
            type="password"
            required
            autoComplete="current-password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />
          <Field
            label="New password"
            type="password"
            required
            autoComplete="new-password"
            hint="At least 8 characters."
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
          <Field
            label="Confirm new password"
            type="password"
            required
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
          <Button type="submit" disabled={pwBusy} className="w-full">
            {pwBusy ? 'Changing…' : 'Change password'}
          </Button>
        </form>
      </Card>

      <Card className="mt-6">
        <p className="text-sm text-muted">Role</p>
        <p className="font-medium">{user?.role ?? '—'}</p>
      </Card>
    </div>
  );
}
