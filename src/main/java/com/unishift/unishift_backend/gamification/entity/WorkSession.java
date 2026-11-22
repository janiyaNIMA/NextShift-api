package com.unishift.unishift_backend.gamification.entity;

import com.unishift.unishift_backend.booking.Booking;
import com.unishift.unishift_backend.common.BaseEntity;
import com.unishift.unishift_backend.gamification.enums.WorkSessionStatus;
import com.unishift.unishift_backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "work_sessions")
public class WorkSession extends BaseEntity {

    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @Column(name = "verified_minutes", nullable = false)
    private long verifiedMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkSessionStatus status = WorkSessionStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public WorkSession() {
    }

    public WorkSession(Booking booking, Instant startedAt, Instant endedAt, long verifiedMinutes, String notes) {
        this.booking = booking;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.verifiedMinutes = verifiedMinutes;
        this.notes = notes;
        this.status = WorkSessionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public Booking getBooking() {
        return booking;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public long getVerifiedMinutes() {
        return verifiedMinutes;
    }

    public WorkSessionStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public String getNotes() {
        return notes;
    }

    /**
     * Controlled domain transition to CONFIRMED.
     */
    public void confirm(User verifiedBy) {
        if (this.status != WorkSessionStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED work sessions can be confirmed. Current status: " + this.status);
        }
        this.status = WorkSessionStatus.CONFIRMED;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = Instant.now();
    }

    /**
     * Controlled domain transition to REJECTED.
     */
    public void reject(User verifiedBy, String reason) {
        if (this.status != WorkSessionStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED work sessions can be rejected. Current status: " + this.status);
        }
        this.status = WorkSessionStatus.REJECTED;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = Instant.now();
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null ? this.notes + " | Rejection: " : "Rejection: ") + reason;
        }
    }
}
