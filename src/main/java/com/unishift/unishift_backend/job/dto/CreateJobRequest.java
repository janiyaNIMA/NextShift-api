package com.unishift.unishift_backend.job.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateJobRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Description is required")
    String description,

    @NotBlank(message = "Category is required")
    String category,

    @NotBlank(message = "Location is required")
    String location,

    @NotNull(message = "Pay amount is required")
    @DecimalMin(value = "0.01", message = "Pay amount must be greater than zero")
    BigDecimal payAmount,

    @NotBlank(message = "Pay unit is required")
    String payUnit,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    Instant startsAt,

    @NotNull(message = "End time is required")
    Instant endsAt,

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity
) {
}
