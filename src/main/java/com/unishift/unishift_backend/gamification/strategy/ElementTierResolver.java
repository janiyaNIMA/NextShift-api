package com.unishift.unishift_backend.gamification.strategy;

import com.unishift.unishift_backend.gamification.entity.ElementTier;

/**
 * Strategy abstraction for resolving a student's chemistry element/isotope tier.
 */
public interface ElementTierResolver {

    /**
     * Resolves the highest configured active threshold reached.
     * Below 10 points: returns null (STARTER / UNRANKED).
     */
    ElementTier resolveTier(int totalPoints);
}
