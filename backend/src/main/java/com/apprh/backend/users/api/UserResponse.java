package com.apprh.backend.users.api;

import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        UserStatus status,
        Instant createdAt
) {
}
