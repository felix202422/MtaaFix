# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] — 2026-10-05
### Phase 0 — Project Foundation
- Repository scaffold: backend, web, mobile, docs, .github
- Architecture decision records
- API surface
- CI workflow (Java 25, Maven)
- Issue/PR templates
- Coding & security standards
- README, CHANGELOG, LICENSE, CODE_OF_CONDUCT, CONTRIBUTING
- `.freebuff/project-id`
- Backend dependency scaffold (`pom.xml`, `.gitignore`, `.env.example`)
- Web frontend scaffold (Vite + React + TypeScript)
- Flutter mobile app scaffold (Android + iOS)

## [0.2.0] — 2026-10-05
### Phase 1 — Requirements & Design
- Domain-driven design: User, Report, Assignment, Organisation, ReportCategory, ReportMedia, ReportComment, ReportStatusHistory, Notification
- Database schema design with PostGIS geometry columns, indexes, foreign keys, cascades
- API contract documentation (`docs/api/API.md`)
- UI/UX wireframes for all five roles (citizen, moderator, admin, field worker, organization admin)
- Coding standards document
- Security standards document

## [0.3.0] — 2026-10-05
### Phase 2 — Backend Foundation & MVP API
- Auth: register/login/refresh/logout with JWT (AuthController, UserService, JwtTokenProvider)
- Report lifecycle: create, submit, verify, reject, assign, resolve, close (ReportController, ReportService)
- Categories and organizations (ReportCategoryRepository, OrganisationRepository, reportRepository.findOrganisationById)
- Location capture with PostGIS geometry (Point, Polygon in Report entity, LocationDto)
- Media upload/download with validation (ReportMedia entity, addMedia, deleteMedia endpoints)
- Notification endpoints (NotificationController: list, mark read, mark all read)
- Report status history (ReportStatusHistory entity)
- Report comments (ReportComment entity)
- Report status tracking
- Dashboard endpoints (DashboardController, DashboardService)
- Category endpoints (CategoryController, CategoryService)
- Assignment endpoints (AssignmentController, AssignmentService)
- Audit endpoints (AuditController, AuditService)
- Fixed compile errors: JWT TokenProvider API for jjwt 0.12.6 (signWith, verifyWith)
- Fixed PagedResponse.size field type from int to long to handle Spring Data 4.x Page.getSize() returning long
- Fixed ReportRepository JPQL query to use r.organisation instead of r.organisations
- Added missing CreateCommentRequest and CreateMediaRequest DTOs
- Fixed sizeBytes casting in ReportService.addMedia
- Fixed ReportRepository findReportsByOrganisationIdWithAssignment JPQL query

## [0.4.0] — 2026-10-09
### Phase 8 — Flutter Mobile App
- Main app entry point with routing
- Splash screen with branding
- Login/Register screens with validation
- Report list screen showing user's reports
- Report form screen with location picker and subject picker
- Notifications screen
- Profile screen with user info
- API client for backend communication
- Android and iOS configuration

## [Unreleased]
### Phase 5 — Web Application (MVP)
- Landing page with navigation
- Citizen dashboard with reports
- Report creation form
- Report list view
- Report detail view
- Authentication page
- Notifications page
- Profile page

### Phase 3 — Role Experience (MVP)
- Citizen dashboard + report history + feedback
- Moderator review queue + analytics
- Administrator user/category/assignment management + audit log
- Field worker assignment + completion evidence
- Organization dashboard + stats

### Phase 4 — Observability & Hardening
- Structured logging and metrics
- Rate limiting, retry, and circuit-breaker policies
- Security scanning, dependency updates, and secret scanning in CI
- Deployments to cloud (env vars, HTTPS, managed DB/PostGIS)
