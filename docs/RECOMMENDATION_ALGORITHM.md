# NextShift / UniShift — Job Recommendation Strategy Specification

## 1. Context and Objective

In accordance with the MVP requirements for NextShift, the recommendation system deliberately avoids subjective, opaque heuristics, or personalized AI models. Instead, **every registered student and platform visitor receives the exact same deterministic feed of all eligible vacancies**.

The algorithm is cleanly encapsulated using the **Strategy Pattern** to ensure future ranking, scoring, or matching algorithms can be introduced seamlessly.

---

## 2. Strategy Interface Design

The recommendation contract is defined in:
`com.unishift.unishift_backend.recommendation.JobRecommendationStrategy`

```java
public interface JobRecommendationStrategy {
    Page<Job> getRecommendedJobs(Long studentId, String category, String location, Pageable pageable);
}
```

- **Abstraction**: Isolates vacancy retrieval and ranking logic from controllers and services.
- **Polymorphism**: `JobService` invokes `JobRecommendationStrategy` polymorphically.
- **Extensibility**: Future strategies (e.g., `ProximityBasedRecommendationStrategy`, `SkillMatchedRecommendationStrategy`) implement this interface without modifying `JobService`.

---

## 3. Algorithm V0: AllPublishedJobsStrategy

The active default implementation is:
`com.unishift.unishift_backend.recommendation.AllPublishedJobsStrategy`

### 3.1 Eligibility Criteria
A vacancy `j` is eligible for recommendation if and only if **all** of the following conditions are met:
1. **Publication Status**: `j.status == JobStatus.PUBLISHED` (newly created `PENDING` vacancies, `REJECTED` jobs, and `CLOSED` jobs are excluded).
2. **Employer Verification**: The posting employer is active (`j.employer.active == true`) and administrator-approved (`j.employer.approved == true`).
3. **Future Scheduling**: The job shift start time is strictly in the future (`j.startsAt > :now` evaluated at UTC query time).
4. **Remaining Capacity**: The vacancy has remaining booking slots:
   $$\text{capacity} > \sum [b \in \text{Bookings}(j) \text{ where } b.\text{status} = \text{CONFIRMED}]$$
5. **Search Filters**: If the user supplies `category` or `location`, case-insensitive matching is applied. These represent explicit search criteria, not individualized recommendations.

### 3.2 Deterministic Ordering
Results are strictly ordered by:
1. `publishedAt DESC` (most recently approved vacancies appear first)
2. `id DESC` (deterministic tie-breaker)

---

## 4. Query Efficiency and Prevention of N+1 Queries

To avoid the $N+1$ query problem and expensive in-memory filtering:
- Eligibility filtering, capacity calculation, and sorting are executed **entirely at the database level** within a single optimized JPQL query:
```sql
SELECT j FROM Job j
JOIN j.employer e
WHERE j.status = :status
  AND e.active = true
  AND e.approved = true
  AND j.startsAt > :now
  AND (:category IS NULL OR LOWER(j.category) = LOWER(:category))
  AND (:location IS NULL OR LOWER(j.location) = LOWER(:location))
  AND j.capacity > (
      SELECT COUNT(b) FROM Booking b
      WHERE b.job = j AND b.status = :confirmedStatus
  )
ORDER BY j.publishedAt DESC, j.id DESC
```
- **Database-Level Pagination**: Supported via `Pageable`, fetching only the requested page slice (`LIMIT` and `OFFSET`) accompanied by a corresponding `COUNT` query.
- **Indexes**: Composite database indexes on `(status, starts_at, published_at)` and `(category)`, `(location)` ensure high throughput under large vacancy catalogs.
