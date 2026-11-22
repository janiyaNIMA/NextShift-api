package com.unishift.unishift_backend.gamification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GamificationSummary(
    int totalPoints,
    long totalVerifiedMinutes,
    ElementTierResponse currentTier,
    ElementTierResponse nextTier,
    int pointsToNextTier,
    double progressPercent
) {
    @JsonProperty("total_points")
    public int totalPointsAlias() {
        return totalPoints;
    }

    @JsonProperty("total_verified_minutes")
    public long totalVerifiedMinutesAlias() {
        return totalVerifiedMinutes;
    }

    @JsonProperty("current_tier")
    public ElementTierResponse currentTierAlias() {
        return currentTier;
    }

    @JsonProperty("next_tier")
    public ElementTierResponse nextTierAlias() {
        return nextTier;
    }

    @JsonProperty("points_to_next_tier")
    public int pointsToNextTierAlias() {
        return pointsToNextTier;
    }

    @JsonProperty("progress_percent")
    public double progressPercentAlias() {
        return progressPercent;
    }
}
