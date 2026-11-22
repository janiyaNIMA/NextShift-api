package com.unishift.unishift_backend.gamification.entity;

import com.unishift.unishift_backend.common.BaseEntity;
import com.unishift.unishift_backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_transactions")
public class PointTransaction extends BaseEntity {

    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_session_id")
    private WorkSession workSession;

    @Column(name = "points_delta", nullable = false)
    private int pointsDelta;

    @Column(nullable = false)
    private String reason;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    public PointTransaction() {
    }

    public PointTransaction(User student, WorkSession workSession, int pointsDelta, String reason, String idempotencyKey) {
        this.student = student;
        this.workSession = workSession;
        this.pointsDelta = pointsDelta;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
    }

    public User getStudent() {
        return student;
    }

    public WorkSession getWorkSession() {
        return workSession;
    }

    public int getPointsDelta() {
        return pointsDelta;
    }

    public String getReason() {
        return reason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
