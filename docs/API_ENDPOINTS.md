# NextShift / UniShift Backend API Specification

This document is the implementation-aligned API specification and frontend integration contract for the **NextShift / UniShift** backend platform.

---

## 1) Quick Start for Frontend Team

- **API Base URL (Local)**: `http://localhost:8080` (configured via `SERVER_PORT`, default: `8080`)
- **API Version Prefix**: `/api/v1`
- **Application Base Path**: `http://localhost:8080/api/v1`
- **Health Check Endpoint** (outside version prefix): `GET http://localhost:8080/health`

On application startup, the backend outputs all registered routes and access links to the console via `EndpointPrinter`.

---

## 2) Authentication Model

Authentication uses stateless **JWT Bearer tokens** signed with HMAC-SHA256 (`HS256`).

1. User registers via `POST /api/v1/auth/register` or logs in via `POST /api/v1/auth/login`.
2. Both endpoints return an `AuthResponse` containing:
   - `token`: JWT string
   - `tokenType`: `Bearer`
   - `expiresIn`: Token validity in milliseconds (default: `86400000` ms / 24 hours)
   - `user`: Authenticated user profile (`UserResponse`)
3. Frontend attaches the token to all protected requests in the HTTP `Authorization` header:

```http
Authorization: Bearer <token>
```

### Token Facts
- Token expiration is controlled by `security.jwt.expiration-ms` (`ACCESS_TOKEN_EXPIRE_MINUTES` / `JWT_EXPIRATION_MS`).
- Tokens are completely stateless; permissions and roles are verified server-side on every request.
- No separate refresh token endpoint exists in MVP. When the token expires, the client must prompt re-login.

---

## 3) Role & Approval Rules (Important)

The platform supports three distinct system roles:
- `STUDENT`: Public user seeking part-time campus shifts.
- `EMPLOYER`: Shop owner / campus department offering shifts.
- `ADMIN`: Internal platform administrator verifying employers and approving shifts.

### Public Registration Rules
- Only `STUDENT` and `EMPLOYER` roles can register via `POST /api/v1/auth/register`.
- Registering with role `ADMIN` is strictly forbidden and rejected with `400 Bad Request`.
- Admin accounts are provisioned exclusively via secure bootstrap configuration (`ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`).

### Approval Behavior
- `STUDENT`: Immediately approved upon registration (`approved: true`, `active: true`).
- `EMPLOYER`: Registered in an unapproved state (`approved: false`, `active: true`) until an `ADMIN` approves the account.
- **Unapproved Employers** can log in, view own profile, and inspect existing jobs/bookings, but **cannot create jobs** (returns `403 Forbidden`).

### Capability Matrix

| Feature | Public | Student | Employer (Unapproved) | Employer (Approved) | Admin |
|---|:---:|:---:|:---:|:---:|:---:|
| Check Service Health (`/health`) | Yes | Yes | Yes | Yes | Yes |
| Register Account | Yes | No | No | No | No |
| Login & Obtain Token | Yes | Yes | Yes | Yes | Yes |
| View Own Profile (`/auth/me`) | No | Yes | Yes | Yes | Yes |
| Browse Published Job Feed (`/jobs`) | Yes | Yes | Yes | Yes | Yes |
| View Job Details (`/jobs/{id}`) | Yes | Yes | Yes | Yes | Yes |
| Create Job Vacancy | No | No | No (`403`) | Yes | No (`403`) |
| View Own Employer Jobs | No | No | Yes | Yes | No (`403`) |
| View Bookings for Own Job | No | No | Yes | Yes | No (`403`) |
| Book a Vacancy | No | Yes | No (`403`) | No (`403`) | No (`403`) |
| View Own Bookings (`/bookings/me`) | No | Yes | No (`403`) | No (`403`) | No (`403`) |
| List Pending Employers | No | No | No (`403`) | No (`403`) | Yes |
| Approve Employer | No | No | No (`403`) | No (`403`) | Yes |
| List Pending Job Vacancies | No | No | No (`403`) | No (`403`) | Yes |
| Approve / Publish Job Vacancy | No | No | No (`403`) | No (`403`) | Yes |

