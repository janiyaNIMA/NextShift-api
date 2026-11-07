# NextShift — Antigravity Implementation Prompt (Java 21 + Spring Boot)

You are a senior Java backend engineer. Implement the NextShift MVP in my **existing Spring Boot project**, which I have initialized **without dependencies**. Use **Java 21**. Inspect the repository first, preserve its existing Maven or Gradle build, package name, Spring Boot version, and application entry point. Do not create a second project.

## Project purpose

NextShift is a campus part-time job platform connecting students with employers/shop owners. There are two public user roles, STUDENT and EMPLOYER, plus an internal ADMIN role for verification and approvals.

MVP flow: Employer registers → Admin approves employer → Employer creates job vacancy → Admin approves vacancy → Student sees published vacancies → Student books a vacancy → Booking is automatically confirmed if capacity remains.

Keep the MVP focused. Do not add payments, messaging, ratings, mobile applications, AI, or personalized recommendations.

## Technology and dependencies

- Java 21 and the already configured compatible Spring Boot version.
- Preserve the existing build tool (Maven or Gradle).
- Add Spring Web, Spring Data JPA, Spring Security, Spring Validation, MySQL Connector/J, Flyway and Flyway MySQL support if needed, a maintained JWT library, Spring Boot Test, Spring Security Test, and Testcontainers MySQL for integration tests if available.
- Use Jakarta APIs compatible with the selected Spring Boot version.
- Prefer constructor injection, DTO records, service classes, repositories, and conventional JPA entities.
- Use environment variables for database credentials and JWT secret. Create `.env.example` and `.gitignore`; never commit secrets.
- Do not introduce unnecessary infrastructure.

## Architecture

Adapt the following to the existing base package:

```text
src/main/java/<base-package>/
  config/SecurityConfig.java
  security/JwtService.java
  security/JwtAuthenticationFilter.java
  auth/AuthController.java
  auth/AuthService.java
  auth/dto/
  user/User.java
  user/UserRole.java
  user/UserRepository.java
  job/Job.java
  job/JobStatus.java
  job/JobRepository.java
  job/JobController.java
  job/JobService.java
  job/dto/
  booking/Booking.java
  booking/BookingStatus.java
  booking/BookingRepository.java
  booking/BookingController.java
  booking/BookingService.java
  booking/dto/
  employer/EmployerController.java
  admin/AdminController.java
  admin/AdminService.java
  recommendation/JobRecommendationStrategy.java
  recommendation/AllPublishedJobsStrategy.java
  common/ApiExceptionHandler.java
  common/HealthController.java
src/main/resources/
  application.yml
  db/migration/
src/test/java/<base-package>/
docs/ARCHITECTURE.md
docs/API_ENDPOINTS.md
docs/RECOMMENDATION_ALGORITHM.md
docs/DEVELOPMENT_STATUS.md
README.md
.env.example
```

Keep controllers thin and business logic in services. Use Spring Data repositories for persistence, Bean Validation for inputs, DTOs for API output, and `@Transactional` for multi-step operations. Do not return JPA entities directly from controllers.

## Data model

**User:** id, name, unique email, passwordHash, role (STUDENT, EMPLOYER, ADMIN), active, approved, createdAt. Students can use the platform after registering. Employers initially require admin approval. Public registration must never create an ADMIN.

**Job:** id, employer (FK User), title, description, category, location, payAmount (BigDecimal), payUnit, startsAt, endsAt, capacity (>=1), status (PENDING, PUBLISHED, REJECTED, CLOSED), createdAt, publishedAt. New vacancies always start PENDING.

**Booking:** id, job (FK Job), student (FK User), status (CONFIRMED, CANCELLED), createdAt. Unique `(job_id, student_id)`; cancellation and rebooking are outside the MVP.

Create Flyway migrations with constraints and indexes. Use UTC timestamps consistently. Configure JPA schema validation rather than automatic schema creation/update in regular development. Validate dates, pay, capacity, and required fields.

## Authentication and security

- Hash passwords using Spring Security PasswordEncoder (BCrypt or Argon2).
- Implement stateless JWT bearer authentication with configurable secret and expiration.
- Registration accepts only STUDENT or EMPLOYER.
- Admin account bootstrap must be an explicit secure command/profile using environment-supplied credentials, never a public endpoint or hardcoded password.
- Enforce role checks and ownership checks server-side.
- Unapproved employers cannot create jobs.
- Students cannot create jobs; employers cannot book jobs.
- Employers can see only bookings for their own jobs.
- Students can see only their own bookings.
- Never expose password hashes or private user data.
- Use modern Spring Security configuration and appropriate CORS settings.

