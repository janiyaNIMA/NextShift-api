package com.unishift.unishift_backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);

    long countByJobIdAndStatus(Long jobId, BookingStatus status);

    List<Booking> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Booking> findByJobIdOrderByCreatedAtDesc(Long jobId);

    List<Booking> findByJobIdAndStatusOrderByCreatedAtDesc(Long jobId, BookingStatus status);
}
