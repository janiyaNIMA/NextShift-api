package com.unishift.unishift_backend.gamification.repository;

import com.unishift.unishift_backend.gamification.entity.StudentGamification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentGamificationRepository extends JpaRepository<StudentGamification, Long> {

    Optional<StudentGamification> findByStudentId(Long studentId);

    @Query("SELECT g FROM StudentGamification g JOIN FETCH g.student s LEFT JOIN FETCH g.currentTier WHERE s.email = :email")
    Optional<StudentGamification> findByStudentEmail(@Param("email") String email);
}
