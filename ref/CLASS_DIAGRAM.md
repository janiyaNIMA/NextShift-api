# UniShift / NextShift — System Class Diagram & OOP Design

මෙම ලේඛනය මඟින් UniShift (NextShift) backend පද්ධතියේ සම්පූර්ණ Object-Oriented Architecture එක සහ Class Diagram එක විස්තර කෙරේ.

---

## 1. Domain Model Class Diagram (Entities & Relationships)

පද්ධතියේ ප්‍රධාන Entity පන්ති අතර ඇති Inheritance (උරුමය), Associations (සම්බන්ධතා), සහ Enums පහත පරිදි වේ:

```mermaid
classDiagram
    direction TB

    class BaseEntity {
        <<abstract>>
        -Long id
        -Instant createdAt
        -Instant updatedAt
        #onCreate() void
        #onUpdate() void
        +getId() Long
        +setId(Long id) void
        +getCreatedAt() Instant
        +setCreatedAt(Instant createdAt) void
        +getUpdatedAt() Instant
        +setUpdatedAt(Instant updatedAt) void
        +equals(Object o) boolean
        +hashCode() int
    }

    class User {
        -String name
        -String email
        -String passwordHash
        -UserRole role
        -boolean active
        -boolean approved
        +approve() void
        +isStudent() boolean
        +isEmployer() boolean
        +isAdmin() boolean
        +canCreateJobs() boolean
        +getName() String
        +setName(String name) void
        +getEmail() String
        +setEmail(String email) void
        +getPasswordHash() String
        +setPasswordHash(String passwordHash) void
        +getRole() UserRole
        +setRole(UserRole role) void
        +isActive() boolean
        +setActive(boolean active) void
        +isApproved() boolean
        +setApproved(boolean approved) void
    }

    class UserRole {
        <<enumeration>>
        STUDENT
        EMPLOYER
        ADMIN
    }

    class Job {
        -User employer
        -String title
        -String description
        -String category
        -String location
        -BigDecimal payAmount
        -String payUnit
        -Instant startsAt
        -Instant endsAt
        -int capacity
        -JobStatus status
        -Instant publishedAt
        +publish() void
        +reject() void
        +close() void
        +isEligibleForBooking(Instant now) boolean
        +getEmployer() User
        +setEmployer(User employer) void
        +getTitle() String
        +setTitle(String title) void
        +getDescription() String
        +setDescription(String description) void
        +getCategory() String
        +setCategory(String category) void
        +getLocation() String
        +setLocation(String location) void
        +getPayAmount() BigDecimal
        +setPayAmount(BigDecimal payAmount) void
        +getPayUnit() String
        +setPayUnit(String payUnit) void
        +getStartsAt() Instant
        +setStartsAt(Instant startsAt) void
        +getEndsAt() Instant
        +setEndsAt(Instant endsAt) void
        +getCapacity() int
        +setCapacity(int capacity) void
        +getStatus() JobStatus
        +setStatus(JobStatus status) void
        +getPublishedAt() Instant
        +setPublishedAt(Instant publishedAt) void
    }

    class JobStatus {
        <<enumeration>>
        PENDING
        PUBLISHED
        REJECTED
        CLOSED
    }

    class Booking {
        -Job job
        -User student
        -BookingStatus status
        +getJob() Job
        +setJob(Job job) void
        +getStudent() User
        +setStudent(User student) void
        +getStatus() BookingStatus
        +setStatus(BookingStatus status) void
    }

    class BookingStatus {
        <<enumeration>>
        CONFIRMED
        CANCELLED
    }

    %% Inheritance
    BaseEntity <|-- User : inherits
    BaseEntity <|-- Job : inherits
    BaseEntity <|-- Booking : inherits

    %% Associations
    User "1" <-- "0..*" Job : employer
    User "1" <-- "0..*" Booking : student
    Job "1" <-- "0..*" Booking : job

    User ..> UserRole : uses
    Job ..> JobStatus : uses
    Booking ..> BookingStatus : uses
```

---

## 2. Recommendation Engine & Service Architecture (Strategy Pattern)

OOP Abstraction සහ Polymorphism මූලධර්ම භාවිතයෙන් Recommendation Engine එක Strategy Pattern එක අනුව නිර්මාණය කර ඇත:

