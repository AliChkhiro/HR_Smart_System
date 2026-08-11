package com.apprh.backend.leaves.api;

import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.domain.LeaveType;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record LeaveResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        LeaveStatus status,
        String reason,
        Long reviewerId,
        String reviewerName,
        Instant reviewDate,
        String reviewComment,
        Instant createdAt
) {
}
