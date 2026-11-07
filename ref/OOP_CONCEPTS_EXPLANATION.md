# UniShift / NextShift — Object-Oriented Programming (OOP) Concepts Guide
# වස්තු-පාදක ක්‍රමලේඛන (OOP) මූලධර්ම භාවිතය පිළිබඳ පුළුල් විග්‍රහය

මෙම ලේඛනය මඟින් UniShift (NextShift) backend ව්‍යාපෘතිය තුළ Object-Oriented Programming (OOP) සංකල්ප ප්‍රායෝගිකව ක්‍රියාත්මක කර ඇති ආකාරය සිංහල සහ ඉංග්‍රීසි (Sinhala & English) මාධ්‍ය ද්විත්වයෙන්ම විස්තර කෙරේ. මෙමගින් විශ්වවිද්‍යාල ව්‍යාපෘති ඇගයීම් (Viva / Project Evaluation) සඳහා අවශ්‍ය සියලුම තාක්ෂණික පැහැදිලි කිරීම් සහ කේත නිදසුන් (Code Examples) සපයයි.

---

## Overview Table / සාරාංශ වගුව

| OOP Concept (සංකල්පය) | Project Implementation (ව්‍යාපෘතියේ භාවිතය) | Key Classes / Interfaces (ප්‍රධාන පන්ති/අතුරුමුහුණත්) |
|---|---|---|
| **1. Encapsulation (කැප්සියුලීකරණය)** | Private fields, Getters/Setters, Controlled Domain Methods, Data Hiding | `User.java`, `Job.java`, `Booking.java`, DTO Records |
| **2. Abstraction (වියුක්තකරණය)** | Interface Definition, Abstract Superclass, Hiding internal mechanics | `JobRecommendationStrategy.java`, `BaseEntity.java` |
| **3. Polymorphism (බහුරූපතාවය)** | Strategy Pattern, Interface reference dependency, Spring Security UserDetails | `JobRecommendationStrategy`, `AllPublishedJobsStrategy`, `UserPrincipal` |
| **4. Inheritance (උරුමය)** | Auditable MappedSuperclass, Code reuse for common lifecycle & identity | `BaseEntity` → `User`, `Job`, `Booking` |

---

## 1. Encapsulation (කැප්සියුලීකරණය)

### English:
**Encapsulation** is the bundling of data (attributes) and the methods that operate on that data into a single unit (class), while restricting direct access to internal state from the outside world.
In this project:
1. **Data Hiding**: All entity attributes (`name`, `email`, `passwordHash`, `status`, `capacity`, etc.) are declared strictly as `private`.
2. **Controlled State Transitions**: Instead of allowing external services or controllers to mutate entity status directly with arbitrary setters (e.g., `job.setStatus(JobStatus.PUBLISHED)`), the entity encapsulates the business rules and exposes meaningful domain methods.
3. **Immutability with Records**: DTOs (`RegisterRequest`, `JobResponse`, etc.) are implemented using Java 21 `record` types, guaranteeing shallow immutability and preventing accidental external state modification.

### සිංහල:
**කැප්සියුලීකරණය (Encapsulation)** යනු පන්තියක දත්ත (variables/fields) සහ එම දත්ත මත ක්‍රියාත්මක වන විධික්‍රම (methods) එකම ඒකකයක් තුළ එකතු කර, බාහිරින් සෘජුවම එම දත්ත වලට සිදු කරන අනවසර ප්‍රවේශයන් සීමා කිරීමයි.
අපගේ ව්‍යාපෘතිය තුළ:
1. **දත්ත සැඟවීම (Data Hiding)**: Entity පන්තිවල සියලුම ගුණාංග `private` ලෙස නම් කර ඇති අතර, ඒවා වෙත පිවිසිය හැක්කේ getters/setters සහ ආරක්ෂිත domain methods මඟින් පමණි.
2. **පාලනය කළ තත්ත්ව වෙනස්වීම් (Controlled Domain Methods)**: බාහිරින් සෘජුව `job.setStatus(...)` ලෙස වෙනස් කිරීමට ඉඩ නොදී, `publish()`, `reject()`, `approve()` වැනි ව්‍යාපාරික නීති (business invariants) entity එක තුළම ක්‍රියාත්මක කර ඇත.
3. **Java Records මඟින් දත්ත සුරැකීම**: API වෙත ලැබෙන සහ පිටවන දත්ත DTO Records ලෙස සකසා ඇති නිසා දත්තවල අගයන් අහඹු ලෙස වෙනස් විය නොහැක (Immutability).

