package com.apprh.backend.ia.application;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.ia.infrastructure.ChatbotClient;
import com.apprh.backend.leaves.domain.LeaveRequest;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.projects.domain.ProjectMember;
import com.apprh.backend.projects.infrastructure.ProjectMemberRepository;
import com.apprh.backend.tasks.domain.Task;
import com.apprh.backend.tasks.domain.TaskStatus;
import com.apprh.backend.tasks.infrastructure.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Construit le contexte RH de l'utilisateur authentifié (identité issue du JWT).
 * Données strictement limitées au profil de l'utilisateur courant : le LLM ne
 * décide jamais des permissions, aucune donnée d'un autre employé n'est exposée.
 */
@Service
@RequiredArgsConstructor
public class ChatContextService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional(readOnly = true)
    public ChatbotClient.ChatContext build(UserPrincipal principal) {
        Optional<Employee> maybeEmployee = employeeRepository.findByUserIdAndDeletedAtIsNull(principal.id());
        if (maybeEmployee.isEmpty()) {
            return new ChatbotClient.ChatContext(
                    principal.firstName(), principal.lastName(), principal.role().name(),
                    null, null, null,
                    "Aucun profil employé n'est associé à ce compte.",
                    List.of(), List.of(), List.of());
        }
        Employee employee = maybeEmployee.get();
        Long employeeId = employee.getId();
        return new ChatbotClient.ChatContext(
                principal.firstName(), principal.lastName(), principal.role().name(),
                employee.getJobTitle(),
                employee.getDepartment() == null ? null : employee.getDepartment().getName(),
                employee.getHireDate() == null ? null : employee.getHireDate().format(DATE_FORMAT),
                leaveSummary(employeeId),
                recentLeaves(employeeId),
                inProgressTasks(employeeId),
                projects(employeeId));
    }

    private String leaveSummary(Long employeeId) {
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdAndDeletedAtIsNull(employeeId);
        long approved = leaves.stream().filter(leave -> leave.getStatus() == LeaveStatus.APPROVED).count();
        long pending = leaves.stream().filter(leave -> leave.getStatus() == LeaveStatus.PENDING).count();
        long rejected = leaves.stream().filter(leave -> leave.getStatus() == LeaveStatus.REJECTED).count();
        long approvedDays = leaves.stream()
                .filter(leave -> leave.getStatus() == LeaveStatus.APPROVED)
                .mapToLong(this::inclusiveDays)
                .sum();
        return "approuvés: %d (%d jours), en attente: %d, refusés: %d".formatted(approved, approvedDays, pending, rejected);
    }

    private List<String> recentLeaves(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdAndDeletedAtIsNull(employeeId).stream()
                .sorted(Comparator.comparing(LeaveRequest::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .map(leave -> "%s du %s au %s - %s".formatted(
                        leave.getType(),
                        leave.getStartDate().format(DATE_FORMAT),
                        leave.getEndDate().format(DATE_FORMAT),
                        leave.getStatus()))
                .toList();
    }

    private List<String> inProgressTasks(Long employeeId) {
        return taskRepository
                .findTop10ByAssigneeIdAndDeletedAtIsNullAndStatusNotOrderByDueDateAsc(employeeId, TaskStatus.DONE)
                .stream()
                .map(this::describeTask)
                .toList();
    }

    private String describeTask(Task task) {
        String due = task.getDueDate() == null ? "sans échéance" : "échéance " + task.getDueDate().format(DATE_FORMAT);
        String project = task.getProject() == null ? "" : " (projet " + task.getProject().getName() + ")";
        return "%s [%s, %s] %s%s".formatted(task.getName(), task.getStatus(), task.getPriority(), due, project);
    }

    private List<String> projects(Long employeeId) {
        return projectMemberRepository.findAllByEmployeeId(employeeId).stream()
                .map(ProjectMember::getProject)
                .filter(project -> project.getDeletedAt() == null)
                .limit(10)
                .map(project -> "%s [%s]".formatted(project.getName(), project.getStatus()))
                .toList();
    }

    private long inclusiveDays(LeaveRequest leave) {
        if (leave.getStartDate() == null || leave.getEndDate() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
    }
}
