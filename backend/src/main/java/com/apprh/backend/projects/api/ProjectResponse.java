package com.apprh.backend.projects.api;

import com.apprh.backend.projects.domain.ProjectPriority;
import com.apprh.backend.projects.domain.ProjectStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus status,
        ProjectPriority priority,
        long memberCount,
        Instant createdAt
) {
}
