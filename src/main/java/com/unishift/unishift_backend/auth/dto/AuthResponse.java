package com.unishift.unishift_backend.auth.dto;

public record AuthResponse(
    String token,
    String tokenType,
    long expiresIn,
    UserResponse user
) {
    public AuthResponse(String token, long expiresIn, UserResponse user) {
        this(token, "Bearer", expiresIn, user);
    }
}
