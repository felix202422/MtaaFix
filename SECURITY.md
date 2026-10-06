# Security

## Reporting a Vulnerability
If you discover a security vulnerability, **do not open a public issue**.
Email the maintainer with a description and steps to reproduce. We will respond
within 48 hours.

## Security Checklist
- [ ] No secrets in Git.
- [ ] Passwords stored with BCrypt.
- [ ] JWT secrets loaded from environment variables.
- [ ] Rate limiting enabled on auth + report submission.
- [ ] File uploads validate extension, MIME, and size.
- [ ] HTTPS in production.
- [ ] OpenAPI docs not exposed in production.