### Code Demonstration / කේත නිදසුන:

#### Entity Domain Methods Encapsulation (`Job.java`):
```java
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    // Private fields - Hidden from direct external modification
    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "published_at")
    private Instant publishedAt;

    // Controlled Business Method: Guarantees status and publishedAt are updated together atomically
    public void publish() {
        this.status = JobStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void reject() {
        this.status = JobStatus.REJECTED;
    }

    // Encapsulated Business Rule: multi-variable eligibility check
    public boolean isEligibleForBooking(Instant now) {
        return this.status == JobStatus.PUBLISHED
                && this.employer != null
                && this.employer.isActive()
                && this.employer.isApproved()
                && this.startsAt.isAfter(now);
    }
}
```

#### User Domain Methods (`User.java`):
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private boolean approved = false;

    // Encapsulated state transition
    public void approve() {
        this.approved = true;
    }

    public boolean canCreateJobs() {
        return this.role == UserRole.EMPLOYER && this.active && this.approved;
    }
}
```

---

## 2. Abstraction (වියුක්තකරණය)

### English:
**Abstraction** involves hiding complex background implementation details and exposing only the essential interface to the consumer. This reduces system complexity and isolates modules from changes in low-level implementations.
In this project:
1. **Recommendation Strategy Abstraction**: The `JobRecommendationStrategy` interface defines *what* recommendation contracts are available without revealing *how* jobs are queried, joined, or filtered.
2. **Repository Abstraction**: Spring Data JPA repositories (`UserRepository`, `JobRepository`, `BookingRepository`) abstract away database interactions, SQL generation, connection pooling, and transaction orchestration.
3. **Base Entity Abstraction**: `BaseEntity` is an abstract mapped superclass that abstracts away entity identity and audit timestamp lifecycle hooks.

### සිංහල:
**වියුක්තකරණය (Abstraction)** යනු පද්ධතියක අභ්‍යන්තරයේ සිදුවන සංකීර්ණ තාක්ෂණික ක්‍රියාදාමයන් බාහිරින් සඟවා, පරිශීලකයාට හෝ සෙසු පන්තිවලට අවශ්‍ය වන මූලික අතුරුමුහුණත (Interface/Contract) පමණක් නිරාවරණය කිරීමයි.
අපගේ ව්‍යාපෘතිය තුළ:
1. **රැකියා නිර්දේශ කිරීමේ අතුරුමුහුණත**: `JobRecommendationStrategy` මඟින් නිර්දේශිත රැකියා ලබාගන්නා ආකාරයේ අභ්‍යන්තර සංකීර්ණ Database Query ක්‍රියාදාමයන් `JobService` එකෙන් සම්පූර්ණයෙන්ම සඟවා ඇත.
2. **දත්ත ගබඩා වියුක්තකරණය (Repository Abstraction)**: `JpaRepository` භාවිතයෙන් සෘජු SQL ලිවීම වෙනුවට සරල ක්‍රමලේඛ අතුරුමුහුණතක් හරහා CRUD ක්‍රියාකාරකම් හසුරුවයි.
3. **පොදු Entity වියුක්තකරණය**: `BaseEntity` abstract class එක මඟින් Entity ID සහ Audit timestamps කළමනාකරණය වියුක්ත කර ඇත.

### Code Demonstration / කේත නිදසුන:

#### The Strategy Interface (`JobRecommendationStrategy.java`):
```java
package com.unishift.unishift_backend.recommendation;

