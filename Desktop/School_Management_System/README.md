# School Management System

[![CI](https://github.com/felix202422/School_Management_System/actions/workflows/ci.yml/badge.svg)](https://github.com/felix202422/School_Management_System/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Enterprise-grade School Management System: student records, attendance,
examinations, fees, library, hostel, transport, inventory, role-based portals
and analytics dashboards.

## Tech Stack

**Frontend** — React 19.3 · Vite 8 · React Router 7 · Axios 1.x ·
Recharts 3 · Material UI 9 · Lucide React · modern JavaScript (ES2023+)

**Backend** — Java 25 LTS · Spring Boot 4.1.1 · Spring Web MVC ·
Spring Data JPA (Hibernate) · Spring Security 7 · Spring Validation ·
JWT authentication · Apache Maven 3.9.x

**Database** — PostgreSQL 16

## Features

- 🔐 **JWT authentication** with refresh tokens, remember-me (30 days),
  account lockout, and BCrypt(12) password hashing
- 👥 **Role-based portals** — dedicated dashboards and menu filtering for
  admin, teacher, student, parent, accountant, librarian and more
- 📊 **Analytics dashboard** — live Recharts visualizations: gender
  distribution, attendance trend & distribution, monthly revenue, class sizes,
  grade distribution
- 🎓 **Academics** — students, teachers, classes, subjects, examinations,
  assignments (with submissions & grading), timetable
- 💰 **Finance** — fee invoices, payment recording, revenue reporting
- 📚 **Operations** — library borrowing, hostel allocation, transport routes,
  inventory, events
- 📤 **Reports & exports** — CSV / Excel / PDF exports (OpenCSV, Apache POI,
  iText) including per-student report cards, plus CSV student import
- 🧾 **Audit logging** via AOP, rate limiting, XSS sanitization,
  security headers
- 📖 **OpenAPI 3 documentation** (Swagger UI)

## Quick Start

### Prerequisites

- JDK 25, Maven 3.9+
- Node.js 20+
- PostgreSQL 16+

### 1. Configure environment

```bash
cp .env.example .env   # fill in POSTGRES_PASSWORD, JWT_SECRET, etc.
```

### 2. Database

```bash
psql -U postgres -c "CREATE DATABASE sms_db;"
psql -U postgres -d sms_db -f backend/src/main/resources/db/schema.sql
psql -U postgres -d sms_db -f backend/src/main/resources/db/data.sql
```

### 3. Backend

```bash
cd backend
DB_PASSWORD=yourpassword mvn spring-boot:run
```

API docs: http://localhost:8080/swagger-ui.html

### 4. Frontend

```bash
cd frontend
npm install
npm run dev           # http://localhost:5173
```

### Docker

```bash
cp .env.example .env  # required: POSTGRES_PASSWORD, JWT_SECRET
docker-compose up -d  # app on http://localhost
```

## Default Credentials

| Username | Password      | Role        |
| -------- | ------------- | ----------- |
| admin    | password123   | SUPER_ADMIN |

> Change immediately outside local development. Seed data includes ~2,000
> students, 60 teachers, attendance, exam results and payments for realistic
> dashboards.

## Role Portal Matrix

| Area        | Admin | Teacher | Student | Parent | Accountant | Librarian |
| ----------- | ----- | ------- | ------- | ------ | ---------- | --------- |
| Analytics   | ✅    | —       | —       | —      | —          | —         |
| Students    | ✅    | ✅      | —       | —      | —          | —         |
| Attendance  | ✅    | ✅      | view    | —      | —          | —         |
| Assignments | ✅    | ✅      | view    | view   | —          | —         |
| Timetable   | ✅    | ✅      | view    | view   | —          | —         |
| Fees        | ✅    | —       | —       | —      | ✅          | —         |
| Library     | ✅    | ✅      | view    | —      | —          | ✅         |
| Portal dashboard | ✅ | ✅    | ✅      | ✅     | —          | —         |

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Endpoint overview: [docs/API.md](docs/API.md)

## Project Structure

```
School_Management_System/
├── backend/                  # Spring Boot 4.1 application (Java 25)
│   └── src/main/
│       ├── java/com/sms/     # controller / service / repository / entity /
│       │                     # dto / security / config / aspect / exception
│       └── resources/
│           ├── application.properties
│           └── db/           # schema.sql (38 tables) + data.sql (seed)
├── frontend/                 # React 19 + Vite 8 application
│   └── src/
│       ├── components/       # layout, portal dashboards, guards
│       ├── context/          # Auth, Theme providers
│       ├── pages/            # one page per module
│       ├── services/         # Axios API layer
│       └── utils/            # constants, permissions (RBAC)
├── docs/                     # architecture, API guide, environment guide
├── .github/workflows/        # CI: backend + frontend builds
├── docker/                   # Dockerfiles
└── docker-compose.yml        # Postgres + backend + frontend
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — layers, request pipeline, RBAC design
- [API reference](docs/API.md) — endpoint catalog
- [Environment setup](docs/ENVIRONMENT.md) — all config variables
- [Contributing](CONTRIBUTING.md) · [Code of Conduct](CODE_OF_CONDUCT.md) ·
  [Security policy](SECURITY.md) · [Changelog](CHANGELOG.md)

## License

[MIT](LICENSE)
