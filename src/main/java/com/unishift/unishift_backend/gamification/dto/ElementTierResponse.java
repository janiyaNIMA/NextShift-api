package com.unishift.unishift_backend.gamification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unishift.unishift_backend.gamification.entity.ElementTier;

public record ElementTierResponse(
    String tierCode,
    String elementName,
    String symbol,
    int massNumber,
    int requiredPoints,
    int tierOrder
) {
    public static ElementTierResponse fromEntity(ElementTier tier) {
        if (tier == null) return null;
        return new ElementTierResponse(
            tier.getTierCode(),
            tier.getElementName(),
            tier.getSymbol(),
            tier.getMassNumber(),
            tier.getRequiredPoints(),
            tier.getTierOrder()
        );
    }

    @JsonProperty("tier_code")
    public String tierCodeAlias() {
        return tierCode;
    }

    @JsonProperty("element_name")
    public String elementNameAlias() {
        return elementName;
    }

    @JsonProperty("mass_number")
    public int massNumberAlias() {
        return massNumber;
    }

    @JsonProperty("required_points")
    public int requiredPointsAlias() {
        return requiredPoints;
    }

    @JsonProperty("tier_order")
    public int tierOrderAlias() {
        return tierOrder;
    }
}
