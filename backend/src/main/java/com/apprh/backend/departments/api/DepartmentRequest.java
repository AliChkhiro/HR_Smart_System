package com.apprh.backend.departments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        Long managerId
) {
}
