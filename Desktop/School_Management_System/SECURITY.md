# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security seriously and appreciate responsible disclosure.

**Please do NOT report security vulnerabilities through public GitHub issues,
discussions, or pull requests.**

Instead, report privately:

1. Use GitHub's ["Report a vulnerability"](../../security/advisories/new)
   feature on this repository, **or**
2. Email the maintainers (address listed in CODE_OF_CONDUCT.md).

Include as much of the following as you can:

- Type of issue (e.g. SQL injection, broken access control, XSS)
- Full paths of source file(s) related to the issue
- Location of the affected source code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

You can expect an initial response within **72 hours**. We will keep you
informed of progress toward a fix and announce the fix in the release notes.

## Security Measures in This Project

- JWT-based stateless authentication with short-lived access tokens and
  rotating refresh tokens (30-day lifetime with "remember me")
- BCrypt (strength 12) password hashing
- Role-based access control enforced server-side (Spring Security +
  `@PreAuthorize`) and mirrored in the UI
- Rate limiting and XSS sanitization filters on every request
- Security headers filter (CSP-friendly defaults, no-cache on API responses)
- All secrets supplied via environment variables — no credentials in source

## Deployment Recommendations

- Generate a unique `JWT_SECRET` per environment:
  `openssl rand -base64 48`
- Enforce HTTPS in production (put the app behind a TLS-terminating proxy)
- Restrict database network access to the backend only
- Set `logging.level.com.sms=INFO` (or higher) in production
- Regularly update dependencies (`mvn versions:display-dependency-updates`,
  `npm audit`)
