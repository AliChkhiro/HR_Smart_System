package com.apprh.backend.leaves.api;

import com.apprh.backend.leaves.domain.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestRecord(
        @NotNull LeaveType type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 500) String reason
) {
}
