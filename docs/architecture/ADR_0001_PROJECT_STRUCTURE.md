# ADR-0001 — Project Structure

## Status

Proposed (Phase 0).

## Context

MtaaFix is a clean, layered monolithic Spring Boot backend with a separate, modular
client scaffold. The system is composed of three independently buildable repositories:

- `backend` — Spring Boot 25 REST API, Maven, PostgreSQL + PostGIS, Flyway.
- `web` — Vite + React + TypeScript (citizen / moderator / admin / field worker / organization).
- `mobile` — Flutter single shared codebase (Android + iOS).

## Decision

1. **Monolith for Phase 0.** Start as a modular monolith: one deployable Spring Boot jar,
   with clear package boundaries (config, security, user, report, category, assignment,
   notification, audit, dashboard, media, common, exception).
2. **No JPA entities exposed via the API.** All API contracts are DTOs; mappers translate
   between entities and DTOs.
3. **UUID identifiers** for public resources (reports, media, assignments) and integer
   surrogate keys where they are purely internal.
4. **Vertical feature slices under `src/main/java/com/mtaafix`**: each feature owns its
   DTO, mapper, repository, service, and controller.
5. **Configuration in `src/main/resources`**, with `application.yml` as the canonical base
   and `application-dev.yml` / `application-test.yml` overriding only environment-specific
   values.
6. **Flyway migrations** live under `db/migration` and run in forward-only, versioned
   `V<version>__<description>.sql` files.
7. **Devweb/mobile are separate packages** with their own `package.json` / `pubspec.yaml`,
   but both consume the same `backend` contract documented in `docs/api/API.md`.

## Consequences

- Easier to get started and to debug locally; a single JVM process serves all roles.
- Clean package boundaries; the eventual extraction to microservices is a refactor,
   not a rewrite.
- Client applications are fully independent frontend/UI repositories.
