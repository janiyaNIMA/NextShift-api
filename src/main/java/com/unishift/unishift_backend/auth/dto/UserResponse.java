package com.unishift.unishift_backend.auth.dto;

import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRole;

import java.time.Instant;

public record UserResponse(
    Long id,
    String name,
    String email,
    UserRole role,
    boolean active,
    boolean approved,
    Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.isApproved(),
            user.getCreatedAt()
        );
    }
}