---

## 4) Common Response Shapes

### 4.1 Success Format
Endpoints return JSON objects, JSON arrays, or paginated envelopes as defined in the contract below.

### 4.2 Domain / Business Error Format (`ApiException`)
Custom application and domain errors adhere to the following structure:

```json
{
  "timestamp": "2026-11-22T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable business failure explanation"
}
```

### 4.3 Validation Error Format (`MethodArgumentNotValidException`)
When input validation fails on `@Valid` request bodies:

```json
{
  "timestamp": "2026-11-22T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "email": "Invalid email format",
    "password": "Password must be at least 6 characters",
    "payAmount": "Pay amount must be greater than zero"
  }
}
```

### 4.4 Typical HTTP Status Codes

- `200 OK`: Request succeeded. Returned on successful retrieval, login, and updates.
- `201 Created`: Resource successfully created (registration, job creation, booking).
- `400 Bad Request`: Business rule violation, invalid parameter, or input validation failure.
- `401 Unauthorized`: Missing, expired, or malformed JWT token, or bad login credentials.
- `403 Forbidden`: Authenticated user lacks required role, employer is unapproved, or resource ownership failed.
- `404 Not Found`: Resource does not exist or is not eligible for viewing.
- `409 Conflict`: Conflict with current state (e.g., duplicate email registration, duplicate booking, capacity exhausted).
- `500 Internal Server Error`: Unexpected server-side failure.

---

## 5) Data Models for Frontend

### 5.1 Auth Models

#### `RegisterRequest`
Request body for user registration.

```json
{
  "name": "Jane Doe",
  "email": "jane@student.uni.edu",
  "password": "SecurePassword123!",
  "role": "STUDENT"
}
```

| Field | Type | Required | Constraints / Notes |
|---|---|:---:|---|
| `name` | String | Yes | Non-blank |
| `email` | String | Yes | Non-blank, valid email format, unique |
| `password` | String | Yes | Non-blank, minimum 6 characters |
| `role` | String | Yes | Allowed values: `"STUDENT"`, `"EMPLOYER"` (`"ADMIN"` rejected) |

#### `LoginRequest`
Request body for authentication.

```json
{
  "email": "jane@student.uni.edu",
  "password": "SecurePassword123!"
}
```

| Field | Type | Required | Constraints / Notes |
|---|---|:---:|---|
| `email` | String | Yes | Non-blank, valid email format |
| `password` | String | Yes | Non-blank |