```mermaid
classDiagram
    direction TB

    class JobRecommendationStrategy {
        <<interface>>
        +getRecommendedJobs(Long studentId, String category, String location, Pageable pageable)* Page~Job~
    }

    class AllPublishedJobsStrategy {
        -JobRepository jobRepository
        +getRecommendedJobs(Long studentId, String category, String location, Pageable pageable) Page~Job~
    }

    class JobService {
        -JobRepository jobRepository
        -UserRepository userRepository
        -BookingRepository bookingRepository
        -JobRecommendationStrategy recommendationStrategy
        +createJob(String employerEmail, CreateJobRequest request) JobResponse
        +getPublishedJobs(String studentEmail, String category, String location, Pageable pageable) Page~JobResponse~
        +getJobById(Long id) JobResponse
        +getEmployerJobs(String employerEmail) List~JobResponse~
        +getJobOwnedByEmployer(Long jobId, String employerEmail) Job
    }

    class BookingService {
        -BookingRepository bookingRepository
        -JobRepository jobRepository
        -UserRepository userRepository
        -JobService jobService
        +createBooking(Long jobId, String studentEmail) BookingResponse
        +getStudentBookings(String studentEmail) List~BookingResponse~
        +getBookingsForJob(Long jobId, String employerEmail) List~BookingResponse~
    }

    class AuthService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtService jwtService
        -AuthenticationManager authenticationManager
        +register(RegisterRequest request) AuthResponse
        +login(LoginRequest request) AuthResponse
        +getCurrentUser(String email) UserResponse
    }

    class AdminService {
        -UserRepository userRepository
        -JobRepository jobRepository
        -BookingRepository bookingRepository
        +getPendingEmployers() List~UserResponse~
        +approveEmployer(Long id) UserResponse
        +getPendingJobs() List~JobResponse~
        +approveJob(Long id) JobResponse
    }

    %% Strategy Pattern Polymorphism
    JobRecommendationStrategy <|.. AllPublishedJobsStrategy : implements
    JobService --> JobRecommendationStrategy : depends on abstraction

    %% Service relationships
    BookingService --> JobService : validates ownership
```

---

## 3. Full System Architecture (Controllers, Services & Repositories)

පද්ධතියේ Controllers, Services, Repositories සහ Security Filters අතර සම්බන්ධතාවය:

```mermaid
classDiagram
    direction TB

    %% Controllers
    class AuthController {
        -AuthService authService
        +register(RegisterRequest request) ResponseEntity~AuthResponse~
        +login(LoginRequest request) ResponseEntity~AuthResponse~
        +getCurrentUser(UserDetails userDetails) ResponseEntity~UserResponse~
    }

    class JobController {
        -JobService jobService
        +getPublishedJobs(String category, String location, UserDetails userDetails, Pageable pageable) ResponseEntity~Page~JobResponse~~
        +getJobById(Long id) ResponseEntity~JobResponse~
        +createJob(UserDetails userDetails, CreateJobRequest request) ResponseEntity~JobResponse~
    }

    class BookingController {
        -BookingService bookingService
        +bookJob(Long id, UserDetails userDetails) ResponseEntity~BookingResponse~
        +getMyBookings(UserDetails userDetails) ResponseEntity~List~BookingResponse~~
    }

    class EmployerController {
        -JobService jobService
        -BookingService bookingService
        +getMyJobs(UserDetails userDetails) ResponseEntity~List~JobResponse~~
        +getJobBookings(Long id, UserDetails userDetails) ResponseEntity~List~BookingResponse~~
    }

    class AdminController {
        -AdminService adminService
        +getPendingEmployers() ResponseEntity~List~UserResponse~~
        +approveEmployer(Long id) ResponseEntity~UserResponse~
        +getPendingJobs() ResponseEntity~List~JobResponse~~
        +approveJob(Long id) ResponseEntity~JobResponse~
    }

    class HealthController {
        +health() ResponseEntity~Map~
    }

    %% Repositories
    class UserRepository {
        <<interface>>
        +findByEmail(String email) Optional~User~
        +existsByEmail(String email) boolean
        +findByRoleAndApprovedFalse(UserRole role) List~User~
    }

    class JobRepository {
        <<interface>>
        +findByIdWithLock(Long id) Optional~Job~
        +findByIdWithEmployer(Long id) Optional~Job~
        +findByEmployerIdOrderByCreatedAtDesc(Long employerId) List~Job~
        +findByStatusOrderByCreatedAtDesc(JobStatus status) List~Job~
        +findEligibleJobs(...) Page~Job~
    }

    class BookingRepository {
        <<interface>>
        +existsByJobIdAndStudentId(Long jobId, Long studentId) boolean
        +countByJobIdAndStatus(Long jobId, BookingStatus status) long
        +findByStudentIdOrderByCreatedAtDesc(Long studentId) List~Booking~
        +findByJobIdOrderByCreatedAtDesc(Long jobId) List~Booking~
        +findByJobIdAndStatusOrderByCreatedAtDesc(Long jobId, BookingStatus status) List~Booking~
    }

    %% Controller to Service
    AuthController --> AuthService
    JobController --> JobService
    BookingController --> BookingService
    EmployerController --> JobService
    EmployerController --> BookingService
    AdminController --> AdminService

    %% Service to Repository
    AuthService --> UserRepository
    JobService --> JobRepository
    JobService --> UserRepository
    JobService --> BookingRepository
    BookingService --> BookingRepository
    BookingService --> JobRepository
    BookingService --> UserRepository
    AdminService --> UserRepository
    AdminService --> JobRepository
    AdminService --> BookingRepository
```

---

## 4. Security & Authentication Architecture

