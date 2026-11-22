package com.unishift.unishift_backend.gamification.strategy;

import org.springframework.stereotype.Component;

/**
 * Production implementation of WorkPointCalculator:
 * 1 verified completed hour = 1 point.
 * Never rounds up: floor(verifiedMinutes / 60).
 */
@Component
public class WholeHourWorkPointCalculator implements WorkPointCalculator {

    @Override
    public int calculatePoints(long verifiedMinutes) {
        if (verifiedMinutes <= 0) {
            return 0;
        }
        return (int) (verifiedMinutes / 60);
    }
}
