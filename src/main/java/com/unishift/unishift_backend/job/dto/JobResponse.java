package com.unishift.unishift_backend.job.dto;

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
}
