package com.apprh.backend.tasks.api;

import com.apprh.backend.tasks.domain.TaskPriority;
import com.apprh.backend.tasks.domain.TaskStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        @NotNull Long projectId,
        Long assigneeId,
        TaskStatus status,
        TaskPriority priority,
        @DecimalMin("0.25") BigDecimal estimatedHours,
        LocalDate startDate,
        LocalDate dueDate
) {
}
