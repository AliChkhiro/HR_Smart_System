package com.apprh.backend.tasks.api;

import com.apprh.backend.tasks.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(
        @NotNull TaskStatus status
) {
}
