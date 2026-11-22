package com.unishift.unishift_backend.job.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.job.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record JobResponse(
    Long id,
    Long employerId,
    String employerName,
    String title,
    String description,
    String category,
    String location,
    BigDecimal payAmount,
    String payUnit,
    Instant startsAt,
    Instant endsAt,
    int capacity,
    long confirmedBookings,
    int remainingCapacity,
    JobStatus status,
    Instant createdAt,
    Instant publishedAt
) {
    public static JobResponse fromEntity(Job job, long confirmedBookings) {
        int remaining = Math.max(0, job.getCapacity() - (int) confirmedBookings);
        return new JobResponse(
            job.getId(),
            job.getEmployer() != null ? job.getEmployer().getId() : null,
            job.getEmployer() != null ? job.getEmployer().getName() : null,
            job.getTitle(),
            job.getDescription(),
            job.getCategory(),
            job.getLocation(),
            job.getPayAmount(),
            job.getPayUnit(),
            job.getStartsAt(),
            job.getEndsAt(),
            job.getCapacity(),
            confirmedBookings,
            remaining,
            job.getStatus(),
            job.getCreatedAt(),
            job.getPublishedAt()
        );
    }

    @JsonProperty("employer_id")
    public Long employerIdAlias() {
        return employerId;
    }

    @JsonProperty("employer_name")
    public String employerNameAlias() {
        return employerName;
    }

    @JsonProperty("pay_amount")
    public BigDecimal payAmountAlias() {
        return payAmount;
    }

    @JsonProperty("pay_unit")
    public String payUnitAlias() {
        return payUnit;
    }

    @JsonProperty("starts_at")
    public Instant startsAtAlias() {
        return startsAt;
    }

    @JsonProperty("ends_at")
    public Instant endsAtAlias() {
        return endsAt;
    }

    @JsonProperty("confirmed_bookings")
    public long confirmedBookingsAlias() {
        return confirmedBookings;
    }

    @JsonProperty("remaining_capacity")
    public int remainingCapacityAlias() {
        return remainingCapacity;
    }

    @JsonProperty("created_at")
    public Instant createdAtAlias() {
        return createdAt;
    }

    @JsonProperty("published_at")
    public Instant publishedAtAlias() {
        return publishedAt;
    }
}

