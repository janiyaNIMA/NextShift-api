package com.unishift.unishift_backend.gamification.repository;

import com.unishift.unishift_backend.gamification.entity.ElementTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElementTierRepository extends JpaRepository<ElementTier, Long> {

    Optional<ElementTier> findByTierCode(String tierCode);

    List<ElementTier> findByActiveTrueOrderByRequiredPointsAsc();

    /**
     * Resolves highest configured active tier threshold <= points.
     */
    Optional<ElementTier> findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(int points);

    /**
     * Resolves the immediate next tier threshold > points.
     */
    Optional<ElementTier> findFirstByActiveTrueAndRequiredPointsGreaterThanOrderByRequiredPointsAsc(int points);
}
