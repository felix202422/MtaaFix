# Security

## Reporting a Vulnerability
If you discover a security vulnerability, **do not open a public issue**.
Email the maintainer with a description and steps to reproduce. We will respond
within 48 hours.

## Security Checklist
- [x] No secrets in Git.
- [x] Passwords stored with BCrypt.
- [x] JWT secrets loaded from environment variables.
- [x] Rate limiting enabled on auth + report submission.
- [x] File uploads validate extension, MIME, and size.
- [ ] HTTPS in production (Phase 18 deployment concern).
- [x] OpenAPI docs not exposed in production (admin-role-only; swagger locked
      to ADMINISTRATOR).
- [x] Password reset/change revokes outstanding tokens (Phase 16,
      `users.token_version`; see [docs/standards/SECURITY_REVIEW.md](docs/standards/SECURITY_REVIEW.md)).

Full review record (Phase 16, 2026-10-22): [docs/standards/SECURITY_REVIEW.md](docs/standards/SECURITY_REVIEW.md).