import com.unishift.unishift_backend.job.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobRecommendationStrategy {

    Page<Job> getRecommendedJobs(Long studentId, String category, String location, Pageable pageable);
}
```

#### Abstract Entity Superclass (`BaseEntity.java`):
```java
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }
}
```

---

## 3. Polymorphism (බහුරූපතාවය)

### English:
**Polymorphism** allows an entity (such as a variable, function, or object) to take on multiple forms. In OOP, it commonly manifests as *subtype polymorphism*, where a client depends on a supertype or interface, and the concrete behavior is provided dynamically at runtime.
In this project:
1. **Strategy Pattern (Interface Polymorphism)**:
   - `JobService` holds a reference to the `JobRecommendationStrategy` interface, **not** to the concrete `AllPublishedJobsStrategy`.
   - At runtime, Spring injects the active strategy implementation.
   - If tomorrow a `MachineLearningRecommendationStrategy` or `ProximityBasedRecommendationStrategy` is written, it can replace `AllPublishedJobsStrategy` with zero changes to `JobService` or `JobController` (fulfilling the Open/Closed Principle).
2. **Spring Security Polymorphism**:
   - `UserPrincipal` implements Spring Security's `UserDetails` interface.
   - The security framework treats `UserPrincipal` polymorphically as a generic `UserDetails` token principal across authentication filters and security context holders.

### සිංහල:
**බහුරූපතාවය (Polymorphism)** යනු එකම අතුරුමුහුණතක් (Interface) හෝ මව් පන්තියක් (Superclass) හරහා විවිධ ආකාරයේ හැසිරීම් (Multiple forms / behaviors) ප්‍රදර්ශනය කිරීමේ හැකියාවයි.
අපගේ ව්‍යාපෘතිය තුළ:
1. **Strategy Pattern මඟින් බහුරූපතාවය භාවිතය**:
   - `JobService` පන්තිය සෘජුවම `AllPublishedJobsStrategy` concrete class එක මත නොරැඳී, `JobRecommendationStrategy` interface එක මත පමණක් රඳා පවතී.
   - Runtime එකේදී Spring Framework මඟින් සුදුසු ක්‍රියාකාරී පන්තිය inject කරනු ලබයි.
   - අනාගතයේදී වෙනත් recommendations ඇල්ගොරිතමයක් එක් කළ විට `JobService` හි කිසිදු පේළියක් වෙනස් කිරීමට අවශ්‍ය නොවේ.
2. **Spring Security UserDetails බහුරූපතාවය**:
   - අපගේ `UserPrincipal` පන්තිය Spring Security හි සම්මත `UserDetails` interface එක implement කරයි. එමඟින් Security framework එකට අපගේ User වස්තුව generic UserDetails එකක් ලෙස හැසිරවීමට ඉඩ සලසයි.

### Code Demonstration / කේත නිදසුන:

#### Polymorphic Dependency in `JobService.java`:
```java
@Service
public class JobService {

    // Dependent on ABSTRACTION, not concrete implementation (Polymorphism)
    private final JobRecommendationStrategy recommendationStrategy;

    public JobService(
        JobRepository jobRepository,
        UserRepository userRepository,
        BookingRepository bookingRepository,
        JobRecommendationStrategy recommendationStrategy // Injected polymorphically
    ) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.recommendationStrategy = recommendationStrategy;
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getPublishedJobs(String studentEmail, String category, String location, Pageable pageable) {
        Long studentId = ...;

        // Polymorphic method call - resolves at runtime to AllPublishedJobsStrategy
        Page<Job> jobPage = recommendationStrategy.getRecommendedJobs(studentId, category, location, pageable);
        
        return jobPage.map(...);
    }
}
```

#### Concrete Implementation (`AllPublishedJobsStrategy.java`):
```java
@Component
@Primary
public class AllPublishedJobsStrategy implements JobRecommendationStrategy {

    private final JobRepository jobRepository;

