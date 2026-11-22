package com.unishift.unishift_backend.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unishift.unishift_backend.booking.Booking;
import com.unishift.unishift_backend.booking.BookingStatus;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record BookingResponse(
    Long id,
    Long jobId,
    String jobTitle,
    String jobLocation,
    Long studentId,
    String studentName,
    String studentEmail,
    BookingStatus status,
    Instant createdAt,
    String jobCategory,
    BigDecimal payAmount,
    String payUnit,
    Instant startsAt,
    Instant endsAt,
    String jobDescription
) {
    public BookingResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String jobLocation,
        Long studentId,
        String studentName,
        String studentEmail,
        BookingStatus status,
        Instant createdAt
    ) {
        this(id, jobId, jobTitle, jobLocation, studentId, studentName, studentEmail, status, createdAt,
             null, null, null, null, null, null);
    }

    public static BookingResponse fromEntity(Booking booking) {
        Job job = booking.getJob();
        User student = booking.getStudent();
        return new BookingResponse(
            booking.getId(),
            job != null ? job.getId() : null,
            job != null ? job.getTitle() : null,
            job != null ? job.getLocation() : null,
            student != null ? student.getId() : null,
            student != null ? student.getName() : null,
            student != null ? student.getEmail() : null,
            booking.getStatus(),
            booking.getCreatedAt(),
            job != null ? job.getCategory() : null,
            job != null ? job.getPayAmount() : null,
            job != null ? job.getPayUnit() : null,
            job != null ? job.getStartsAt() : null,
            job != null ? job.getEndsAt() : null,
            job != null ? job.getDescription() : null
        );
    }

    @JsonProperty("job_id")
    public Long jobIdAlias() {
        return jobId;
    }

    @JsonProperty("student_id")
    public Long studentIdAlias() {
        return studentId;
    }

    @JsonProperty("created_at")
    public Instant createdAtAlias() {
        return createdAt;
    }

    @JsonProperty("student")
    public Map<String, Object> student() {
        if (studentId == null && studentName == null && studentEmail == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", studentId);
        map.put("name", studentName);
        map.put("email", studentEmail);
        return map;
    }

    @JsonProperty("job")
    public Map<String, Object> job() {
        if (jobId == null && jobTitle == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", jobId);
        map.put("title", jobTitle);
        map.put("location", jobLocation);
        map.put("category", jobCategory);
        map.put("pay_amount", payAmount);
        map.put("payAmount", payAmount);
        map.put("pay_unit", payUnit);
        map.put("payUnit", payUnit);
        map.put("starts_at", startsAt);
        map.put("startsAt", startsAt);
        map.put("ends_at", endsAt);
        map.put("endsAt", endsAt);
        map.put("description", jobDescription);
        return map;
    }
}

