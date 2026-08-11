package com.apprh.backend.projects.api;

import com.apprh.backend.projects.domain.ProjectMemberRole;

public record ProjectMemberResponse(
        Long employeeId,
        String employeeName,
        String jobTitle,
        ProjectMemberRole role
) {
}