    public AllPublishedJobsStrategy(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public Page<Job> getRecommendedJobs(Long studentId, String category, String location, Pageable pageable) {
        return jobRepository.findEligibleJobs(...);
    }
}
```

---

## 4. Inheritance (උරුමය)

### English:
**Inheritance** is a mechanism where a new class derives properties and behaviors from an existing class, promoting code reuse, clean hierarchy, and standardized lifecycle management.
In this project:
1. **Shared Auditable Superclass**:
   - `BaseEntity` is declared as `@MappedSuperclass`.
   - `User`, `Job`, and `Booking` inherit `id`, `createdAt`, `updatedAt`, lifecycle annotations (`@PrePersist`, `@PreUpdate`), and robust `equals`/`hashCode` implementations.
   - This eliminates redundant field declarations across entities and guarantees consistent timestamp auditing across the entire domain.
2. **Exception Hierarchy Inheritance**:
   - Custom exceptions (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `ForbiddenException`, `UnauthorizedException`) inherit from a custom `ApiException`, which in turn inherits from `RuntimeException`.
   - This allows `ApiExceptionHandler` to catch and handle common API exceptions uniformly while preserving specific HTTP status codes.
3. **Why Discriminator Inheritance was Avoided for Roles**:
   - In relational database design and JPA, creating a deep table inheritance hierarchy (`Student extends User`, `Employer extends User`) creates complicated JOIN or SINGLE_TABLE mapping overhead with nullable columns.
   - Instead, justifiable inheritance was used for common infrastructure (`BaseEntity`), while user specialization was achieved via clean composition (`role` attribute and domain helper methods), which is an acknowledged OOP best practice ("Favor composition over complex inheritance where appropriate").

### සිංහල:
**උරුමය (Inheritance)** යනු එක් මව් පන්තියක (Superclass) පවතින ගුණාංග (attributes) සහ ක්‍රම (methods) තවත් අනු පන්තියක් (Subclass) වෙත උරුම කර දීමයි. එමඟින් කේත නැවත නැවත ලිවීම වැළකී (Code Reusability) පද්ධතිය පිරිසිදු වේ.
අපගේ ව්‍යාපෘතිය තුළ:
1. **`BaseEntity` මව් පන්තිය උරුම වීම**:
   - `BaseEntity` නමැති `@MappedSuperclass` එක මඟින් `id`, `createdAt`, `updatedAt` සහ lifecycle hooks සියලුම ප්‍රධාන entities (`User`, `Job`, `Booking`) වෙත උරුම කර දී ඇත.
   - මෙමගින් සෑම class එකක් තුළම නැවත නැවත timestamps සහ ID fields ලිවීම වැළකේ.
2. **Exception පන්ති අතර උරුමය**:
   - අපගේ `BadRequestException`, `ConflictException`, `ResourceNotFoundException` ආදී සියලුම exceptions `ApiException` මව් පන්තියෙන් උරුම වන අතර, එය `RuntimeException` වෙතින් උරුම වේ.
3. **ප්‍රවේශමෙන් උරුමය භාවිතා කිරීම (Design Justification)**:
   - දත්ත සමුදාය (Database) අනවශ්‍ය ලෙස සංකීර්ණ වීම වැළැක්වීම සඳහා `Student extends User` ලෙස වෙනම JPA table inheritance එකක් නොසාදා, `BaseEntity` වැනි සාධාරණීකරණය කළ හැකි තාක්ෂණික මට්ටමේදී පමණක් Inheritance භාවිතා කර ඇත.

### Code Demonstration / කේත නිදසුන:

#### Entity Hierarchy (`BaseEntity` → Subclasses):
```
       ┌───────────────────────┐
       │   <<MappedSuperclass>> │
       │       BaseEntity      │
       │───────────────────────│
       │ - Long id             │
       │ - Instant createdAt   │
       │ - Instant updatedAt   │
       └───────────┬───────────┘
                   │ inherits (extends)
         ┌─────────┼─────────┐
         ▼         ▼         ▼
    ┌────────┐ ┌───────┐ ┌─────────┐
    │  User  │ │  Job  │ │ Booking │
    └────────┘ └───────┘ └─────────┘
```

```java
// Subclass 1: Inherits id, createdAt, updatedAt
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    private String name;
    private String email;
    // ...
}

// Subclass 2: Inherits id, createdAt, updatedAt
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {
    private String title;
    private BigDecimal payAmount;
    // ...
}

// Subclass 3: Inherits id, createdAt, updatedAt
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {
    // ...
}
```

#### Exception Hierarchy:
```java
// Base custom API exception
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}

