import { Link } from 'react-router-dom';

export function ForgotPasswordPage() {
  return (
    <div className="mx-auto max-w-md px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-extrabold">Forgot password</h1>
      <p className="mt-4 text-muted">
        Password self-service is not available yet. When it ships you&apos;ll be able to
        request a reset link here. Until then, contact support and we&apos;ll help you
        regain access to your account.
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
