# API Overview

Base URL: `http://localhost:8080/api` (configurable via `VITE_API_URL` on the
frontend). Interactive docs: `http://localhost:8080/swagger-ui.html`.

All responses use a common envelope:

```json
{ "success": true, "message": "Success", "data": { }, "timestamp": "..." }
```

Errors return the same shape with `success: false`, an appropriate HTTP status
and a human-readable `message`.

## Authentication

All endpoints except `/auth/**` and Swagger require:

```
Authorization: Bearer <accessToken>
```

| Method | Path                  | Description                          |
| ------ | --------------------- | ------------------------------------ |
| POST   | `/auth/login`         | Login; body `{ username, password, rememberMe? }` |
| POST   | `/auth/refresh`       | Exchange refresh token for new access token |
| POST   | `/auth/logout`        | Invalidate session server-side       |
| POST   | `/auth/change-password` | Change own password                |
| POST   | `/auth/forgot-password` | Generate reset token               |
| POST   | `/auth/reset-password`  | Reset password with token          |

## Dashboard

| Method | Path                  | Notes                                          |
| ------ | --------------------- | ---------------------------------------------- |
| GET    | `/dashboard/admin`    | Stats + `charts` (gender, attendance, revenue, grades, class sizes) |
| GET    | `/dashboard/teacher`  | Classes, subjects, assignments for the logged-in teacher |
| GET    | `/dashboard/student`  | Attendance rate, assignments due, results count |
| GET    | `/dashboard/parent`   | Linked children with class info                |

## Students

| Method | Path                        | Description                     |
| ------ | --------------------------- | ------------------------------- |
| GET    | `/students`                 | Paged list (filters supported)  |
| GET    | `/students/{id}`            | Detail                          |
| GET    | `/students/search?q=`       | Search by name/admission number |
| POST   | `/students`                 | Create (admin)                  |
| PUT    | `/students/{id}`            | Update (admin)                  |
| DELETE | `/students/{id}`            | Delete (admin)                  |
| GET    | `/students/by-class/{id}`   | Class roster                    |

## Assignments

| Method | Path                                        | Description              |
| ------ | ------------------------------------------- | ------------------------ |
| GET    | `/assignments`                              | Paged list               |
| GET    | `/assignments/{id}`                         | Detail                   |
| POST   | `/assignments`                              | Create (teacher/admin)   |
| PUT    | `/assignments/{id}`                         | Update                   |
| DELETE | `/assignments/{id}`                         | Delete                   |
| POST   | `/assignments/{id}/submit`                  | Student submission       |
| GET    | `/assignments/{id}/submissions`             | List submissions (grader)|
| POST   | `/assignments/submissions/{submissionId}/grade` | Grade a submission  |

## Timetable

| Method | Path                                  | Description               |
| ------ | ------------------------------------- | ------------------------- |
| GET    | `/timetable`                          | Paged list                |
| POST   | `/timetable`                          | Create period             |
| PUT    | `/timetable/{id}`                     | Update period             |
| DELETE | `/timetable/{id}`                     | Delete period             |
| GET    | `/timetable/class/{classId}`          | Full week for a class     |
| GET    | `/timetable/class/{classId}/day/{day}`| One day (`MONDAY`…`FRIDAY`) |
| GET    | `/timetable/teacher/{teacherId}`      | Full week for a teacher   |
| GET    | `/timetable/teacher/{teacherId}/day/{day}` | One day              |

## Subjects

CRUD at `/subjects` (GET/POST/PUT/DELETE), plus `GET /subjects/department/{id}`.

## Examinations & Results

CRUD at `/examinations`; `POST /examinations/results` records a result;
`GET /examinations/{id}/results` lists results; student history at
`/examinations/results/student/{studentId}`.

## Fees

Invoice and payment management at `/fees/invoices` and `/fees/payments`
(student-scoped variants included).

## Library, Hostel, Transport, Inventory

Module CRUD under `/library`, `/hostel`, `/transport`, `/inventory` — see
Swagger for full schemas.

## Reports & Exports

| Method | Path                                        | Output                     |
| ------ | ------------------------------------------- | -------------------------- |
| GET    | `/reports/students/export?format=csv\|excel\|pdf` | File download        |
| POST   | `/reports/students/import`                  | CSV import (multipart)     |
| GET    | `/reports/attendance/export`                | Attendance export          |
| GET    | `/reports/payments/export`                  | Payments export            |
| GET    | `/reports/examinations/{id}/results/export` | Exam results export        |
| GET    | `/reports/report-card?studentId=&examinationId=` | Report-card PDF       |

## Other

- `GET/POST/DELETE /events` — school events
- `GET /audit` — audit log (admin)
- `/api-docs` — OpenAPI 3 JSON for code generation