#### `AuthResponse`
Returned upon successful registration or login.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@student.uni.edu",
    "role": "STUDENT",
    "active": true,
    "approved": true,
    "createdAt": "2026-11-22T08:00:00Z"
  }
}
```

---

### 5.2 User Models

#### `UserResponse`
Represents public user identity and status profile.

```json
{
  "id": 1,
  "name": "Jane Doe",
  "email": "jane@student.uni.edu",
  "role": "STUDENT",
  "active": true,
  "approved": true,
  "createdAt": "2026-11-22T08:00:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | Long | Unique user identifier |
| `name` | String | Full user name |
| `email` | String | Unique user email |
| `role` | String | User role: `"STUDENT"`, `"EMPLOYER"`, or `"ADMIN"` |
| `active` | Boolean | Account active status (`true` = active, `false` = disabled) |
| `approved` | Boolean | Approval status (`true` = approved, `false` = pending) |
| `createdAt` | String (ISO-8601) | Timestamp when account was created |

---

### 5.3 Job Models

#### `CreateJobRequest`
Request payload submitted by approved employers to post a vacancy.

```json
{
  "title": "Barista Shift",
  "description": "Prepare espresso drinks during morning rush",
  "category": "Hospitality",
  "location": "Student Union Cafe",
  "payAmount": 20.00,
  "payUnit": "HOURLY",
  "startsAt": "2026-11-25T08:00:00Z",
  "endsAt": "2026-11-25T12:00:00Z",
  "capacity": 2
}
```

| Field | Type | Required | Constraints / Validation |
|---|---|:---:|---|
| `title` | String | Yes | Non-blank |
| `description` | String | Yes | Non-blank |
| `category` | String | Yes | Non-blank |
| `location` | String | Yes | Non-blank |
| `payAmount` | BigDecimal | Yes | Minimum `0.01` |
| `payUnit` | String | Yes | Non-blank (e.g. `"HOURLY"`, `"FIXED"`, `"per_hour"`) |
| `startsAt` | String (ISO-8601) | Yes | Must be in the future (`@Future`) |
| `endsAt` | String (ISO-8601) | Yes | Must be strictly after `startsAt` |
| `capacity` | Integer | Yes | Minimum `1` |

#### `JobResponse`
Standard vacancy response representation.

```json
{
  "id": 10,
  "employerId": 2,
  "employerName": "Campus Bookstore",
  "title": "Inventory Assistant",
  "description": "Stocking books and scanning barcodes",
  "category": "Retail",
  "location": "North Campus Bookstore",
  "payAmount": 22.50,
  "payUnit": "HOURLY",
  "startsAt": "2026-11-25T09:00:00Z",
  "endsAt": "2026-11-25T15:00:00Z",
  "capacity": 3,
  "confirmedBookings": 1,
  "remainingCapacity": 2,
  "status": "PUBLISHED",
  "createdAt": "2026-11-22T08:00:00Z",
  "publishedAt": "2026-11-22T09:30:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | Long | Unique vacancy identifier |
| `employerId` | Long | ID of employer who created the job |
| `employerName` | String | Name of employer |
| `title` | String | Job title |
| `description` | String | Detailed job description |
| `category` | String | Job category |
| `location` | String | Campus or facility location |
| `payAmount` | BigDecimal | Monetary remuneration rate |
| `payUnit` | String | Payment interval/type (e.g., `"HOURLY"`) |
| `startsAt` | String (ISO-8601) | Shift commencement timestamp |
| `endsAt` | String (ISO-8601) | Shift conclusion timestamp |
| `capacity` | Integer | Total worker capacity required |
| `confirmedBookings` | Long | Number of active confirmed student bookings |
| `remainingCapacity` | Integer | Remaining available booking slots (`max(0, capacity - confirmedBookings)`) |
| `status` | String | Job status: `"PENDING"`, `"PUBLISHED"`, `"REJECTED"`, or `"CLOSED"` |
| `createdAt` | String (ISO-8601) | Timestamp when vacancy was submitted |
| `publishedAt` | String (ISO-8601) | Timestamp when admin approved and published the vacancy (or `null`) |

---

### 5.4 Booking Models

#### `BookingResponse`
Represents a confirmed booking between a student and a vacancy.

```json
{
  "id": 1,
  "jobId": 10,
  "jobTitle": "Inventory Assistant",
  "jobLocation": "North Campus Bookstore",
  "studentId": 5,
  "studentName": "Alice Johnson",
  "studentEmail": "alice@student.uni.edu",
  "status": "CONFIRMED",
  "createdAt": "2026-11-22T10:15:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | Long | Unique booking identifier |
| `jobId` | Long | Associated job vacancy ID |
| `jobTitle` | String | Associated job vacancy title |
| `jobLocation` | String | Associated job vacancy location |
| `studentId` | Long | ID of student who made the booking |
| `studentName` | String | Name of student |
| `studentEmail` | String | Email of student |
| `status` | String | Booking status: `"CONFIRMED"` or `"CANCELLED"` |
| `createdAt` | String (ISO-8601) | Timestamp when booking was confirmed |

---

### 5.5 Pagination Envelope (`Page<JobResponse>`)

The public job feed utilizes standard Spring Data pagination:

```json
{
  "content": [
    {
      "id": 10,
      "employerId": 2,
      "employerName": "Campus Bookstore",
      "title": "Inventory Assistant",
      "description": "Stocking books and scanning barcodes",
      "category": "Retail",
      "location": "North Campus Bookstore",
      "payAmount": 22.50,
      "payUnit": "HOURLY",
      "startsAt": "2026-11-25T09:00:00Z",
      "endsAt": "2026-11-25T15:00:00Z",
      "capacity": 3,
      "confirmedBookings": 1,
      "remainingCapacity": 2,
      "status": "PUBLISHED",
      "createdAt": "2026-11-22T08:00:00Z",
      "publishedAt": "2026-11-22T09:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

---

## 6) Endpoint-by-Endpoint Contract

All paths below are relative to `/api/v1` unless explicitly noted as outside the prefix.

---

### 6.1 Health

#### `GET /health` (outside `/api/v1`)

- **Auth**: None (Public)
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`
```json
{
  "status": "UP",
  "service": "unishift-backend",
  "timestamp": "2026-11-22T10:00:00Z"
}
```
- **Notes**: Operational status probe suitable for load balancers and frontend uptime verification.
- **Errors**: None under normal operation.

---

### 6.2 Auth

#### `POST /auth/register`

- **Auth**: None (Public)
- **Query params**: None
- **Body**: `RegisterRequest`
```json
{
  "name": "Jane Doe",
  "email": "jane@student.uni.edu",
  "password": "SecurePassword123!",
  "role": "STUDENT"
}
```
- **Response**: `201 Created`, `AuthResponse`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@student.uni.edu",
    "role": "STUDENT",
    "active": true,
    "approved": true,
    "createdAt": "2026-11-22T08:00:00Z"
  }
}
```
- **Notes**:
  - Automatically issues a JWT bearer token upon successful registration.
  - Role `STUDENT` is immediately approved (`approved: true`).
  - Role `EMPLOYER` is created pending approval (`approved: false`).
- **Errors**:
  - `400 Bad Request`: Input validation failure (e.g. invalid email format, short password, or prohibited role `ADMIN`).
  - `409 Conflict`: Email address is already registered.

---

#### `POST /auth/login`

- **Auth**: None (Public)
- **Query params**: None
- **Body**: `LoginRequest`
```json
{
  "email": "jane@student.uni.edu",
  "password": "SecurePassword123!"
}
```
- **Response**: `200 OK`, `AuthResponse`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@student.uni.edu",
    "role": "STUDENT",
    "active": true,
    "approved": true,
    "createdAt": "2026-11-22T08:00:00Z"
  }
}
```
- **Notes**:
  - Authenticates credentials against BCrypt password hash.
  - Returns bearer token and user profile.
