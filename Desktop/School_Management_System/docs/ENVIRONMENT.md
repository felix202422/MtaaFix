# Environment & Configuration

All secrets are supplied via environment variables. Application properties
reference them with `${VAR:default}` syntax, so the app runs locally with
sensible defaults but production credentials come from the environment.

## Backend variables

| Variable                  | Used in                  | Default (local dev)        | Notes                          |
| ------------------------- | ------------------------ | -------------------------- | ------------------------------ |
| `DB_URL`                  | `spring.datasource.url`  | `jdbc:postgresql://localhost:5432/sms_db` | Full JDBC URL |
| `DB_USERNAME`             | datasource username      | `postgres`                 |                                |
| `DB_PASSWORD`             | datasource password      | *(empty — must be set)*    | Required to start              |
| `SERVER_PORT`             | `server.port`            | `8080`                     | Use 8090 locally if 8080 is taken |
| `JWT_SECRET`              | `app.jwt.secret`         | dev-only fallback          | **Must** override in production: `openssl rand -base64 48` |
| `CORS_ALLOWED_ORIGINS`    | `app.cors.allowed-origins` | `http://localhost:5173,http://localhost:3000` | Comma-separated |
| `MAIL_HOST` / `MAIL_PORT` | SMTP                     | `smtp.gmail.com` / `587`   |                                |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP auth        | empty                      | Required for password-reset email |

## Frontend variables

| Variable       | Used in          | Default                     | Notes                            |
| -------------- | ---------------- | --------------------------- | -------------------------------- |
| `VITE_API_URL` | `services/api.js`| `http://localhost:8080/api` | Base URL of backend REST API     |

Only variables prefixed `VITE_` are exposed to the Vite client bundle.
Create `frontend/.env.local` for local overrides.

## Docker Compose variables

See `.env.example` at the repo root. `POSTGRES_PASSWORD` and `JWT_SECRET` are
required (compose refuses to start without them).

## Quick start

```bash
# 1. Configure
cp .env.example .env      # then edit values
# backend: export DB_PASSWORD=... (or use your shell profile / IDE run config)

# 2. Database
psql -U postgres -c "CREATE DATABASE sms_db;"
psql -U postgres -d sms_db -f backend/src/main/resources/db/schema.sql
psql -U postgres -d sms_db -f backend/src/main/resources/db/data.sql

# 3. Backend (8090 if 8080 is occupied)
cd backend
DB_PASSWORD=... mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8090

# 4. Frontend
cd frontend
npm install
npm run dev               # http://localhost:5173
```

Default seeded login: `admin` / `password123` (change immediately in any
non-local environment).

## Rotating secrets

1. Generate new values (`openssl rand -base64 48` for `JWT_SECRET`).
2. Update the environment/secret store — never commit them.
3. Restart the backend; all issued tokens become invalid, forcing re-login.
