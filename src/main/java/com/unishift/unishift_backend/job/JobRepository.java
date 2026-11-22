package com.unishift.unishift_backend.job;

import com.unishift.unishift_backend.booking.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j JOIN FETCH j.employer WHERE j.id = :id")
    Optional<Job> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT j FROM Job j JOIN FETCH j.employer WHERE j.id = :id")
    Optional<Job> findByIdWithEmployer(@Param("id") Long id);

    @Query("SELECT j FROM Job j JOIN FETCH j.employer WHERE j.employer.id = :employerId ORDER BY j.createdAt DESC")
    List<Job> findByEmployerIdOrderByCreatedAtDesc(@Param("employerId") Long employerId);

    @Query("SELECT j FROM Job j JOIN FETCH j.employer WHERE j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findByStatusOrderByCreatedAtDesc(@Param("status") JobStatus status);

    @Query(
        value = """
            SELECT j FROM Job j
            JOIN j.employer e
            WHERE j.status = :status
              AND e.active = true
              AND e.approved = true
              AND j.startsAt > :now
              AND (CAST(:category AS string) IS NULL OR LOWER(j.category) = LOWER(CAST(:category AS string)))
              AND (CAST(:location AS string) IS NULL OR LOWER(j.location) = LOWER(CAST(:location AS string)))
              AND j.capacity > (
                  SELECT COUNT(b) FROM Booking b
                  WHERE b.job = j AND b.status = :confirmedStatus
              )
        """,
        countQuery = """
            SELECT COUNT(j) FROM Job j
            JOIN j.employer e
            WHERE j.status = :status
              AND e.active = true
              AND e.approved = true
              AND j.startsAt > :now
              AND (CAST(:category AS string) IS NULL OR LOWER(j.category) = LOWER(CAST(:category AS string)))
              AND (CAST(:location AS string) IS NULL OR LOWER(j.location) = LOWER(CAST(:location AS string)))
              AND j.capacity > (
                  SELECT COUNT(b) FROM Booking b
                  WHERE b.job = j AND b.status = :confirmedStatus
              )
        """
    )
    Page<Job> findEligibleJobs(
        @Param("status") JobStatus status,
        @Param("now") Instant now,
        @Param("category") String category,
        @Param("location") String location,
        @Param("confirmedStatus") BookingStatus confirmedStatus,
        Pageable pageable
    );
}
