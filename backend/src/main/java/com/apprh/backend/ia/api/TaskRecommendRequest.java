package com.apprh.backend.ia.api;

import com.apprh.backend.tasks.domain.TaskPriority;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TaskRecommendRequest(
        @Size(max = 20) List<Long> skillIds,
        LocalDate startDate,
        LocalDate dueDate,
        TaskPriority priority
) {
}