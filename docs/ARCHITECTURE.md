# NextShift / UniShift — System Architecture

## 1. Overview

NextShift is a campus part-time job platform designed to bridge university students with local campus employers and shop owners. The system is implemented as a production-grade, stateless REST API backend built on **Java 21** and **Spring Boot**, utilizing **Spring Security**, **Spring Data JPA**, **Flyway**, and **JJWT**.

---

## 2. Object-Oriented Programming (OOP) Principles

The backend architecture is structured around core object-oriented principles to guarantee maintainability, high cohesion, low coupling, and extensible domain behavior:

### 2.1 Encapsulation
- **Domain State Protection**: All JPA entities (`User`, `Job`, `Booking`) maintain strictly `private` fields. Mutators are guarded by business invariants.
- **Controlled Domain Transitions**: Rather than permitting arbitrary external state modifications, entities encapsulate their state transitions:
  - `Job.publish()` atomically transitions state to `PUBLISHED` and stamps `publishedAt = Instant.now()`.
  - `Job.reject()` and `Job.close()` control the lifecycle of vacancies.
  - `User.approve()` safely marks employers approved without exposing internal role assignment logic.
  - `Job.isEligibleForBooking(Instant now)` encapsulates the multi-variable rule ensuring a vacancy is published, future-dated, and associated with an active, approved employer.

### 2.2 Abstraction
- The recommendation engine is abstracted behind the `JobRecommendationStrategy` interface:
  ```java
  public interface JobRecommendationStrategy {
      Page<Job> getRecommendedJobs(Long studentId, String category, String location, Pageable pageable);
  }
  ```
- This completely abstracts how jobs are ranked, filtered, or selected from the service and controller layers.

### 2.3 Polymorphism
- `JobService` depends purely on the `JobRecommendationStrategy` abstraction rather than a concrete implementation class.
- The default V0 strategy (`AllPublishedJobsStrategy`) is injected via Spring's dependency injection container.
- Future algorithms (e.g., TF-IDF skill matching, geospatial proximity, collaborative filtering) can be introduced by creating new strategy implementations without altering any service or controller code.

### 2.4 Inheritance
- Common persistent state and lifecycle behavior are factored into an auditable base class:
  ```java
  @MappedSuperclass
  public abstract class BaseEntity { ... }
  ```
- `BaseEntity` encapsulates entity identity (`id`), audit timestamps (`createdAt`, `updatedAt`), pre-persist / pre-update hooks, and identity-safe `equals` and `hashCode` implementations.
- Subclasses (`User`, `Job`, `Booking`) inherit these persistent capabilities cleanly without requiring unnatural relational table inheritance hierarchies.

---

## 3. High-Level Architecture

```
[ HTTP Client / Web Client ]
             │
             ▼
[ JwtAuthenticationFilter ] (Stateless Bearer JWT Validation)
             │
             ▼
[ SecurityFilterChain ] (Role-based access: ADMIN, EMPLOYER, STUDENT)
             │
             ▼
[ REST Controllers ] (Thin web layer: Auth, Job, Booking, Employer, Admin)
             │
             ▼
[ Application Services ] (Transaction boundaries & Business logic)
       │              │
       ▼              ▼
[ Strategy Engine ]   [ Spring Data Repositories ]
       │                      │
       └──────────────┬───────┘
                      ▼
             [ JPA / Hibernate ]
                      │
                      ▼
           [ MySQL / H2 Database ]
```

---

## 4. Package Structure

```text
com.unishift.unishift_backend/
├── admin/
│   ├── AdminController.java
│   └── AdminService.java
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   └── dto/
│       ├── AuthResponse.java
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── UserResponse.java
├── booking/
│   ├── Booking.java
│   ├── BookingController.java
│   ├── BookingRepository.java
│   ├── BookingService.java
│   ├── BookingStatus.java
│   └── dto/
│       └── BookingResponse.java
├── common/
│   ├── ApiExceptionHandler.java
│   ├── BaseEntity.java
│   ├── HealthController.java
│   └── exception/
│       ├── ApiException.java
│       ├── BadRequestException.java
│       ├── ConflictException.java
│       ├── ErrorResponse.java
│       ├── ForbiddenException.java
│       ├── ResourceNotFoundException.java
│       └── UnauthorizedException.java
├── config/
│   ├── AdminBootstrapConfig.java
│   ├── DotenvEnvironmentPostProcessor.java
│   └── SecurityConfig.java
├── employer/
│   └── EmployerController.java
├── job/
│   ├── Job.java
│   ├── JobController.java
│   ├── JobRepository.java
│   ├── JobService.java
│   ├── JobStatus.java
│   └── dto/
│       ├── CreateJobRequest.java
│       └── JobResponse.java
├── recommendation/
│   ├── AllPublishedJobsStrategy.java
│   └── JobRecommendationStrategy.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── UserPrincipal.java
└── user/
    ├── User.java
    ├── UserRepository.java
    └── UserRole.java
```

---

## 5. Security & Authentication Model

- **Password Hashing**: BCrypt (`PasswordEncoder`) with strong default cost factors.
- **JWT Authentication**: HMAC-SHA256 tokens carrying claims for subject email, user ID, role, and approval status.
- **Role Isolation**:
  - `STUDENT`: Public job feed discovery, individual vacancy booking, personal booking history. Cannot create jobs or approve accounts.
  - `EMPLOYER`: Vacancy creation (only when approved by admin), viewing own vacancies and candidate bookings. Cannot book jobs or approve vacancies.
  - `ADMIN`: Employer verification and vacancy approval. Cannot be registered via public endpoints.
- **Admin Bootstrapping**: Managed securely at startup via environment configuration (`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`).

---

## 6. Concurrency & Booking Integrity

To prevent race conditions and overbooking under high concurrent load:
1. **Pessimistic Locking**: `JobRepository.findByIdWithLock(Long id)` issues a `SELECT ... FOR UPDATE` (`LockModeType.PESSIMISTIC_WRITE`) on the target `Job` record.
2. **Atomic Verification**: Within the same database transaction (`@Transactional`), the active confirmed booking count is evaluated against `job.getCapacity()`.
3. **Capacity Safeguards**: If the confirmed count meets or exceeds capacity, a `409 Conflict` is returned immediately.
4. **Unique Database Constraints**: A database-level unique constraint `uk_booking_job_student` on `(job_id, student_id)` ensures duplicate bookings are rejected at persistence level even across distributed nodes.
