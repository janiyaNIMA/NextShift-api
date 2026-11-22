package com.unishift.unishift_backend.gamification.strategy;

import com.unishift.unishift_backend.gamification.entity.ElementTier;
import com.unishift.unishift_backend.gamification.repository.ElementTierRepository;
import org.springframework.stereotype.Component;

/**
 * Production implementation of ElementTierResolver:
 * Resolves the highest threshold <= totalPoints from database tiers.
 * Returns null if totalPoints < 10 (Starter / Unranked).
 */
@Component
public class ThresholdElementTierResolver implements ElementTierResolver {

    private final ElementTierRepository elementTierRepository;

    public ThresholdElementTierResolver(ElementTierRepository elementTierRepository) {
        this.elementTierRepository = elementTierRepository;
    }

    @Override
    public ElementTier resolveTier(int totalPoints) {
        if (totalPoints < 10) {
            return null;
        }
        return elementTierRepository
            .findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(totalPoints)
            .orElse(null);
    }
}
