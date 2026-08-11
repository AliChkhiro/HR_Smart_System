package com.apprh.backend.projects.api;

import com.apprh.backend.projects.domain.ProjectPriority;
import com.apprh.backend.projects.domain.ProjectStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectUpdateRequest(
        @Size(max = 200) String name,
        @Size(max = 4000) String description,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus status,
        ProjectPriority priority
) {
}
