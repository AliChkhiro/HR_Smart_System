package com.apprh.backend.users.api;

import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        UserRole role,
        UserStatus status
) {
}
