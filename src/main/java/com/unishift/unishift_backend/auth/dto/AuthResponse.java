package com.unishift.unishift_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
    String token,
    String tokenType,
    long expiresIn,
    UserResponse user
) {
    public AuthResponse(String token, long expiresIn, UserResponse user) {
        this(token, "Bearer", expiresIn, user);
    }

    @JsonProperty("access_token")
    public String accessToken() {
        return token;
    }

    @JsonProperty("token_type")
    public String tokenTypeAlias() {
        return tokenType;
    }
}

