package com.apprh.backend.dashboard.application;

import com.apprh.backend.dashboard.api.DashboardStatsResponse;
import com.apprh.backend.dashboard.api.TaskSummary;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.notifications.infrastructure.NotificationRepository;
import com.apprh.backend.projects.infrastructure.ProjectRepository;
import com.apprh.backend.tasks.domain.Task;
import com.apprh.backend.tasks.domain.TaskStatus;
import com.apprh.backend.tasks.infrastructure.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int UPCOMING_DAYS = 30;

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats(Long currentUserId) {
        LocalDate today = LocalDate.now();

        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status.name(), 0L);
        }
        for (Object[] row : taskRepository.countByStatus()) {
            tasksByStatus.put(((TaskStatus) row[0]).name(), (Long) row[1]);
        }

        List<Task> overdue = taskRepository.findOverdue(TaskStatus.DONE, today);
        List<Task> upcoming = taskRepository.findDueBetween(TaskStatus.DONE, today, today.plusDays(UPCOMING_DAYS));

        return DashboardStatsResponse.builder()
                .employeeCount(employeeRepository.countByDeletedAtIsNull())
                .projectCount(projectRepository.countByDeletedAtIsNull())
                .tasksByStatus(tasksByStatus)
                .overdueTaskCount(overdue.size())
                .upcomingTaskCount(upcoming.size())
                .latestOverdueTask(overdue.isEmpty() ? null : toSummary(overdue.get(0)))
                .nextUpcomingTask(upcoming.isEmpty() ? null : toSummary(upcoming.get(0)))
                .pendingLeaveCount(leaveRequestRepository.countByStatusAndDeletedAtIsNull(LeaveStatus.PENDING))
                .unreadNotificationCount(notificationRepository
                        .countByUserIdAndReadFalseAndDeletedAtIsNull(currentUserId))
                .build();
    }

    private TaskSummary toSummary(Task task) {
        return TaskSummary.builder()
                .id(task.getId())
                .name(task.getName())
                .projectName(task.getProject() == null ? null : task.getProject().getName())
                .assigneeName(task.getAssignee() == null ? null
                        : task.getAssignee().getUser().getFirstName() + " " + task.getAssignee().getUser().getLastName())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .build();
    }
}
