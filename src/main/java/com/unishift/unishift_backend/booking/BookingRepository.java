package com.unishift.unishift_backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);

    long countByJobIdAndStatus(Long jobId, BookingStatus status);

    @Query("SELECT b FROM Booking b JOIN FETCH b.job j JOIN FETCH b.student s WHERE b.student.id = :studentId ORDER BY b.createdAt DESC")
    List<Booking> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") Long studentId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.job j JOIN FETCH b.student s WHERE b.job.id = :jobId ORDER BY b.createdAt DESC")
    List<Booking> findByJobIdOrderByCreatedAtDesc(@Param("jobId") Long jobId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.job j JOIN FETCH b.student s WHERE b.job.id = :jobId AND b.status = :status ORDER BY b.createdAt DESC")
    List<Booking> findByJobIdAndStatusOrderByCreatedAtDesc(@Param("jobId") Long jobId, @Param("status") BookingStatus status);
}
