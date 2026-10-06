# Roadmap — MtaaFix

## Phase 0 — Project Foundation (current)

- Repository scaffold: backend, web, mobile, docs, `.github`.
- Architecture decision records.
- API surface documentation.
- CI workflow (Java 25, Maven) and issue/PR templates.
- Coding & security standards.
- Root documentation (README, CHANGELOG, LICENSE, CODE_OF_CONDUCT, CONTRIBUTING).
- Backend dependency scaffold (`pom.xml`, `.gitignore`, `.env.example`).

## Phase 1 — Requirements & Design

- Detailed functional & non-functional requirements.
- Domain-driven design and problem/event storming.
- Database schema design (entities, relations, constraints, indexes).
- API contract finalization (OpenAPI/Swagger + JSON schemas).
- UI/UX wireframes for all five roles.

## Phase 2 — MVP (Web + Mobile + API)

- Auth: register/login/refresh/logout with JWT.
- Report lifecycle: create, submit, verify, reject, assign, resolve, close.
- Categories and organizations.
- Location capture with PostGIS geometry.
- Media upload/download with validation.
- Notification endpoints.

## Phase 3 — Role Experience

- Citizen dashboard + report history + feedback.
- Moderator review queue + analytics.
- Administrator user/category/assignment management + audit log.
- Field worker assignment + completion evidence.
- Organization dashboard + stats.

## Phase 4 — Observability & Hardening

- Structured logging, metrics, and distributed tracing.
- Rate limiting, retry, and circuit-breaker policies.
- Security scanning, dependency updates, and secret scanning in CI.
- Deployments to cloud (env vars, HTTPS, managed DB/PostGIS).

## Phase 5 — Scale & Adapt

- Multitenancy and organization isolation.
- Advanced analytics and configurable report workflows.
- Delivery to municipalities, universities, estates, utilities, hotels, etc.