```mermaid
classDiagram
    direction TB

    class JwtService {
        -SecretKey signingKey
        -long expirationMs
        +generateToken(UserDetails userDetails, Map extraClaims) String
        +generateToken(UserDetails userDetails) String
        +extractUsername(String token) String
        +extractClaim(String token, Function claimsResolver) T
        +isTokenValid(String token, UserDetails userDetails) boolean
        +isTokenExpired(String token) boolean
    }

    class CustomUserDetailsService {
        -UserRepository userRepository
        +loadUserByUsername(String email) UserDetails
    }

    class UserPrincipal {
        -Long id
        -String name
        -String email
        -String password
        -UserRole role
        -boolean active
        -boolean approved
        -Collection authorities
        +create(User user)$ UserPrincipal
        +getAuthorities() Collection
        +getPassword() String
        +getUsername() String
        +isEnabled() boolean
    }

    class JwtAuthenticationFilter {
        -JwtService jwtService
        -CustomUserDetailsService userDetailsService
        #doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain) void
    }

    class SecurityConfig {
        -JwtAuthenticationFilter jwtAuthenticationFilter
        -CustomUserDetailsService userDetailsService
        +securityFilterChain(HttpSecurity http) SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationManager(AuthenticationConfiguration config) AuthenticationManager
        +authenticationProvider() AuthenticationProvider
    }

    UserDetailsService <|.. CustomUserDetailsService : implements
    UserDetails <|.. UserPrincipal : implements
    OncePerRequestFilter <|-- JwtAuthenticationFilter : inherits
    JwtAuthenticationFilter --> JwtService : uses
    JwtAuthenticationFilter --> CustomUserDetailsService : uses
    SecurityConfig --> JwtAuthenticationFilter : injects
```

---

## 5. DTO Records (Data Transfer Objects)

```mermaid
classDiagram
    direction LR

    class RegisterRequest {
        <<record>>
        +String name
        +String email
        +String password
        +UserRole role
    }

    class LoginRequest {
        <<record>>
        +String email
        +String password
    }

    class AuthResponse {
        <<record>>
        +String token
        +String tokenType
        +long expiresIn
        +UserResponse user
    }

    class UserResponse {
        <<record>>
        +Long id
        +String name
        +String email
        +UserRole role
        +boolean active
        +boolean approved
        +Instant createdAt
        +fromEntity(User user)$ UserResponse
    }

    class CreateJobRequest {
        <<record>>
        +String title
        +String description
        +String category
        +String location
        +BigDecimal payAmount
        +String payUnit
        +Instant startsAt
        +Instant endsAt
        +Integer capacity
    }

    class JobResponse {
        <<record>>
        +Long id
        +Long employerId
        +String employerName
        +String title
        +String description
        +String category
        +String location
        +BigDecimal payAmount
        +String payUnit
        +Instant startsAt
        +Instant endsAt
        +int capacity
        +long confirmedBookings
        +int remainingCapacity
        +JobStatus status
        +Instant createdAt
        +Instant publishedAt
        +fromEntity(Job job, long confirmedBookings)$ JobResponse
    }

    class BookingResponse {
        <<record>>
        +Long id
        +Long jobId
        +String jobTitle
        +String jobLocation
        +Long studentId
        +String studentName
        +String studentEmail
        +BookingStatus status
        +Instant createdAt
        +fromEntity(Booking booking)$ BookingResponse
    }
```

---

## 6. OOP මූලධර්ම ක්‍රියාත්මක කර ඇති ආකාරය පිළිබඳ සාරාංශය

1. **Encapsulation (කැප්සියුලීකරණය)**:
   - සියලුම entities හි fields `private` කර ඇත.
   - Entity හි තත්ත්ව වෙනස්වීම් (`publish()`, `approve()`, `isEligibleForBooking()`) entity එක තුළම ආරක්ෂිත domain methods මඟින් පාලනය වේ.

2. **Abstraction (වියුක්තකරණය)**:
   - `JobRecommendationStrategy` අතුරුමුහුණත (interface) මඟින් රැකියා නිර්දේශ කිරීමේ ඇල්ගොරිතමයේ ක්‍රියාකාරීත්වය application layer එකෙන් වෙන් කර ඇත.

3. **Polymorphism (බහුරූපතාවය)**:
   - `JobService` එක සෘජුවම concrete class එකක් මත රඳා නොපවතින අතර, `JobRecommendationStrategy` interface එක මත පමණක් රඳා පවතී. අනාගතයේදී වෙනත් recommendations ඇල්ගොරිතමයක් ලියූ විට `JobService` වෙනස් නොකර එය සම්බන්ධ කළ හැක.

4. **Inheritance (උරුමය)**:
   - `BaseEntity` mapped superclass එක මඟින් `id`, `createdAt`, `updatedAt` වැනි පොදු ගුණාංග සහ methods සියලුම entities (`User`, `Job`, `Booking`) වෙත සාධාරණීකරණය කර උරුම කර දී ඇත.
