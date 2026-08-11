package com.apprh.backend.employees.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EmployeeSkillRequest(
        @NotNull Long skillId,
        @NotNull @Min(1) @Max(5) int level
) {
}
