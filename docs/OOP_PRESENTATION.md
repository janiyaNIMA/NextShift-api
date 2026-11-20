---
marp: true
theme: default
paginate: true
header: 'UniShift — Object-Oriented Programming Architecture'
footer: '© 2026 UniShift Team | Technical Presentation'
style: |
  section {
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    font-size: 24px;
    padding: 40px;
  }
  h1 {
    color: #1e3a8a;
  }
  h2 {
    color: #2563eb;
    border-bottom: 2px solid #e2e8f0;
    padding-bottom: 8px;
  }
  pre {
    background-color: #f1f5f9;
    border-radius: 8px;
    font-size: 17px;
  }
  footer {
    font-size: 12px;
    color: #64748b;
  }
  header {
    font-size: 13px;
    color: #3b82f6;
  }
  .highlight {
    background-color: #eff6ff;
    border-left: 4px solid #3b82f6;
    padding: 10px 16px;
    margin: 10px 0;
  }
---

# UniShift / NextShift Backend
## Object-Oriented Programming (OOP) Architecture
### Classes, Subclasses, Interfaces, Polymorphism, Encapsulation, Abstraction & Inheritance

**Presenter:** Engineering Team  
**Tech Stack:** Java 21 | Spring Boot 3.3.5 | Spring Security | Spring Data JPA  

---

## 1. System Overview & Problem Domain

- **UniShift (NextShift)**: Production-grade campus part-time job marketplace.
- **Key Stakeholders**:
  - **Students**: Discover verified shifts, book vacancies, view booking history.
  - **Employers**: Post job shifts, manage worker capacity, review applicants.
  - **Administrators**: Verify and approve employers and posted vacancies.
- **Backend Architecture**:
  - RESTful API with stateless JWT security.
  - Layered Architecture: Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ Database.
  - Built with clean OOP principles and domain-driven design.

<!--
Presenter Notes (Sinhala / English):
"අද අපි ඉදිරිපත් කරන්නේ UniShift backend එකේ Object-Oriented Architecture එක. මේ project එකේ Java 21 සහ Spring Boot භාවිතා කරලා classes, inheritance, encapsulation, abstraction, interfaces සහ polymorphism ප්‍රායෝගිකව ක්‍රියාත්මක කර ඇති ආකාරය අපි පියවරෙන් පියවර විස්තර කරනවා."
-->

---

## 2. The Core OOP Pillars in UniShift

| OOP Concept | Implementation in UniShift | Key Class Reference |
|---|---|---|
| **Classes** | Blueprints for Entities, Services, Controllers, DTOs | `User`, `Job`, `Booking`, `JobService` |
| **Subclasses** | Derived classes inheriting shared state & behavior | `Job extends BaseEntity`, `ConflictException` |
| **Interfaces** | Contracts decoupling callers from implementations | `JobRecommendationStrategy`, Repositories |
| **Polymorphism** | Strategy Pattern, Method Overriding & Overloading | Strategy injection, `UserPrincipal` as `UserDetails` |
| **Encapsulation** | `private` data hiding, Rich domain methods, Records | `job.publish()`, `job.isEligibleForBooking()` |
| **Abstraction** | Hiding implementation complexity behind contracts | `BaseEntity`, `JobRecommendationStrategy` |
| **Inheritance** | MappedSuperclass reuse & Custom Exception tree | `BaseEntity` $\rightarrow$ Entities; `ApiException` tree |

---

## 3. High-Level Domain Model Diagram

```
       ┌────────────────────────────────────────────────────────┐
       │                <<abstract, MappedSuperclass>>          │
       │                         BaseEntity                     │
       │────────────────────────────────────────────────────────│
       │ - Long id                                              │
       │ - Instant createdAt                                    │
       │ - Instant updatedAt                                    │
       └───────────────────────────┬────────────────────────────┘
                                   │ extends (Inheritance)
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  ┌─────────────┐           ┌─────────────┐           ┌─────────────┐
  │    User     │ 1       * │     Job     │ 1       * │   Booking   │
  │─────────────│◄──────────│─────────────│◄──────────│─────────────│
  │ - name      │ employer  │ - title     │ job       │ - status    │
  │ - email     │           │ - payAmount │           │ - createdAt │
  │ - role      │           │ - capacity  │           └─────────────┘
  │ - approved  │           │ - status    │                  ▲
  └─────────────┘           └─────────────┘                  │ * student
         ▲                                                   │
         └───────────────────────────────────────────────────┘
```

---

## 4. Classes (පන්ති)

Classes serve as blueprints defining state and behavior across specific architectural tiers:

1. **Domain Entities (`@Entity`)**:
   - `User`: Manages user credentials, roles (`STUDENT`, `EMPLOYER`, `ADMIN`), and approval states.
   - `Job`: Encapsulates vacancy details, shift schedules, wage rates, and capacity limits.
   - `Booking`: Encapsulates confirmed reservation connections between Students and Jobs.
