# API Reference — MtaaFix

Base URL: `https://api.mtaafix.example.com/api/v1`

All responses are JSON. Errors follow a common envelope:

```json
{
  "timestamp": "2026-10-05T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "The request failed validation.",
  "path": "/api/v1/reports"
}
```

## Authenticate

### Register

`POST /auth/register`

```json
{
  "email": "citizen@example.com",
  "password": "Str0ngPass!",
  "fullName": "Jane Citizen",
  "mobile": "+255712345678",
  "role": "CITIZEN"
}
```

### Login

`POST /auth/login`

```json
{
  "email": "citizen@example.com",
  "password": "Str0ngPass!"
}
```

Returns `accessToken`, `refreshToken`, and `expiresIn`.

### Refresh

`POST /auth/refresh`

```json
{ "refreshToken": "..." }
```

### Logout

`POST /auth/logout`

```json
{ "refreshToken": "..." }
```

## Reports

### Create

`POST /reports`

```json
{
  "categoryId": "uuid",
  "title": "Streetlight out",
  "description": "The west-side streetlight on Main St has been out since morning.",
  "location": {
    "latitude": -6.1965,
    "longitude": 39.2083,
    "point": { "type": "Point", "coordinates": [39.2083, -6.1965] }
  }
}
```

Response: `201 Created` with a `Mtf-2026-<serial>` identifier.

### List

`GET /reports?status=VERIFIED&category=STREET_LIGHT&orgId=uuid&page=0&size=20`

Returns a paginated list of reports (citizens see their own; moderators/admins see all).

### Get one

`GET /reports/{id}`

### Update (own report)

`PATCH /reports/{id}`

### Submit

`POST /reports/{id}/submit`

### Reject

`POST /reports/{id}/reject`

```json
{ "comment": "No valid issue reported." }
```

### Verify

`POST /reports/{id}/verify`

### Assign

`POST /reports/{id}/assign`

```json
{ "assigneeId": "uuid", "organizationId": "uuid" }
```

### Resolve

`POST /reports/{id}/resolve`

### Close

`POST /reports/{id}/close`

### Add comment

`POST /reports/{id}/comments`

### Add evidence

`POST /reports/{id}/media`

## Media

### Upload

`POST /media`

Multipart with a `file` part. Validates MIME type, extension, and size.

### Delete

`DELETE /media/{id}`

## Dashboard

### Me

`GET /dashboard/me`

### Organization statistics

`GET /dashboard/organizations/{id}/stats`

### Global statistics

`GET /dashboard/stats`

## Categories

### List

`GET /categories`

### Create (admin)

`POST /categories`

## Assignments

### My list

`GET /assignments/me`

### Organization's reports

`GET /assignments/organizations/{id}/reports`

## Notifications

### My notifications

`GET /notifications`

### Mark read

`POST /notifications/{id}/read`

## Audit

### Organization audit log

`GET /audit/organizations/{id}`

### Global audit log (admin)

`GET /audit`

## Common Patterns

- `GET /reports` never exposes report content to unauthorized users.
- `PATCH /reports/{id}` is restricted to the report creator until the report is
  assigned.
- `POST /reports/{id}/close` requires a citizen confirmation payload.