- **Errors**:
  - `400 Bad Request`: Missing or invalid email/password format.
  - `401 Unauthorized`: Invalid email or password credentials.
  - `403 Forbidden`: Account is inactive (`active: false`).

---

#### `GET /auth/me`

- **Auth**: Bearer token required (`STUDENT`, `EMPLOYER`, or `ADMIN`)
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `UserResponse`
```json
{
  "id": 1,
  "name": "Jane Doe",
  "email": "jane@student.uni.edu",
  "role": "STUDENT",
  "active": true,
  "approved": true,
  "createdAt": "2026-11-22T08:00:00Z"
}
```
- **Notes**:
  - Identifies caller from JWT security principal and returns fresh profile data.
- **Errors**:
  - `401 Unauthorized`: Missing, expired, or invalid token.
  - `404 Not Found`: Authenticated user not found in database.

---

### 6.3 Jobs & Vacancies

#### `GET /jobs`

- **Auth**: None (Public; optional Bearer token)
- **Query params**:
  - `category` (optional string): Case-insensitive category filter.
  - `location` (optional string): Case-insensitive location filter.
  - `page` (optional integer, default `0`, min `0`): Zero-based page index.
  - `size` (optional integer, default `20`, min `1`, max `100`): Page size limit.
  - `sort` (optional string, default `publishedAt,desc; id,desc`): Property sort order.