2. **Business Services (`@Service`)**:
   - `JobService`, `BookingService`, `AuthService`, `AdminService`: Orchestrate business transactions.
3. **Web Controllers (`@RestController`)**:
   - `JobController`, `BookingController`, `AuthController`, `AdminController`: Handle HTTP requests.
4. **Data Transfer Objects (`record`)**:
   - `CreateJobRequest`, `JobResponse`, `AuthResponse`: Lightweight immutable data packets.

<!--
Presenter Notes:
"Class එකක් කියන්නේ object එකක blueprint එක. අපේ project එකේ Single Responsibility Principle (SRP) අනුව classes කොටස් කිහිපයකට බෙදලා තියෙනවා: Entities, Services, Controllers, DTOs සහ Configuration."
-->

---

## 5. Subclasses & Class Hierarchies (අනුපන්ති)

Subclasses specialize and extend parent behavior using the `extends` keyword.

### 1. Persistent Entity Hierarchy:
```java
// Subclass 1: Extends BaseEntity
public class User extends BaseEntity { ... }

// Subclass 2: Extends BaseEntity
public class Job extends BaseEntity { ... }

// Subclass 3: Extends BaseEntity
public class Booking extends BaseEntity { ... }
```
- Subclasses inherit `id`, `createdAt`, `updatedAt`, and lifecycle hooks `@PrePersist` and `@PreUpdate`.

### 2. Exception Subclass Hierarchy:
`ApiException extends RuntimeException`  
$\rightarrow$ Subclasses: `ConflictException`, `ResourceNotFoundException`, `BadRequestException`, `ForbiddenException`, `UnauthorizedException`.

---

## 6. Interfaces (අතුරුමුහුණත්)

Interfaces declare pure behavioral contracts, decoupling high-level services from low-level implementations.

### 1. Strategy Interface (`JobRecommendationStrategy`):
```java
public interface JobRecommendationStrategy {
    Page<Job> getRecommendedJobs(
        Long studentId, String category, String location, Pageable pageable
    );
}
```
- Decouples job feeds from database queries and sorting mechanisms.
- Allows plugging in machine learning or geolocation strategies without touching service code.

### 2. Persistence Interfaces (`JpaRepository`):
- `UserRepository`, `JobRepository`, `BookingRepository` extend Spring Data's `JpaRepository<T, ID>`, providing declarative CRUD and query abstraction.

---

## 7. Polymorphism (බහුරූපතාවය) — Strategy Pattern

UniShift utilizes **Subtype Polymorphism** via the **Strategy Pattern**:

```java
@Service
public class JobService {
    // 1. Depends on the INTERFACE, not the concrete implementation
    private final JobRecommendationStrategy recommendationStrategy;

    public JobService(JobRecommendationStrategy recommendationStrategy) {
        this.recommendationStrategy = recommendationStrategy; // Injected polymorphically
    }

    public Page<JobResponse> getPublishedJobs(...) {
        // 2. Polymorphic method call resolved dynamically at runtime
        Page<Job> jobPage = recommendationStrategy.getRecommendedJobs(...);
        return jobPage.map(JobResponse::fromEntity);
    }
}
```
- Concrete class: `AllPublishedJobsStrategy implements JobRecommendationStrategy`.
- Open/Closed Principle: A new `AIRecommendationStrategy` can be swapped in with **zero** changes to `JobService`.

---

## 8. Polymorphism: Method Overriding & Overloading

### Method Overriding (`@Override` — Dynamic Polymorphism):
Subclasses and implementations provide specialized implementations:
```java
// BaseEntity.java: Overriding Object.equals() for JPA identity
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BaseEntity that = (BaseEntity) o;
    return id != null && Objects.equals(id, that.id);
}
```

### Method Overloading (Static / Compile-Time Polymorphism):
In `JwtService.java`, multiple methods share a name with different signatures:
```java
// Overload 1
public String generateToken(UserDetails userDetails) { ... }

// Overload 2: Overloaded with additional custom claims map
public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) { ... }
```

---

## 9. Encapsulation (කැප්සියුලීකරණය) — Data Hiding

Encapsulation bundles data and methods while restricting direct external access:

1. **Strict Data Hiding**:
   - All entity fields are declared `private`.
   - Outside classes cannot mutate fields directly.
2. **Controlled Domain Methods (Rich Domain Model)**:
   - State mutations are governed by encapsulated business methods:

```java
// Job.java: Encapsulating state transition and business rules
public void publish() {
    this.status = JobStatus.PUBLISHED;
    this.publishedAt = Instant.now(); // Guaranteed atomic update
}

public boolean isEligibleForBooking(Instant now) {
    return this.status == JobStatus.PUBLISHED
            && this.employer != null
            && this.employer.isActive()
            && this.employer.isApproved()
            && this.startsAt.isAfter(now);
}
```

