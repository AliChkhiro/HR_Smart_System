package com.apprh.backend.leaves.api;

import com.apprh.backend.leaves.domain.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveReviewRequest(
        @NotNull LeaveStatus status,
        @Size(max = 500) String comment
) {
}
