package com.unishift.unishift_backend.gamification.entity;

import com.unishift.unishift_backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "element_tiers")
public class ElementTier extends BaseEntity {

    @Column(name = "tier_code", nullable = false, unique = true, length = 50)
    private String tierCode;

    @Column(name = "element_name", nullable = false, length = 100)
    private String elementName;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(name = "mass_number", nullable = false)
    private int massNumber;

    @Column(name = "required_points", nullable = false, unique = true)
    private int requiredPoints;

    @Column(name = "tier_order", nullable = false)
    private int tierOrder;

    @Column(nullable = false)
    private boolean active = true;

    public ElementTier() {
    }

    public ElementTier(String tierCode, String elementName, String symbol, int massNumber, int requiredPoints, int tierOrder, boolean active) {
        this.tierCode = tierCode;
        this.elementName = elementName;
        this.symbol = symbol;
        this.massNumber = massNumber;
        this.requiredPoints = requiredPoints;
        this.tierOrder = tierOrder;
        this.active = active;
    }

    public String getTierCode() {
        return tierCode;
    }

    public String getElementName() {
        return elementName;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getMassNumber() {
        return massNumber;
    }

    public int getRequiredPoints() {
        return requiredPoints;
    }

    public int getTierOrder() {
        return tierOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
