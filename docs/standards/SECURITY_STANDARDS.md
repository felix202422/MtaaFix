# Security Standards — MtaaFix

## Authentication & Session

- JWT access tokens (short-lived) + refresh tokens (longer-lived, stored server-side).
- Passwords hashed with BCrypt; never plaintext or reversible.
- Constant-time comparison for tokens; rotate tokens on privilege changes.

## Secrets

- No secrets in Git. Use `backend/.env.example` as the template; load real values
  from the deployment environment.
- CI is configured to fail a build if `*.env`, `secrets`, or credential-looking
  strings are committed.

## Input Validation

- Validate every external input at the boundary: `@Valid` on DTOs, custom validators
  for business rules (e.g., report geometry must be within a valid bounding box /
  organization area).
- Sanitize file names and content types.

## File Uploads

- Restrict allowed MIME types (images, PDF) and extensions.
- Enforce a size limit via `MEDIA_MAX_FILE_SIZE_BYTES`.
- Store uploads outside the web root, with random file names, and serve through a
  controlled endpoint that determines content type.

## API Security

- HTTPS-only in production; HSTS and secure headers where applicable.
- CORS allow-listed to known origins.
- Rate limiting on `POST /api/v1/auth/*` and `POST /api/v1/reports`.
- OpenAPI (Swagger) endpoints disabled or protected in production.

## Data & Compliance

- PostgreSQL with PostGIS stored geospatial data; sensitive location data is
  restricted to authorized roles.
- Audit logs record every status transition and sensitive admin action.
- GDPR-style rights are out of scope for Phase 0 but must be considered as the
  platform scales.

## Vulnerabilities

- Use Spring Boot BOM versions; keep dependencies updated.
- Report security issues privately to the maintainer (see `SECURITY.md`).
