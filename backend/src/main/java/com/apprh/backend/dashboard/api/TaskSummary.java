package com.apprh.backend.dashboard.api;

import com.apprh.backend.tasks.domain.TaskPriority;
import com.apprh.backend.tasks.domain.TaskStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record TaskSummary(
        Long id,
        String name,
        String projectName,
        String assigneeName,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate
) {
}