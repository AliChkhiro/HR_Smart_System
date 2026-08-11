package com.apprh.backend.projects.api;

import com.apprh.backend.projects.domain.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRequest(
        @NotNull Long employeeId,
        @NotNull ProjectMemberRole role
) {
}
