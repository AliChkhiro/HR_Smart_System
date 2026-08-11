package com.apprh.backend.employees.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotNull Long userId,
        Long departmentId,
        @NotNull @Size(max = 100) String jobTitle,
        @NotNull LocalDate hireDate
) {
}
