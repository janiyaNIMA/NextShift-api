package com.unishift.unishift_backend.gamification.repository;

import com.unishift.unishift_backend.gamification.entity.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<PointTransaction> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);
}
