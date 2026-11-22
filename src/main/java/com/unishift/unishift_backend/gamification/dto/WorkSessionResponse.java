package com.unishift.unishift_backend.gamification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unishift.unishift_backend.gamification.entity.WorkSession;
import com.unishift.unishift_backend.gamification.enums.WorkSessionStatus;

import java.time.Instant;

public record WorkSessionResponse(
    Long id,
    Long bookingId,
    Long jobId,
    String jobTitle,
    Long studentId,
    String studentName,
    Instant startedAt,
    Instant endedAt,
    long verifiedMinutes,
    WorkSessionStatus status,
    Instant submittedAt,
    Instant verifiedAt,
    Long verifiedBy,
    String notes
) {
    public static WorkSessionResponse fromEntity(WorkSession ws) {
        if (ws == null) return null;
        Long bId = ws.getBooking() != null ? ws.getBooking().getId() : null;
        Long jId = (ws.getBooking() != null && ws.getBooking().getJob() != null) ? ws.getBooking().getJob().getId() : null;
        String title = (ws.getBooking() != null && ws.getBooking().getJob() != null) ? ws.getBooking().getJob().getTitle() : null;
        Long sId = (ws.getBooking() != null && ws.getBooking().getStudent() != null) ? ws.getBooking().getStudent().getId() : null;
        String sName = (ws.getBooking() != null && ws.getBooking().getStudent() != null) ? ws.getBooking().getStudent().getName() : null;
        Long vById = ws.getVerifiedBy() != null ? ws.getVerifiedBy().getId() : null;

        return new WorkSessionResponse(
            ws.getId(),
            bId,
            jId,
            title,
            sId,
            sName,
            ws.getStartedAt(),
            ws.getEndedAt(),
            ws.getVerifiedMinutes(),
            ws.getStatus(),
            ws.getSubmittedAt(),
            ws.getVerifiedAt(),
            vById,
            ws.getNotes()
        );
    }

    @JsonProperty("booking_id")
    public Long bookingIdAlias() {
        return bookingId;
    }

    @JsonProperty("job_id")
    public Long jobIdAlias() {
        return jobId;
    }

    @JsonProperty("job_title")
    public String jobTitleAlias() {
        return jobTitle;
    }

    @JsonProperty("student_id")
    public Long studentIdAlias() {
        return studentId;
    }

    @JsonProperty("student_name")
    public String studentNameAlias() {
        return studentName;
    }

    @JsonProperty("started_at")
    public Instant startedAtAlias() {
        return startedAt;
    }

    @JsonProperty("ended_at")
    public Instant endedAtAlias() {
        return endedAt;
    }

    @JsonProperty("verified_minutes")
    public long verifiedMinutesAlias() {
        return verifiedMinutes;
    }

    @JsonProperty("submitted_at")
    public Instant submittedAtAlias() {
        return submittedAt;
    }

    @JsonProperty("verified_at")
    public Instant verifiedAtAlias() {
        return verifiedAt;
    }

    @JsonProperty("verified_by")
    public Long verifiedByAlias() {
        return verifiedBy;
    }
}