// Inherited specialized exceptions
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
```

---

## 5. SOLID Principles in UniShift / SOLID මූලධර්ම භාවිතය

| Principle | Description in UniShift / අපගේ ව්‍යාපෘතියේ භාවිතය |
|---|---|
| **S - Single Responsibility Principle (SRP)** | සෑම class එකකටම ඇත්තේ එක් පැහැදිලි වගකීමක් පමණි. Controllers හසුරුවන්නේ HTTP requests පමණි; Business logic ඇත්තේ Services තුළය; Data persistence ඇත්තේ Repositories තුළය. |
| **O - Open/Closed Principle (OCP)** | පද්ධතිය නව විශේෂාංග සඳහා විවෘතය (Open for extension), නමුත් පවතින කේත වෙනස් කිරීමට අවශ්‍ය නැත (Closed for modification). උදා: `JobRecommendationStrategy` මඟින් නව recommendation algorithm එකක් ලියූ විට `JobService` වෙනස් කිරීමට අවශ්‍ය නොවේ. |
| **L - Liskov Substitution Principle (LSP)** | `JobRecommendationStrategy` implement කරන ඕනෑම පන්තියක් (උදා: `AllPublishedJobsStrategy`) `JobService` හි කිසිදු බිඳවැටීමකින් තොරව ආදේශ කළ හැක. එසේම `UserPrincipal` සම්පූර්ණයෙන්ම `UserDetails` වෙනුවට ආදේශ විය හැක. |
| **I - Interface Segregation Principle (ISP)** | අතුරුමුහුණත් කුඩා සහ අදාළ කාර්යයට පමණක් සීමා කර ඇත. සේවාදායකයාට අවශ්‍ය නොවන methods බලහත්කාරයෙන් පැටවීම සිදු කර නැත (`JobRecommendationStrategy` හි ඇත්තේ එක් නිශ්චිත method එකක් පමණි). |
| **D - Dependency Inversion Principle (DIP)** | ඉහළ මට්ටමේ modules (උදා: `JobService`) පහළ මට්ටමේ concrete classes මත රඳා නොපවතී. ඒවා රඳා පවතින්නේ Abstractions (`JobRecommendationStrategy` interface) මත පමණි. |

---

## 6. Viva / Interview Questions & Answers Cheatsheet (ප්‍රශ්න සහ පිළිතුරු)

### Q1: How did you implement the Strategy Pattern in this project?
> **Answer**: We abstracted the job feed recommendation logic into the `JobRecommendationStrategy` interface. The `JobService` depends only on this interface. The default concrete implementation `AllPublishedJobsStrategy` queries the database for all eligible, future-dated, capacity-available vacancies. If we want to add a machine learning or location-proximity ranking later, we simply create a new class implementing `JobRecommendationStrategy` without touching `JobService` or `JobController`.

### Q2: Why did you use `BaseEntity` instead of adding `id` and timestamps to every entity?
> **Answer**: To demonstrate **Inheritance** and **DRY (Don't Repeat Yourself)** principles. `BaseEntity` is an abstract `@MappedSuperclass` containing the primary key `id`, `createdAt`, and `updatedAt`, along with `@PrePersist` and `@PreUpdate` lifecycle callbacks and an identity-safe `equals`/`hashCode` implementation. `User`, `Job`, and `Booking` inherit these attributes, eliminating boilerplate and ensuring uniform auditability.

### Q3: How is Encapsulation demonstrated in your domain models?
> **Answer**: All entity fields are `private`. Instead of setting raw statuses from outside, we created controlled domain methods like `job.publish()`, which transitions the status to `PUBLISHED` and stamps `publishedAt` atomically. Similarly, multi-condition business logic like `job.isEligibleForBooking(now)` is encapsulated directly inside `Job`, keeping domain rules close to the data they evaluate.

### Q4: Why did you not create `Student` and `Employer` as subclasses extending `User` in the database?
> **Answer**: Because in relational JPA mappings, class inheritance hierarchies across roles often result in either redundant nullable columns (`SINGLE_TABLE`) or expensive join queries (`JOINED`). In professional software architecture, inheritance is justified for common infrastructural concerns (like `BaseEntity`), whereas business roles are better modeled using role attributes and composition, adhering to the principle: *"Favor composition over complex inheritance where justified"*.
