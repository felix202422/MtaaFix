import { PreviewShell, PreviewButton } from './PreviewShell';

export function AuthPreview() {
  return (
    <PreviewShell label="Pages 5–6 — Login / Registration (Phase 7)">
      <div className="grid min-h-80 lg:grid-cols-2">
        <div className="flex flex-col justify-center bg-gradient-to-br from-brand-green to-brand-green-deep p-10 text-white">
          <p className="text-3xl font-extrabold">MtaaFix</p>
          <p className="mt-2 text-sm opacity-90">See It. Report It. Fix It Together.</p>
        </div>
        <div className="flex flex-col justify-center gap-4 p-10">
          <h2 className="text-2xl font-bold">Welcome back</h2>
          <div>
            <span className="mb-1 block text-sm font-medium">Email</span>
            <div className="h-11 rounded-lg border border-line bg-canvas px-3 py-2 text-sm text-muted">
              you@example.com
            </div>
          </div>
          <div>
            <span className="mb-1 block text-sm font-medium">Password</span>
            <div className="h-11 rounded-lg border border-line bg-canvas px-3 py-2 text-sm text-muted">
              ••••••••
            </div>
          </div>
          <PreviewButton>Sign in</PreviewButton>
          <p className="text-center text-sm text-muted">
            No account? <span className="font-semibold text-brand-blue">Register</span>
          </p>
        </div>
      </div>
    </PreviewShell>
  );
}
