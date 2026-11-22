package com.unishift.unishift_backend.gamification.entity;

import com.unishift.unishift_backend.common.BaseEntity;
import com.unishift.unishift_backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_gamification")
public class StudentGamification extends BaseEntity {

    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Column(name = "total_verified_minutes", nullable = false)
    private long totalVerifiedMinutes = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_element_tier_id")
    private ElementTier currentTier;

    public StudentGamification() {
    }

    public StudentGamification(User student) {
        this.student = student;
        this.totalPoints = 0;
        this.totalVerifiedMinutes = 0;
        this.currentTier = null;
    }

    public User getStudent() {
        return student;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public long getTotalVerifiedMinutes() {
        return totalVerifiedMinutes;
    }

    public ElementTier getCurrentTier() {
        return currentTier;
    }

    /**
     * Domain method to encapsulate point and minute accumulation with tier synchronization.
     */
    public void addVerifiedWork(int pointsDelta, long minutesDelta, ElementTier newTier) {
        if (pointsDelta < 0 || minutesDelta < 0) {
            throw new IllegalArgumentException("Points and minutes delta must be non-negative");
        }
        this.totalPoints += pointsDelta;
        this.totalVerifiedMinutes += minutesDelta;
        this.currentTier = newTier;
    }

    public void setCurrentTier(ElementTier currentTier) {
        this.currentTier = currentTier;
    }
}
