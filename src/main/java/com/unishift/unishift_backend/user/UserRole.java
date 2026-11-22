package com.unishift.unishift_backend.user;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserRole {
    STUDENT,
    EMPLOYER,
    ADMIN;

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}

