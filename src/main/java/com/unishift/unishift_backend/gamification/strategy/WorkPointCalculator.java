package com.unishift.unishift_backend.gamification.strategy;

/**
 * Strategy abstraction for calculating gamification points earned from work duration.
 */
public interface WorkPointCalculator {

    /**
     * Calculates points from total verified minutes.
     * Core Rule: 1 hour = 1 point; floor(verifiedMinutes / 60); never round partial hours up.
     */
    int calculatePoints(long verifiedMinutes);
}