---

## 10. Encapsulation: Immutability with Java 21 Records

All Request/Response DTOs are declared as Java 21 `record` types:

```java
public record CreateJobRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String category,
    @NotBlank String location,
    @NotNull @DecimalMin("0.01") BigDecimal payAmount,
    @NotBlank String payUnit,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @NotNull @Min(1) Integer capacity
) {}
```

- **Built-in Encapsulation**:
  - All components are `private` and `final`.
  - No mutator methods (setters) exist $\rightarrow$ **Immutable state**.
  - Prevents accidental data alteration across controller, service, and security layers.

---

## 11. Abstraction (වියුක්තකරණය)

Abstraction hides low-level complexity and exposes only essential operations:

1. **Abstract Superclass (`BaseEntity`)**:
   - Declared as `public abstract class BaseEntity`.
   - Abstracts entity primary key generation and audit timestamps (`createdAt`, `updatedAt`).
2. **Repository Abstraction**:
   - Spring Data JPA abstracts database connectivity, JDBC connections, and SQL generation.
3. **Layered Architecture Abstraction**:
   - Controllers abstract HTTP protocol and serialization.
   - Services abstract domain business transactions (`@Transactional`).
   - Repositories abstract persistence operations.

<!--
Presenter Notes:
"Abstraction එකෙන් කරන්නේ අභ්‍යන්තරයේ සිදුවන සංකීර්ණ ක්‍රියාදාමයන් සඟවලා, අවශ්‍ය මූලික අතුරුමුහුණත පමණක් ලබාදීමයි. උදාහරණයක් ලෙස BaseEntity එකෙන් ID සහ timestamps හැසිරවීම සඟවා තියෙනවා."
-->

---

## 12. Inheritance (උරුමය) & Architectural Decisions

### 1. `@MappedSuperclass` Inheritance:
- `BaseEntity` provides `id`, `createdAt`, `updatedAt`, `@PrePersist`, and `@PreUpdate`.
- Derived by `User`, `Job`, and `Booking`.
- **DRY Principle**: Prevents redundant code across database entities.

### 2. Architectural Choice: "Favor Composition Over Inheritance"
> *Why not `Student extends User` and `Employer extends User` in the database?*
- Relational JPA table inheritance (`SINGLE_TABLE` or `JOINED`) causes nullable columns or expensive table joins.
- **Our Solution**: Shared technical infrastructure uses **Inheritance** (`BaseEntity`), while business roles use **Composition** (`UserRole` enum + helper methods).

---

## 13. SOLID Principles in Action

- **S - Single Responsibility Principle**:
  - `JobController` (HTTP only) $\neq$ `JobService` (Business logic) $\neq$ `JobRepository` (Persistence).
- **O - Open/Closed Principle**:
  - `JobRecommendationStrategy` allows adding new ranking algorithms without modifying `JobService`.
- **L - Liskov Substitution Principle**:
  - `AllPublishedJobsStrategy` can replace `JobRecommendationStrategy` seamlessly without altering client behavior.
- **I - Interface Segregation Principle**:
  - Interfaces are lean and focused (`JobRecommendationStrategy` defines only one focused method).
- **D - Dependency Inversion Principle**:
  - High-level `JobService` depends on abstraction `JobRecommendationStrategy`, not the concrete class.

---

## 14. Key Viva / Evaluation Questions & Quick Answers

- **Q1: Where did you use the Strategy Pattern?**
  - **A**: In the job recommendation engine (`JobRecommendationStrategy` interface and `AllPublishedJobsStrategy` implementation injected into `JobService`).
- **Q2: Why use `BaseEntity`?**
  - **A**: Demonstrates Inheritance and Abstraction. Provides centralized ID generation, audit timestamps (`createdAt`, `updatedAt`), and JPA-safe `equals`/`hashCode`.
- **Q3: How do you prevent anemic domain models?**
  - **A**: Through Encapsulation. Entities contain business methods like `job.publish()`, `job.isEligibleForBooking()`, and `user.approve()`, rather than plain setters.
- **Q4: Why not create `Student` and `Employer` as subclasses of `User` in JPA?**
  - **A**: To avoid relational database overhead (joins or sparse null columns). We adhere to "Favor composition over inheritance for entity roles".

---

## 15. Summary & Conclusion

- **Solid OOP Foundation**: UniShift is built on industry-standard Object-Oriented best practices.
- **Maintainable & Extensible**:
  - Strategy Pattern enables plug-and-play recommendation algorithms.
  - Layered architecture isolates changes and prevents ripple effects.
- **Secure & Robust**:
  - Strict encapsulation protects domain invariants.
  - Concurrency safeguards (pessimistic locking) protect against overbooking.

### Thank You!
**Questions & Answers (Q&A)**
