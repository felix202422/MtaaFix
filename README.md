# MtaaFix — See It. Report It. Fix It.

A production-grade civic issue-reporting and response platform for communities,
municipalities, universities, estates, organizations, and private communities.

## Tagline
**See It. Report It. Fix It.**

## The Lifecycle
```
Citizen reports issue
        ↓
System validates report
        ↓
Report enters review
        ↓
Moderator verifies report
        ↓
Report is assigned
        ↓
Responsible personnel work on issue
        ↓
Issue is marked resolved
        ↓
Citizen verifies/confirms resolution
        ↓
Report is closed
```

Every transition is recorded. The whole lifecycle is auditable.

## Three Experiences
- **Web** — responsive React + TypeScript app (citizen, moderator, admin, field worker, org)
- **Flutter Mobile** — single shared codebase for Android + iOS
- **Backend API** — centralized Spring Boot REST API (PostgreSQL + PostGIS)

## Quick Reference
- [Architecture](docs/architecture/ADR_0001_PROJECT_STRUCTURE.md)
- [API](docs/api/API.md)
- [Roadmap](docs/project-management/ROADMAP.md)
- [CI/CD](.github/workflows/ci.yml)
- [Issue Templates](.github/ISSUE_TEMPLATE)
- [Pull Request Template](.github/PULL_REQUEST_TEMPLATE/pull_request_template.md)
- [Coding Standards](docs/standards/CODING_STANDARDS.md)
- [Security Standards](docs/standards/SECURITY_STANDARDS.md)
