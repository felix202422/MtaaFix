# Coding Standards — MtaaFix

## Language & Toolchain

- Backend: **Java 25**, Maven, Spring Boot 4.1.1.
- Web: **TypeScript / React 18** on Vite.
- Mobile: **Dart / Flutter**, single shared codebase for Android + iOS.
- Formatters: Prettier (web), `flutter format` (mobile), `mvn formatter:format` or
  equivalent for Java.

## Naming

- Packages: lowercase reverse-DNS `com.mtaafix.*`.
- Classes: `PascalCase`. Methods/fields: `camelCase`. Constants: `UPPER_SNAKE_CASE`.
- Controllers: `*Controller`. Services: `*Service`. Repositories: `*Repository`.
  DTOs: `*Dto`. Mappers: `*Mapper`. Exceptions: `*Exception`.

## Structure

- One concern per package; keep domain logic out of controllers.
- Controllers delegate to services; services coordinate repositories and mappers.
- Never expose JPA entities through REST. Use DTOs for request and response bodies.

## Error Handling

- Centralize exception handling in `com.mtaafix.common.exception.GlobalExceptionHandler`.
- Return structured JSON errors (timestamp, status, code, message, path).
- Never leak stack traces or SQL/DB internals to API consumers.

## Security

- All endpoints secured with Spring Security; JWT access + refresh tokens.
- Passwords hashed with BCrypt; never store plaintext.
- Validate all input with Bean Validation and service-level guards.
- Rate-limit auth and report submission endpoints.

## Testing

- Unit tests for services/mappers/validation, using Mockito.
- Node-level integration tests with Spring Boot Test + a real PostgreSQL/PostGIS
  container (Testcontainers).
- Keep the CI pipeline green: `mvn -f backend/pom.xml verify`, `npm run build`,
  `flutter analyze`.

## Commits & PRs

- Conventional commits: `feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`.
- Every PR references an issue and updates docs/CHANGELOG where relevant.