- **Body**: None
- **Response**: `200 OK`, `Page<JobResponse>`
- **Notes**:
  - Implements **Recommendation Strategy V0**: returns all eligible vacancies to all callers without personalization.
  - Eligibility criteria strictly enforced at the database query level:
    1. Vacancy status is `PUBLISHED`.
    2. Employer account is `active: true` AND `approved: true`.
    3. Shift start time is in the future (`startsAt > NOW()`).
    4. Has remaining capacity (`capacity > confirmedBookings`).
- **Errors**: None under normal operation.

---

#### `GET /jobs/{id}`

- **Auth**: None (Public)
- **Path params**:
  - `id` (Long, required): Job vacancy ID.
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `JobResponse`
```json
{
  "id": 10,
  "employerId": 2,
  "employerName": "Campus Bookstore",
  "title": "Inventory Assistant",
  "description": "Stocking books and scanning barcodes",
  "category": "Retail",
  "location": "North Campus Bookstore",
  "payAmount": 22.50,
  "payUnit": "HOURLY",
  "startsAt": "2026-11-25T09:00:00Z",
  "endsAt": "2026-11-25T15:00:00Z",
  "capacity": 3,
  "confirmedBookings": 1,
  "remainingCapacity": 2,
  "status": "PUBLISHED",
  "createdAt": "2026-11-22T08:00:00Z",
  "publishedAt": "2026-11-22T09:30:00Z"
}
```
- **Notes**:
  - Returns complete vacancy information if eligible.
- **Errors**:
  - `404 Not Found`: Job does not exist, status is not `PUBLISHED`, or `startsAt` has already passed.

---

#### `POST /jobs`

- **Auth**: Bearer token, `EMPLOYER` role, and employer must be approved (`approved: true`)
- **Query params**: None
- **Body**: `CreateJobRequest`
```json
{
  "title": "Barista Shift",
  "description": "Prepare espresso drinks during morning rush",
  "category": "Hospitality",
  "location": "Student Union Cafe",
  "payAmount": 20.00,
  "payUnit": "HOURLY",
  "startsAt": "2026-11-25T08:00:00Z",
  "endsAt": "2026-11-25T12:00:00Z",
  "capacity": 2
}
```
- **Response**: `201 Created`, `JobResponse`
- **Notes**:
  - Newly created vacancies are assigned initial status `PENDING`.
  - Vacancy will not appear on the public feed until an `ADMIN` approves it.
- **Errors**:
  - `400 Bad Request`: Input validation failed, or `endsAt` is not strictly after `startsAt`.
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_EMPLOYER`, or employer account has not been approved by an admin yet (`approved: false`).

---

### 6.4 Employer Management

#### `GET /employer/jobs`

- **Auth**: Bearer token, `EMPLOYER` role required
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `JobResponse[]`
```json
[
  {
    "id": 10,
    "employerId": 2,
    "employerName": "Campus Bookstore",
    "title": "Inventory Assistant",
    "description": "Stocking books and scanning barcodes",
    "category": "Retail",
    "location": "North Campus Bookstore",
    "payAmount": 22.50,
    "payUnit": "HOURLY",
    "startsAt": "2026-11-25T09:00:00Z",
    "endsAt": "2026-11-25T15:00:00Z",
    "capacity": 3,
    "confirmedBookings": 1,
    "remainingCapacity": 2,
    "status": "PUBLISHED",
    "createdAt": "2026-11-22T08:00:00Z",
    "publishedAt": "2026-11-22T09:30:00Z"
  }
]
```
- **Notes**:
  - Returns all vacancies owned by the authenticated employer across all statuses (`PENDING`, `PUBLISHED`, `REJECTED`, `CLOSED`).
  - Accessible to both approved and unapproved employers.
- **Errors**:
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_EMPLOYER`.

---

