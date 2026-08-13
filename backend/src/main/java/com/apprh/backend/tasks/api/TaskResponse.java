package com.apprh.backend.tasks.api;

import com.apprh.backend.tasks.domain.TaskPriority;
import com.apprh.backend.tasks.domain.TaskStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaskResponse(
        Long id,
        String name,
        String description,
        Long projectId,
        String projectName,
        Long assigneeId,
        String assigneeName,
        List<Long> skillIds,
        TaskStatus status,
        TaskPriority priority,
        BigDecimal estimatedHours,
        LocalDate startDate,
        LocalDate dueDate,
        Instant createdAt
) {
}
