# NextShift Element Journey — Backend Team Guide

**Project:** NextShift – Campus Gig Platform  
**Backend stack:** Java 21, Spring Boot, MySQL  
**Feature branch:** `feature/student-gamification-backend`

## 1. Objective

Implement the backend support for **NextShift Element Journey**, a gamification feature where students earn points for verified completed work and progress through chemistry element/isotope tiers.

### Core rules

- 1 verified completed working hour = 1 point.
- Only employer-confirmed work earns points.
- Calculate points on the server; never trust points sent by the frontend.
- Use whole completed hours: `floor(verifiedMinutes / 60)`.
- Do not round partial hours up.
- Points accumulate over the student's lifetime.
- The current tier is the highest configured threshold reached.
- `requiredPoints = isotopeMassNumber * 10`.
- Below 10 points, the student is `STARTER`/`UNRANKED` and has no element tier.
- Preserve the existing registration, authentication, employer approval, job approval and booking workflows.

The isotope progression is a curated gamification sequence, not a complete periodic table. Some listed isotopes are radioactive; their use here is thematic.

## 2. Initial tier data

Store tiers in the database. Do not hard-code thresholds throughout service logic.

| Order | Tier code | Element | Symbol | Mass number | Required points |
|---:|---|---|---|---:|---:|
| 1 | H-1 | Hydrogen | H | 1 | 10 |
| 2 | He-4 | Helium | He | 4 | 40 |
| 3 | Li-7 | Lithium | Li | 7 | 70 |
| 4 | Be-9 | Beryllium | Be | 9 | 90 |
| 5 | B-11 | Boron | B | 11 | 110 |
| 6 | C-12 | Carbon | C | 12 | 120 |
| 7 | C-14 | Carbon | C | 14 | 140 |
| 8 | N-15 | Nitrogen | N | 15 | 150 |
| 9 | O-16 | Oxygen | O | 16 | 160 |
| 10 | F-19 | Fluorine | F | 19 | 190 |
| 11 | Ne-20 | Neon | Ne | 20 | 200 |
| 12 | Na-23 | Sodium | Na | 23 | 230 |
| 13 | Mg-24 | Magnesium | Mg | 24 | 240 |
| 14 | Al-27 | Aluminium | Al | 27 | 270 |
| 15 | Si-28 | Silicon | Si | 28 | 280 |
| 16 | P-31 | Phosphorus | P | 31 | 310 |
| 17 | S-32 | Sulfur | S | 32 | 320 |
| 18 | Cl-35 | Chlorine | Cl | 35 | 350 |
| 19 | Ar-40 | Argon | Ar | 40 | 400 |

Validation:
- `mass_number > 0`.
- `required_points = mass_number * 10`.
- `tier_code` unique; `C-12` and `C-14` must be distinct.
- `required_points` unique and strictly increasing.
- Resolve the highest threshold `<= totalPoints`.

## 3. Inspect before implementation

Before editing:
1. Confirm the active Git branch.
2. Inspect entities, repositories, services, controllers and DTO conventions.
3. Inspect the existing booking lifecycle and determine how completed work is represented.
4. Inspect Spring Security and authenticated-principal handling.
5. Inspect MySQL schema and the project's migration tool.
6. Inspect existing tests and package structure.

Do not initialize a new Spring Boot project, duplicate existing entities, or refactor authentication unnecessarily. If the project has no work-completion process, propose a minimal additive work-session flow and document the integration point before changing code.

## 4. Database design

Adapt these suggested tables to the actual schema. Reuse existing Student, User, Employer and Booking entities.

### `element_tiers`

Suggested columns:
- `id` primary key.
- `tier_code` unique, e.g. `C-14`.
- `element_name`.
- `symbol`.
- `mass_number`.
- `required_points` unique.
- `tier_order`.
- `active`.
- optional description and timestamps.

Seed the initial tier list above through the existing migration tool (Flyway if already configured). Do not introduce a second migration framework.

### `student_gamification`

One row per student:
- `id`.
- `student_id` unique FK to existing student/user representation.
- `total_points` non-negative integer/BIGINT.
- `current_element_tier_id` nullable FK to `element_tiers`.
- `total_verified_minutes` non-negative BIGINT.
- `created_at`, `updated_at`.
- optional optimistic-lock `version`.

The tier is derived from total points. A cached current-tier FK is acceptable for fast reads, but must be updated/reconciled whenever points change.

### `work_sessions`

Represents actual work performed for an existing booking:
- `id`.
- `booking_id` FK to existing booking.
- `started_at`, `ended_at`.
- `verified_minutes`.
- `status`, such as `SUBMITTED`, `CONFIRMED`, `REJECTED`.
- `submitted_at`, `verified_at`.
- `verified_by` reference to confirming employer/user.
- timestamps.