## API endpoints

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/health` | Public | Health check |
| POST | `/api/v1/auth/register` | Public | Student/employer registration |
| POST | `/api/v1/auth/login` | Public | JWT login |
| GET | `/api/v1/auth/me` | Authenticated | Current user |
| GET | `/api/v1/jobs` | Public | Paginated published job feed; optional category/location filters |
| GET | `/api/v1/jobs/{id}` | Public | Eligible job details |
| POST | `/api/v1/jobs` | Approved employer | Create pending vacancy |
| GET | `/api/v1/employer/jobs` | Employer | Own vacancies |
| GET | `/api/v1/employer/jobs/{id}/bookings` | Employer owner | Bookings for own vacancy |
| POST | `/api/v1/jobs/{id}/bookings` | Student | Confirm booking |
| GET | `/api/v1/bookings/me` | Student | Own bookings |
| GET | `/api/v1/admin/employers/pending` | Admin | Pending employers |
| PATCH | `/api/v1/admin/employers/{id}/approve` | Admin | Approve employer |
| GET | `/api/v1/admin/jobs/pending` | Admin | Pending jobs |
| PATCH | `/api/v1/admin/jobs/{id}/approve` | Admin | Publish vacancy |

Use request/response DTOs, pagination, consistent JSON errors, and suitable HTTP status codes (401, 403, 404, 409, 422/400). Preserve any equivalent endpoints already implemented.

## Job recommendation algorithm V0

Create **separate files**:

- `recommendation/JobRecommendationStrategy.java`
- `recommendation/AllPublishedJobsStrategy.java`

For the MVP, **show every eligible vacancy to every student**. Do not personalize or score results. A job is eligible if it is PUBLISHED, its employer is active and approved, its start time is in the future, and it has remaining booking capacity. Sort by `publishedAt DESC, id DESC` and paginate at the database level. Optional category and location filters are user-selected search filters, not recommendations.

Define a strategy interface so later algorithms (skill-based, location-based, hybrid) can be swapped in without rewriting controllers. Ensure filtering and capacity calculations are efficient and avoid N+1 queries. Document the algorithm in `docs/RECOMMENDATION_ALGORITHM.md`.

## Booking correctness

- Only a student can book a future, eligible published job.
- Booking is CONFIRMED immediately, without employer approval.
- Prevent duplicate bookings with both service validation and a database unique constraint.
- Prevent overbooking under concurrent requests using a transaction and a lock on the relevant Job row before counting confirmed bookings.
- Reject full jobs and duplicate bookings with 409 Conflict.
- Keep feed eligibility consistent with booking availability.

## OOP requirements

This is an Object-Oriented Programming university project. Demonstrate meaningful:

- **Encapsulation:** private fields and controlled domain/service methods.
- **Abstraction:** `JobRecommendationStrategy` interface.
- **Polymorphism:** service depends on the strategy interface, not a concrete implementation.
- **Inheritance:** use it where justified, such as a shared auditable base entity; do not force a complicated Student/Employer/Admin JPA hierarchy merely to demonstrate inheritance.

Explain the choices in `docs/ARCHITECTURE.md`. **Do not put comments in generated Java source code**; use clear naming and separate Markdown documentation instead.

## Implementation phases

1. Inspect repository and build configuration; add dependencies and configuration.
2. Implement health endpoint, MySQL connectivity, entities, and Flyway migrations.
3. Implement registration, password hashing, login, JWT, role checks, and secure admin bootstrap.
4. Implement employer approval, vacancy creation, and vacancy approval.
5. Implement recommendation strategy V0 and paginated public vacancy feed.
6. Implement booking, capacity safeguards, and user-specific views.
7. Add unit/integration tests, documentation, and build verification.

After each phase, compile and run relevant tests. Do not overwrite existing work. Ask only if a genuinely blocking decision is missing.

## Tests and acceptance criteria

Test registration, duplicate email, login failures, token protection, prohibition of public admin registration, employer approval requirement, job approval workflow, invisibility of pending/unapproved jobs, all-students-same-feed behavior, filters, pagination, deterministic sorting, past/full vacancies, successful booking, duplicate booking, capacity exhaustion, concurrent booking safety, role checks, ownership restrictions, and invalid inputs.

Prefer isolated Testcontainers MySQL integration tests. Never run destructive tests against a development database. If Docker or MySQL is unavailable, report unverified tests honestly.

## Final deliverables

Make real changes in the existing repository. Provide a runnable Java 21 Spring Boot backend, dependency configuration, migrations, `.env.example`, tests, README setup/run commands, sample API requests, and Markdown architecture/API/algorithm documentation. At the end report the files changed, build/test commands and actual results, assumptions, incomplete features, and the next command to run.

**Start by inspecting the current repository and reporting what exists. Then implement incrementally.**