#### `GET /employer/jobs/{id}/bookings`

- **Auth**: Bearer token, `EMPLOYER` role and job ownership required
- **Path params**:
  - `id` (Long, required): Job vacancy ID.
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `BookingResponse[]`
```json
[
  {
    "id": 1,
    "jobId": 10,
    "jobTitle": "Inventory Assistant",
    "jobLocation": "North Campus Bookstore",
    "studentId": 5,
    "studentName": "Alice Johnson",
    "studentEmail": "alice@student.uni.edu",
    "status": "CONFIRMED",
    "createdAt": "2026-11-22T10:15:00Z"
  }
]
```
- **Notes**:
  - Lists student applications/bookings confirmed for the employer's vacancy.
- **Errors**:
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller is not an employer or does not own this vacancy.
  - `404 Not Found`: Job vacancy does not exist.

---

### 6.5 Bookings

#### `POST /jobs/{id}/bookings`

- **Auth**: Bearer token, `STUDENT` role required
- **Path params**:
  - `id` (Long, required): Job vacancy ID to book.
- **Query params**: None
- **Body**: None
- **Response**: `201 Created`, `BookingResponse`
```json
{
  "id": 1,
  "jobId": 10,
  "jobTitle": "Inventory Assistant",
  "jobLocation": "North Campus Bookstore",
  "studentId": 5,
  "studentName": "Alice Johnson",
  "studentEmail": "alice@student.uni.edu",
  "status": "CONFIRMED",
  "createdAt": "2026-11-22T10:15:00Z"
}
```
- **Notes**:
  - Bookings are confirmed immediately upon submission without requiring manual employer review.
  - Safe against race conditions: uses database-level pessimistic locking (`PESSIMISTIC_WRITE`) to evaluate real-time capacity and prevent overbooking.
- **Errors**:
  - `400 Bad Request`: Vacancy is ineligible (e.g. shift start time has already passed).
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_STUDENT`.
  - `404 Not Found`: Job does not exist or is not in `PUBLISHED` status.
  - `409 Conflict`:
    - Student has already booked this vacancy (`unique (job_id, student_id)` constraint).
    - Vacancy capacity is fully exhausted (`capacity <= confirmedBookings`).

---

#### `GET /bookings/me`

- **Auth**: Bearer token, `STUDENT` role required
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `BookingResponse[]`
```json
[
  {
    "id": 1,
    "jobId": 10,
    "jobTitle": "Inventory Assistant",
    "jobLocation": "North Campus Bookstore",
    "studentId": 5,
    "studentName": "Alice Johnson",
    "studentEmail": "alice@student.uni.edu",
    "status": "CONFIRMED",
    "createdAt": "2026-11-22T10:15:00Z"
  }
]
```
- **Notes**:
  - Returns list of all bookings belonging to the authenticated student, ordered by creation date descending.
- **Errors**:
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_STUDENT`.

---

### 6.6 Admin

#### `GET /admin/employers/pending`

- **Auth**: Bearer token, `ADMIN` role required
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `UserResponse[]`
```json
[
  {
    "id": 3,
    "name": "Bob Employer",
    "email": "bob@cafe.com",
    "role": "EMPLOYER",
    "active": true,
    "approved": false,
    "createdAt": "2026-11-22T08:30:00Z"
  }
]
```
- **Notes**:
  - Returns list of employers registered with `approved: false`.
- **Errors**:
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_ADMIN`.

---

#### `PATCH /admin/employers/{id}/approve`

- **Auth**: Bearer token, `ADMIN` role required
- **Path params**:
  - `id` (Long, required): User ID of the employer to approve.
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `UserResponse`
```json
{
  "id": 3,
  "name": "Bob Employer",
  "email": "bob@cafe.com",
  "role": "EMPLOYER",
  "active": true,
  "approved": true,
  "createdAt": "2026-11-22T08:30:00Z"
}
```
- **Notes**:
  - Atomically marks the employer account as approved (`approved: true`).
  - Enables the employer to create job vacancies.
- **Errors**:
  - `400 Bad Request`: User exists but is not an `EMPLOYER`.
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_ADMIN`.
  - `404 Not Found`: User not found.

