import { useEffect, useState, type ReactNode } from 'react';
import { api, isApiError } from '../../lib/api';
import type { SystemSettings } from '../../lib/types';
import { Badge, Card, ErrorState, LoadingState } from '../../design-system';
import { AdminPageShell } from './AdminPageShell';

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Card aria-label={title}>
      <h2 className="mb-3 text-base font-semibold text-brand-black">{title}</h2>
      <dl className="flex flex-col gap-2 text-sm">{children}</dl>
    </Card>
  );
}

function Row({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-line/50 py-1.5 last:border-b-0">
      <dt className="text-muted">{label}</dt>
      <dd className="text-right font-medium text-brand-black">{value}</dd>
    </div>
  );
}

export function AdminSettingsPage() {
  const [settings, setSettings] = useState<SystemSettings | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .getSettings()
      .then(setSettings)
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load settings.'));
  }, []);

  return (
    <AdminPageShell
      title="System settings"
      description="Effective server configuration, grouped by area (P24). Read-only in the MVP — configuration is environment-driven (FR-12.3); the full settings editor is post-MVP."
    >
      <div className="flex items-center gap-2">
        <Badge tone="info">Read-only (MVP)</Badge>
        <p className="text-sm text-muted">Sensitive values are masked; changes happen via environment configuration.</p>
      </div>

      {error && <ErrorState message={error} />}
      {!error && !settings && <LoadingState label="Loading settings…" />}
      {settings && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Section title="General">
            <Row label="Application name" value={settings.general.applicationName} />
            <Row label="Timezone" value={settings.general.applicationTimeZone} />
          </Section>

          <Section title="Reporting rules">
            <Row
              label="Max upload size"
              value={`${Math.round(settings.reporting.maxUploadBytes / (1024 * 1024))} MiB`}
            />
            <Row
              label="Multipart limit"
              value={settings.reporting.multipartMaxFileSize}
            />
            <Row
              label="Allowed media types"
              value={settings.reporting.allowedMediaTypes.join(', ')}
            />
            <Row
              label="In-app notifications"
              value={settings.reporting.notifications.inAppEnabled ? 'Enabled' : 'Disabled'}
            />
            <Row
              label="Email notifications"
              value={
                settings.reporting.notifications.emailEnabled ? 'Enabled' : 'Post-MVP (Phase 13)'
              }
            />
          </Section>

          <Section title="Security">
            <Row
              label="Access token TTL"
              value={`${Math.round(settings.security.accessTokenTtlSeconds / 60)} minutes`}
            />
            <Row
              label="Refresh token TTL"
              value={`${Math.round(settings.security.refreshTokenTtlSeconds / 86400)} days`}
            />
            <Row label="CORS origins" value={settings.security.corsAllowedOrigins} />
          </Section>

          <Section title="Privacy">
            <Row label="Location fuzzing" value={settings.privacy.locationFuzzing} />
            <Row
              label="Roles seeing precise locations"
              value={settings.privacy.rolesSeeingPreciseLocations.join(', ')}
            />
          </Section>

          <Section title="Geographic configuration">
            <Row label="Fuzz radius (min)" value={`${settings.geographic.fuzzMinMeters} m`} />
            <Row label="Fuzz radius (max)" value={`${settings.geographic.fuzzMaxMeters} m`} />
            <Row
              label="Fuzz secret"
              value={
                settings.geographic.fuzzSecretConfigured
                  ? 'Configured (masked)'
                  : settings.geographic.fuzzSecretIsDefault
                    ? 'Default development value — change in production'
                    : 'Not set'
              }
            />
          </Section>

          <Section title="Integrations">
            <Row label="Map tiles" value={settings.integrations.mapTiles} />
            <Row label="Email" value={settings.integrations.email} />
            <Row label="Push" value={settings.integrations.push} />
          </Section>
        </div>
      )}
    </AdminPageShell>
  );
}