Avoid redundant student/employer columns unless required for auditing or querying. If stored, validate consistency against the booking and job ownership.

### `point_transactions`

Append-only audit ledger:
- `id`.
- `student_id`.
- `work_session_id` source.
- `points_delta`.
- `reason`, e.g. `VERIFIED_WORK_HOURS`.
- unique `idempotency_key`.
- `created_at`.
- optional `created_by`.

Do not silently edit/delete awarded-point records. Future corrections should use auditable reversal/adjustment transactions with authorization.

### Constraints and indexes

Consider:
- Unique `student_gamification.student_id`.
- Unique `element_tiers.tier_code` and `required_points`.
- Unique `point_transactions.idempotency_key`.
- Unique award per work session (or `(work_session_id, reason)` where appropriate).
- Indexes on `point_transactions(student_id, created_at)`, `work_sessions(booking_id, status)`, and `element_tiers(required_points)`.
- Deliberate FK delete behaviour. Do not cascade-delete the point ledger when a student or booking is removed.

The ledger is the audit source. `student_gamification.total_points` is a cached aggregate. Update both atomically and provide a reconciliation test or safe reconciliation mechanism.

## 5. Work completion and point awarding

Recommended lifecycle:

1. Student books a published job using the existing booking flow.
2. Student completes the work.
3. Student submits a work session with actual start/end times (or a verified duration, depending on the established design).
4. The employer who owns the job confirms it.
5. Backend validates the work session.
6. Backend calculates eligible whole-hour points.
7. Backend writes the point ledger, updates the total and resolves the tier.
8. Work session is marked confirmed.

Do not award points when a booking is created or merely confirmed. Preserve the current booking semantics.

### Duration policy

- Derive `verifiedMinutes` from validated start/end timestamps, unless the established project design uses a trusted verified-duration field.
- `pointsAwarded = floor(verifiedMinutes / 60)`.
- Never round up.
- Reject zero/negative duration and impossible timestamps.
- Enforce a plausible maximum shift duration.
- If multiple sessions contribute to one accumulation, carry partial minutes forward. Example: 45 + 30 minutes = 1 point total, not two awards.
- If only one work session per booking is allowed, enforce it.

A student cannot approve their own work. Only the employer who owns the job can confirm the session. Unconfirmed/rejected sessions earn no points.

## 6. OOP design requirements

Use OOP to solve actual domain problems.

### Encapsulation
- Use private entity fields.
- Give domain entities controlled methods for valid transitions and point changes.
- `StudentGamification` should control adding verified points and updating its tier.
- `WorkSession` should control valid status transitions.
- Avoid unrestricted setters for `totalPoints`, `currentTier` and work-session `status`.

### Inheritance
- Reuse the existing `User` → `Student` / `Employer` inheritance if present.
- Do not refactor the user/authentication model just to force inheritance.
- Gamification belongs to Student, not all User types.
- If the current project uses a single User entity with a role enum, preserve it and explain the association.

### Abstraction
Create focused interfaces where variation is real:

```java
public interface WorkPointCalculator {
    int calculatePoints(long verifiedMinutes);
}

public interface ElementTierResolver {
    ElementTier resolveTier(int totalPoints);
}
```

Implement:
- `WholeHourWorkPointCalculator`
- `ThresholdElementTierResolver`

### Polymorphism
Inject and use the interfaces in application services. The current whole-hour calculator and threshold resolver are the production implementations. Document future alternatives as extension points; do not claim they are implemented.

### Composition
Compose the gamification service from calculator, tier resolver and repositories. Associate the profile with the existing Student and work sessions with existing bookings.

Avoid artificial design patterns and unnecessary class hierarchies.

## 7. Suggested package structure

Adapt to the existing package root and naming style:

```text
gamification/
  controller/
  dto/
  entity/
  enums/
  repository/
  service/
  strategy/
  exception/
```

Possible classes:
- `GamificationController`
- `GamificationService`
- `WorkSessionService`
- `StudentGamification`
- `ElementTier`
- `PointTransaction`
- `WorkSession`
- `WorkSessionStatus`
- `WorkPointCalculator`
- `WholeHourWorkPointCalculator`
- `ElementTierResolver`
- `ThresholdElementTierResolver`

Do not duplicate existing domain entities.

## 8. API contract proposal

Agree on the final contract with the frontend team before implementation. Follow existing `/api/v1` conventions.

### Student endpoints

`GET /api/v1/students/me/gamification`

