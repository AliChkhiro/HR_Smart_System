package com.apprh.backend.employees.api;

import com.apprh.backend.employees.domain.EmployeeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EmployeeResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String jobTitle,
        LocalDate hireDate,
        Long departmentId,
        String departmentName,
        EmployeeStatus status,
        List<EmployeeSkillResponse> skills,
        Instant createdAt
) {
}
