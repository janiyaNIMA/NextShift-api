package com.unishift.unishift_backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    Map<String, String> details
) {
    public ErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> details) {
        this(Instant.now(), status, error, message, details);
    }

    @JsonProperty("detail")
    public String detailAlias() {
        return message;
    }
}