---

#### `GET /admin/jobs/pending`

- **Auth**: Bearer token, `ADMIN` role required
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `JobResponse[]`
```json
[
  {
    "id": 11,
    "employerId": 3,
    "employerName": "Bob Employer",
    "title": "Evening Barista",
    "description": "Making coffees for study sessions",
    "category": "Hospitality",
    "location": "Library Cafe",
    "payAmount": 19.50,
    "payUnit": "HOURLY",
    "startsAt": "2026-11-26T17:00:00Z",
    "endsAt": "2026-11-26T21:00:00Z",
    "capacity": 2,
    "confirmedBookings": 0,
    "remainingCapacity": 2,
    "status": "PENDING",
    "createdAt": "2026-11-22T09:00:00Z",
    "publishedAt": null
  }
]
```
- **Notes**:
  - Returns list of vacancies submitted by employers awaiting admin moderation (`status = PENDING`).
- **Errors**:
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_ADMIN`.

---

#### `PATCH /admin/jobs/{id}/approve`

- **Auth**: Bearer token, `ADMIN` role required
- **Path params**:
  - `id` (Long, required): Job vacancy ID to approve.
- **Query params**: None
- **Body**: None
- **Response**: `200 OK`, `JobResponse`
```json
{
  "id": 11,
  "employerId": 3,
  "employerName": "Bob Employer",
  "title": "Evening Barista",
  "description": "Making coffees for study sessions",
  "category": "Hospitality",
  "location": "Library Cafe",
  "payAmount": 19.50,
  "payUnit": "HOURLY",
  "startsAt": "2026-11-26T17:00:00Z",
  "endsAt": "2026-11-26T21:00:00Z",
  "capacity": 2,
  "confirmedBookings": 0,
  "remainingCapacity": 2,
  "status": "PUBLISHED",
  "createdAt": "2026-11-22T09:00:00Z",
  "publishedAt": "2026-11-22T10:30:00Z"
}
```
- **Notes**:
  - Transitions job status from `PENDING` to `PUBLISHED`.
  - Sets `publishedAt` to the current UTC timestamp.
  - Vacancy immediately becomes visible on the public feed and open for student bookings.
- **Errors**:
  - `400 Bad Request`: Job is not in `PENDING` status, or the vacancy owner is not an active, approved employer.
  - `401 Unauthorized`: Missing or invalid token.
  - `403 Forbidden`: Caller does not have `ROLE_ADMIN`.
  - `404 Not Found`: Job vacancy not found.

---

## 7) Frontend Integration Flows

### 7.1 Student Flow

```
1. Register Student: POST /api/v1/auth/register (role: "STUDENT")
      │  └─ Returns 201 Created with JWT access_token & user profile (approved: true)
      ▼
2. Login (if returning): POST /api/v1/auth/login
      │  └─ Returns 200 OK with JWT access_token
      ▼
3. Browse Shifts: GET /api/v1/jobs?category=Retail&location=Campus&page=0&size=20
      │  └─ Displays paginated eligible vacancies (startsAt in future, capacity remaining)
      ▼
4. View Shift Details: GET /api/v1/jobs/{id}
      │
      ▼
5. Instant Booking: POST /api/v1/jobs/{id}/bookings
      │  └─ Returns 201 Created with BookingResponse (status: "CONFIRMED")
      ▼
6. View Own Bookings: GET /api/v1/bookings/me
         └─ Displays student's confirmed booking history
```

### 7.2 Employer Flow

```
1. Register Employer: POST /api/v1/auth/register (role: "EMPLOYER")
      │  └─ Returns 201 Created with JWT access_token & user profile (approved: false)
      ▼
