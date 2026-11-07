# UniShift / NextShift — Campus Part-Time Job Platform Backend

A production-grade, fully functional REST API backend for the NextShift campus part-time job platform, connecting university students with campus employers. Built using **Java 21**, **Spring Boot**, **Spring Security**, **Spring Data JPA**, **Flyway**, and **JJWT**.

---

## Features

- **Role-Based Access Control**: Strict segregation between `STUDENT`, `EMPLOYER`, and `ADMIN`.
- **Stateless JWT Authentication**: HMAC-SHA256 bearer tokens with configurable expiration.
- **Admin Verification Lifecycle**:
  - Employers register with pending approval status until verified by an Admin.
  - Job vacancies created by approved employers remain pending until reviewed and approved by an Admin.
- **Recommendation Algorithm V0**:
  - Deterministic feed displaying all eligible vacancies (published, future-dated, approved active employer, with remaining capacity).
  - Single-query DB pagination (`publishedAt DESC, id DESC`) preventing N+1 queries.
  - Strategy Pattern interface allowing pluggable future algorithms.
- **Booking Safety & Concurrency**:
  - Pessimistic write locking (`PESSIMISTIC_WRITE`) preventing overbooking under concurrent requests.
  - Database-level unique constraint preventing duplicate bookings per student.
  - Immediate `CONFIRMED` booking upon validation.
- **Configurable Environment**: Full `.env` file support for database credentials and application secrets.

---

## Tech Stack

- **Java**: 21
- **Framework**: Spring Boot 4.1.2
- **Persistence**: Spring Data JPA / Hibernate ORM
- **Database**: PostgreSQL (Neon-ready), with H2 available for tests
- **Security**: Spring Security 7.x, BCrypt PasswordEncoder, JJWT (io.jsonwebtoken 0.12.6)
- **Database Migration**: Flyway
- **Build Tool**: Maven Wrapper (`mvnw`)

---

## Prerequisites

- **Java Development Kit (JDK)**: 21+
- **PostgreSQL-compatible database**: local PostgreSQL or Neon Postgres
- **Environment file**: a `.env` file in the project root for runtime configuration

---

## Configuration & Environment Variables

Create a `.env` file in the project root with your runtime settings:

```env
PROJECT_NAME="NextShift"
ENVIRONMENT="development"
DEBUG=true
API_V1_STR="/api/v1"

DATABASE_URL="jdbc:postgresql://<host>:5432/<database>?sslmode=require&channel_binding=require"
DATABASE_USERNAME="<username>"
DATABASE_PASSWORD="<password>"

JWT_SECRET_KEY="change-me-in-production"
JWT_ALGORITHM="HS256"
ACCESS_TOKEN_EXPIRE_MINUTES=60

FIRST_ADMIN_NAME="System Administrator"
FIRST_ADMIN_EMAIL="admin@example.com"
FIRST_ADMIN_PASSWORD="AdminSecurePassword123!"
```

Notes:
- `DATABASE_URL` must be a full JDBC PostgreSQL URL, for example `jdbc:postgresql://...`
- The project accepts both the newer names (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`) and legacy compatibility keys.
- The app loads values from `.env` automatically at startup via the main entrypoint.

---

## Build and Run

### Load Environment Variables
```bash
set -a
. ./.env
set +a
```

### Run Automated Tests
```bash
./mvnw clean test
```

### Start the Backend Application
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

This setup has been validated against a Neon PostgreSQL instance using the runtime environment variables above.

---

## End-to-End API Walkthrough

### 1. Health Check
```bash
curl -X GET http://localhost:8080/health
```

---

### 2. Employer Registers
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Campus Cafe",
    "email": "cafe@campus.edu",
    "password": "EmployerPassword123!",
    "role": "EMPLOYER"
  }'
```
*The employer account is created with `approved: false`.*

---

### 3. Admin Log In & Approve Employer
```bash
# Log in as Admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@unishift.com",
    "password": "AdminSecurePassword123!"
  }' | jq -r '.token')

# List pending employers
curl -X GET http://localhost:8080/api/v1/admin/employers/pending \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Approve employer (e.g. ID 2)
curl -X PATCH http://localhost:8080/api/v1/admin/employers/2/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### 4. Approved Employer Creates Job Vacancy
```bash
# Log in as Employer
EMPLOYER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cafe@campus.edu",
    "password": "EmployerPassword123!"
  }' | jq -r '.token')

# Create a vacancy (starts in PENDING status)
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer $EMPLOYER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Barista Shift",
    "description": "Morning shift making coffee and attending counter",
    "category": "Hospitality",
    "location": "North Campus Cafe",
    "payAmount": 22.00,
    "payUnit": "HOURLY",
    "startsAt": "2026-11-15T08:00:00Z",
    "endsAt": "2026-11-15T12:00:00Z",
    "capacity": 2
  }'
```

---

### 5. Admin Approves Job Vacancy
```bash
# List pending jobs
curl -X GET http://localhost:8080/api/v1/admin/jobs/pending \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Approve job (e.g. ID 1)
curl -X PATCH http://localhost:8080/api/v1/admin/jobs/1/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```
*The job is now `PUBLISHED` with its `publishedAt` timestamp populated.*

---

### 6. Public Vacancy Feed
```bash
curl -X GET "http://localhost:8080/api/v1/jobs?category=Hospitality&page=0&size=10"
```

---

### 7. Student Registers & Books Vacancy
```bash
# Register Student
STUDENT_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Student",
    "email": "jane@student.uni.edu",
    "password": "StudentPassword123!",
    "role": "STUDENT"
  }' | jq -r '.token')

# Book vacancy 1
curl -X POST http://localhost:8080/api/v1/jobs/1/bookings \
  -H "Authorization: Bearer $STUDENT_TOKEN"

# View student's confirmed bookings
curl -X GET http://localhost:8080/api/v1/bookings/me \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```

---

### 8. Employer Inspects Bookings for Job
```bash
curl -X GET http://localhost:8080/api/v1/employer/jobs/1/bookings \
  -H "Authorization: Bearer $EMPLOYER_TOKEN"
```

---

## Documentation

Detailed architecture and design specifications are located in the `docs/` folder:
- [Architecture & OOP Design](docs/ARCHITECTURE.md)
- [API Endpoints Reference](docs/API_ENDPOINTS.md)
- [Job Recommendation Strategy V0](docs/RECOMMENDATION_ALGORITHM.md)
- [Development Status](docs/DEVELOPMENT_STATUS.md)
