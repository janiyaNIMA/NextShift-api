package com.unishift.unishift_backend.gamification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SubmitWorkSessionRequest(
    @NotNull(message = "startedAt timestamp is required")
    @JsonAlias({"started_at", "startTime", "start_time"})
    Instant startedAt,

    @NotNull(message = "endedAt timestamp is required")
    @JsonAlias({"ended_at", "endTime", "end_time"})
    Instant endedAt,

    String notes
) {
    @JsonProperty("started_at")
    public Instant startedAtAlias() {
        return startedAt;
    }

    @JsonProperty("ended_at")
    public Instant endedAtAlias() {
        return endedAt;
    }
}