2. Await Verification:
      │  └─ While unapproved, POST /api/v1/jobs returns 403 Forbidden
      │  └─ Can view own profile (GET /auth/me) and existing vacancies (GET /employer/jobs)
      ▼
3. Admin Approval: Admin marks employer approved (PATCH /admin/employers/{id}/approve)
      ▼
4. Post Shift: POST /api/v1/jobs
      │  └─ Returns 201 Created with JobResponse (status: "PENDING")
      ▼
5. Await Job Publication:
      │  └─ Admin verifies and approves shift (PATCH /admin/jobs/{id}/approve)
      │  └─ Status changes to "PUBLISHED"; shift appears on public feed
      ▼
6. Track Applicants: GET /api/v1/employer/jobs/{id}/bookings
         └─ Inspects list of students who have booked shifts
```

### 7.3 Admin Flow

```
1. Login as Admin: POST /api/v1/auth/login
      │  └─ Credentials configured via environment bootstrap
      ▼
2. Moderation Dashboard:
      ├─ GET /api/v1/admin/employers/pending (Review unapproved employers)
      └─ GET /api/v1/admin/jobs/pending (Review unapproved job vacancies)
      ▼
3. Verification Actions:
      ├─ PATCH /api/v1/admin/employers/{id}/approve (Approve employer)
      └─ PATCH /api/v1/admin/jobs/{id}/approve (Approve & publish vacancy)
```

---

## 8) Frontend Implementation Notes

- **Token Storage & Transmission**:
  - Securely persist the JWT token in frontend state, `sessionStorage`, or an `HttpOnly` cookie.
  - Include the `Authorization: Bearer <token>` header on every authenticated request.
- **Handling Auth Errors (`401 Unauthorized`)**:
  - Intercept `401` responses in your HTTP client (e.g. Axios interceptor or Fetch wrapper).
  - Clear expired credentials from storage and redirect the user to the login screen.
- **Handling Permission Restrictions (`403 Forbidden`)**:
  - Distinguish between role mismatches (e.g., student attempting to post a job) and unapproved employer state (`approved: false`).
  - Render an "Account Pending Verification" banner for unapproved employers instead of a generic error.
- **Handling Booking Conflicts (`409 Conflict`)**:
  - Present user-friendly notifications for conflict messages:
    - `"You have already booked this job"` (Duplicate booking).
    - `"Job capacity has been reached"` (Shift slots filled concurrently).
- **Error Body Parsing**:
  - Check whether `details` exists in error responses:
    - If `details` object is present, map field errors to individual UI input components.
    - If only `message` is present, display it in a toast or alert banner.
- **Timestamp Handling**:
  - All timestamps (`createdAt`, `startsAt`, `endsAt`, `publishedAt`) are serialized in ISO-8601 UTC format (e.g. `2026-11-25T09:00:00Z`).
  - Parse and format these into local user timezones using date libraries (such as `date-fns` or Day.js).
- **Zero-Indexed Pagination**:
  - Spring Data pagination uses `0` as the first page (`page=0`). Adjust UI pagination components that use 1-based indexing accordingly.

---

## 9) Environment & Configuration Caveats

1. **Default Server Port**:
   - The Spring Boot server runs on port `8080` by default (`http://localhost:8080`).
   - If using a reverse proxy or alternate port, set `SERVER_PORT` in your `.env` or application config.
2. **Initial Admin Account Bootstrap**:
   - Admin accounts cannot be created via the register endpoint.
   - Default initial admin account is provisioned during startup using the following environment variables:
     - `ADMIN_NAME`: `Platform Administrator`
     - `ADMIN_EMAIL`: `admin@unishift.com`
     - `ADMIN_PASSWORD`: `AdminSecurePassword123!`
3. **Email Validation Rules**:
   - Standard RFC-compliant email formatting is enforced via `@Email`. Avoid unconventional test domains like `.local` to ensure smooth validation across environments.
4. **CORS Policy**:
   - Configured in `SecurityConfig.java` to allow standard HTTP methods (`GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS`) and headers from frontend clients.
