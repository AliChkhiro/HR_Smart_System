package com.apprh.backend.employees.api;

import com.apprh.backend.employees.domain.EmployeeStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeUpdateRequest(
        Long departmentId,
        @Size(max = 100) String jobTitle,
        LocalDate hireDate,
        EmployeeStatus status
) {
}
