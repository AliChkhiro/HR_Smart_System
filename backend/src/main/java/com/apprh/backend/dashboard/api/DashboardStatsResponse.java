package com.apprh.backend.dashboard.api;

import com.apprh.backend.tasks.domain.TaskPriority;
import com.apprh.backend.tasks.domain.TaskStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Map;

@Builder
public record DashboardStatsResponse(
        long employeeCount,
        long projectCount,
        Map<String, Long> tasksByStatus,
        long overdueTaskCount,
        long upcomingTaskCount,
        TaskSummary latestOverdueTask,
        TaskSummary nextUpcomingTask,
        long pendingLeaveCount,
        long unreadNotificationCount
) {
}