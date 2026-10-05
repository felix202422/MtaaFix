# Contributing to School Management System

Thank you for considering a contribution! This document explains how to set up
the project and submit changes.

## Code of Conduct

By participating, you agree to abide by our
[Code of Conduct](CODE_OF_CONDUCT.md). Please report unacceptable behavior.

## Getting Started

### Prerequisites

- Java 25 (JDK) — Spring Boot 4.1
- Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16+

### Setup

1. Fork and clone the repository.
2. Copy `.env.example` to `.env` and fill in local values (DB password, JWT secret).
3. Create the database:

   ```bash
   psql -U postgres -c "CREATE DATABASE sms_db;"
   psql -U postgres -d sms_db -f backend/src/main/resources/db/schema.sql
   psql -U postgres -d sms_db -f backend/src/main/resources/db/data.sql
   ```

4. Start the backend:

   ```bash
   cd backend
   DB_PASSWORD=yourpassword mvn spring-boot:run
   ```

5. Start the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## How to Contribute

1. Open or comment on an issue describing the change.
2. Create a feature branch from `main`:
   `git checkout -b feat/your-feature` (or `fix/...`, `docs/...`).
3. Make your changes with focused, descriptive commits.
4. Verify before pushing:
   - Backend: `mvn -f backend/pom.xml clean verify`
   - Frontend: `npm run build && npm run lint` (inside `frontend/`)
5. Open a pull request with a clear description and reference the issue.

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(assignments): add submission grading endpoint
fix(auth): remember-me refresh token expiry
docs: update API examples in README
```

## Coding Guidelines

- **Java:** Follow existing package layout (`controller` / `service` /
  `repository` / `entity` / `dto` / `config` / `security`). New services get an
  interface + `impl`. Keep controllers thin; business logic in services.
- **React:** Function components + hooks. Pages in `src/pages`, reusable
  components in `src/components`, API calls only through `src/services/api.js`.
- **Secrets:** Never commit credentials, tokens, or passwords. All
  configuration flows through environment variables (see `.env.example`).
- **Migrations:** Schema changes go in `backend/src/main/resources/db/schema.sql`
  with corresponding seed data in `data.sql`.

## Reporting Bugs

Open a GitHub issue including:

- Steps to reproduce
- Expected vs actual behavior
- Backend log excerpt (redact any secrets)
- Environment (OS, JDK version, browser)

## Security Issues

See [SECURITY.md](SECURITY.md). Do not open public issues for security
vulnerabilities.