Returns:
- `totalPoints`
- `currentTier` (nullable below first threshold)
- `nextTier` (nullable at maximum tier)
- `pointsToNextTier`
- `progressPercent`

`GET /api/v1/students/me/gamification/transactions?page=0&size=20`

Returns paginated point history:
- transaction date
- points delta
- reason
- related work-session/booking reference

`GET /api/v1/students/me/gamification/tiers`

Returns active tier progression for the UI.

### Work-session endpoints

Proposed only; adapt to existing booking routes and authorization:

- `POST /api/v1/bookings/{bookingId}/work-sessions` — student submits completed-work details.
- `POST /api/v1/employer/work-sessions/{sessionId}/confirm` — owning employer confirms.
- Optional rejection endpoint if supported by the existing workflow.

Never accept client-provided points, total points or tier. Derive student/employer identity from the authenticated principal and existing booking/job relationships. Students can access only their own records; employers can confirm only sessions for jobs they own.

### Example summary response

```json
{
  "totalPoints": 140,
  "currentTier": {
    "tierCode": "C-14",
    "elementName": "Carbon",
    "symbol": "C",
    "massNumber": 14,
    "requiredPoints": 140
  },
  "nextTier": {
    "tierCode": "N-15",
    "elementName": "Nitrogen",
    "symbol": "N",
    "massNumber": 15,
    "requiredPoints": 150
  },
  "pointsToNextTier": 10,
  "progressPercent": 0.0
}
```

Progress is calculated between current and next tier thresholds:
`(totalPoints - currentThreshold) / (nextThreshold - currentThreshold) * 100`.

At 140 points, progress from C-14 to N-15 is 0%. For unranked students, measure from 0 to the first threshold. At the highest tier, return `nextTier = null`, `pointsToNextTier = 0`, `progressPercent = 100`.

## 9. Transaction safety and duplicate prevention

Employer confirmation must be one atomic transaction:

1. Load work session and booking.
2. Verify employer owns the job.
3. Verify session is confirmable.
4. Validate duration and booking eligibility.
5. Calculate newly eligible whole-hour points.
6. Insert point transaction with unique idempotency key.
7. Update student total and verified minutes.
8. Resolve/update current tier.
9. Mark session confirmed.
10. Commit.

Use `@Transactional`, database uniqueness and suitable locking/optimistic concurrency. Repeated or concurrent confirmation must not award points twice.

## 10. Tests

### Point calculation
- 0 minutes → 0 points.
- 59 → 0.
- 60 → 1.
- 90 → 1.
- 120 → 2.
- Negative/invalid duration → rejected.

### Tier resolution
- 0 and 9 points → unranked.
- 10 → H-1.
- 40 → He-4.
- 139 → C-12.
- 140 → C-14.
- 150 → N-15.
- Above 400 → highest configured tier.

### Authorization and integrity
- Student cannot view another student's history.
- Student cannot confirm their own work.
- Employer cannot confirm another employer's session.
- Unconfirmed/rejected work earns no points.
- Repeated/concurrent confirmation does not duplicate points.
- Partial-hour carry-over works if multiple sessions are supported.
- Ledger and cached total remain consistent.

Do not weaken or delete existing tests. Run the full suite.

## 11. Implementation phases

**Phase 0 — Inspection:** inspect branch, architecture, booking/work-completion workflow, security and schema. Report integration plan before editing.

**Phase 1 — Database:** add additive migration and seed tier data. Verify migration against the current schema.

**Phase 2 — Domain/OOP:** add entities, enums, controlled methods, interfaces and implementations.

**Phase 3 — Services:** implement work-session verification, point ledger, calculation, tier updates and transaction safety.

**Phase 4 — API/security:** add DTOs/endpoints and enforce ownership.

**Phase 5 — Tests:** add unit/integration tests and run the existing suite.

**Phase 6 — Documentation:** document schema, endpoints, point rules, tier rules, security, OOP concepts, migration/rollback notes and changed files.

## 12. Backend Git workflow

Do not work directly on `main`.

First inspect:
```bash
git status
git branch --show-current
git branch
```

Create the branch from the branch that contains the latest integrated NextShift backend. If that is `main` and the working tree is clean:

```bash
git switch main
git pull origin main
git switch -c feature/student-gamification-backend
```

If current NextShift work is on another branch, branch from that branch instead. Preserve any uncommitted changes; do not discard them.

Verify:
```bash
git branch --show-current
git status
```

After implementation and tests:
```bash
git add <reviewed-files>
git commit -m "Add Element Journey gamification backend"
git push -u origin feature/student-gamification-backend
```

Open a Pull Request into the team's integration branch. Coordinate API contract and response shape with the frontend team before merging.
