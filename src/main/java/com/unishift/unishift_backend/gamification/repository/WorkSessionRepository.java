package com.unishift.unishift_backend.gamification.repository;

import com.unishift.unishift_backend.gamification.entity.WorkSession;
import com.unishift.unishift_backend.gamification.enums.WorkSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {

    List<WorkSession> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<WorkSession> findByBookingIdAndStatus(Long bookingId, WorkSessionStatus status);

    @Query("SELECT ws FROM WorkSession ws " +
           "JOIN FETCH ws.booking b " +
           "JOIN FETCH b.job j " +
           "JOIN FETCH b.student s " +
           "WHERE j.employer.id = :employerId AND ws.status = :status " +
           "ORDER BY ws.submittedAt DESC")
    List<WorkSession> findByEmployerIdAndStatus(
        @Param("employerId") Long employerId,
        @Param("status") WorkSessionStatus status
    );
}
