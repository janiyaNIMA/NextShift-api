package com.unishift.unishift_backend.gamification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unishift.unishift_backend.gamification.entity.PointTransaction;

import java.time.Instant;

public record PointTransactionResponse(
    Long id,
    int pointsDelta,
    String reason,
    Long workSessionId,
    Long bookingId,
    String jobTitle,
    Instant createdAt
) {
    public static PointTransactionResponse fromEntity(PointTransaction tx) {
        if (tx == null) return null;
        Long wsId = tx.getWorkSession() != null ? tx.getWorkSession().getId() : null;
        Long bId = (tx.getWorkSession() != null && tx.getWorkSession().getBooking() != null)
            ? tx.getWorkSession().getBooking().getId()
            : null;
        String title = (tx.getWorkSession() != null && tx.getWorkSession().getBooking() != null && tx.getWorkSession().getBooking().getJob() != null)
            ? tx.getWorkSession().getBooking().getJob().getTitle()
            : null;

        return new PointTransactionResponse(
            tx.getId(),
            tx.getPointsDelta(),
            tx.getReason(),
            wsId,
            bId,
            title,
            tx.getCreatedAt()
        );
    }

    @JsonProperty("points_delta")
    public int pointsDeltaAlias() {
        return pointsDelta;
    }

    @JsonProperty("work_session_id")
    public Long workSessionIdAlias() {
        return workSessionId;
    }

    @JsonProperty("booking_id")
    public Long bookingIdAlias() {
        return bookingId;
    }

    @JsonProperty("job_title")
    public String jobTitleAlias() {
        return jobTitle;
    }

    @JsonProperty("created_at")
    public Instant createdAtAlias() {
        return createdAt;
    }
}
