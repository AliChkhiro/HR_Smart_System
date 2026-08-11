package com.apprh.backend.departments.api;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String name,
        String description,
        Long managerId,
        String managerName,
        long employeeCount,
        Instant createdAt
) {
}
