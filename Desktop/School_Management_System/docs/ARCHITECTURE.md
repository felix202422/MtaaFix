# Architecture

## Overview

The School Management System is a decoupled web application:

```
┌──────────────┐        HTTP/JSON + JWT         ┌──────────────────┐
│   Frontend   │ ─────────────────────────────► │     Backend      │
│ React 19     │   Authorization: Bearer <JWT>  │ Spring Boot 4.1  │
│ Vite 8       │ ◄───────────────────────────── │ Java 25          │
│ MUI 9        │        REST /api/**            │ Security 7       │
└──────────────┘                                └────────┬─────────┘
                                                         │ JDBC
                                                ┌────────▼─────────┐
                                                │   PostgreSQL 16  │
                                                └──────────────────┘
```

## Backend Layering

| Layer        | Package                | Responsibility                                    |
| ------------ | ---------------------- | ------------------------------------------------- |
| Controller   | `com.sms.controller`   | HTTP endpoints, request validation, status codes  |
| Service      | `com.sms.service`      | Business logic; interface + `impl` per module     |
| Repository   | `com.sms.repository`   | Spring Data JPA interfaces, custom `@Query`       |
| Entity       | `com.sms.entity`       | JPA entities mapped to `db/schema.sql`            |
| DTO          | `com.sms.dto`          | Request/response records isolated from entities   |
| Security     | `com.sms.security`     | JWT provider, authentication filter, rate limiting, XSS sanitization, security headers |
| Config       | `com.sms.config`       | Beans: CORS, OpenAPI, ModelMapper, AOP, properties |
| Aspect       | `com.sms.aspect`       | `@Auditable` audit logging via AOP                |

### Request pipeline

```
Client → CorsFilter → XssSanitizationFilter → RateLimitFilter
      → SecurityHeadersFilter → JwtAuthenticationFilter
      → SecurityFilterChain → @PreAuthorize → Controller → Service → JPA
```

### Authentication

1. `POST /api/auth/login` authenticates via Spring Security's
   `AuthenticationManager` and returns an access token (24 h) plus refresh
   token (7 d, or 30 d with *remember me*).
2. Every subsequent request carries `Authorization: Bearer <access-token>`.
3. `JwtAuthenticationFilter` validates the token and loads the user (with
   eager roles) into the `SecurityContext`.
4. Authorization is role-based (`ROLE_*` authorities from the `roles` table).

## Database

- PostgreSQL; schema is fully scripted in
  `backend/src/main/resources/db/schema.sql` (38 tables) with seed data in
  `data.sql`.
- Hibernate `ddl-auto=none` — the SQL scripts are the source of truth.
- Aggregations for dashboards use native queries with `CAST(x AS TEXT)` for
  date/enum formatting (Hibernate does not pass through the `::` shorthand).

## Frontend

| Concern        | Location                     |
| -------------- | ---------------------------- |
| Routing + RBAC | `App.jsx`, `components/RoleRoute.jsx`, `utils/permissions.js` |
| API layer      | `services/api.js` (Axios instance, token refresh interceptor, per-module API objects) |
| State          | React context (`AuthContext`, `ThemeContext`) + TanStack Query |
| Charts         | Recharts (area, pie, donut, bar, line) fed by `/api/dashboard/admin` |
| Layout         | `components/layout/*` (role-filtered Sidebar, Header) |
| Pages          | `pages/*.jsx` — one page per module          |

### Role-based portals

The logged-in user's role (from the login response) drives:

- Which routes are reachable (`RoleRoute` + `permissions.js`),
- Which sidebar entries are visible,
- The dashboard shown at `/` (admin analytics for admins; `PortalDashboard`
  with role-specific stats for teacher/student/parent).

## Deployment

`docker-compose.yml` starts PostgreSQL (with schema + seed bootstrap),
the Spring Boot backend and an Nginx-served frontend build. All credentials
come from environment variables — see `.env.example`.
