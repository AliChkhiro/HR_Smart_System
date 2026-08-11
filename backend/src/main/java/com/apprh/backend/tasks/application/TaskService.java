package com.apprh.backend.tasks.application;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.projects.domain.Project;
import com.apprh.backend.projects.infrastructure.ProjectRepository;
import com.apprh.backend.tasks.api.TaskRequest;
import com.apprh.backend.tasks.api.TaskResponse;
import com.apprh.backend.tasks.api.TaskUpdateRequest;
import com.apprh.backend.tasks.domain.Task;
import com.apprh.backend.tasks.domain.TaskPriority;
import com.apprh.backend.tasks.domain.TaskStatus;
import com.apprh.backend.tasks.infrastructure.TaskMapper;
import com.apprh.backend.tasks.infrastructure.TaskRepository;
import com.apprh.backend.tasks.infrastructure.TaskSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(String search, Long projectId, Long assigneeId, TaskStatus status,
                                   TaskPriority priority, LocalDate dueFrom, LocalDate dueTo, Pageable pageable) {
        return taskRepository.findAll(TaskSpecifications.activeAndFiltered(
                        search, projectId, assigneeId, status, priority, dueFrom, dueTo), pageable)
                .map(taskMapper::toResponse);
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = Task.builder()
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .project(findProject(request.projectId()))
                .assignee(findAssignee(request.assigneeId()))
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .estimatedHours(normalizeHours(request.estimatedHours()))
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .build();
        validateDates(task.getStartDate(), task.getDueDate());
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        Task task = findActive(id);
        if (request.name() != null && !request.name().isBlank()) {
            task.setName(request.name().trim());
        }
        if (request.description() != null) {
            task.setDescription(trimToNull(request.description()));
        }
        if (request.projectId() != null) {
            task.setProject(findProject(request.projectId()));
        }
        if (request.assigneeId() != null) {
            task.setAssignee(findAssignee(request.assigneeId()));
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.estimatedHours() != null) {
            task.setEstimatedHours(normalizeHours(request.estimatedHours()));
        }
        if (request.startDate() != null) {
            task.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        validateDates(task.getStartDate(), task.getDueDate());
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status, Long currentUserId, boolean manager) {
        Task task = findActive(id);
        if (!manager && task.getAssignee() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TASK_NOT_ASSIGNED",
                    "Seul un responsable peut déplacer une tâche non assignée");
        }
        if (!manager && task.getAssignee() != null
                && !currentUserId.equals(task.getAssignee().getUser().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TASK_NOT_YOURS",
                    "Vous ne pouvez déplacer que vos propres tâches");
        }
        task.setStatus(status);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = findActive(id);
        task.setDeletedAt(Instant.now());
        taskRepository.save(task);
    }

    private Task findActive(Long id) {
        return taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Tâche introuvable"));
    }

    private Project findProject(Long projectId) {
        if (projectId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROJECT_REQUIRED", "Le projet est obligatoire");
        }
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROJECT_NOT_FOUND", "Projet introuvable"));
    }

    private Employee findAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return employeeRepository.findByIdAndDeletedAtIsNull(assigneeId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "ASSIGNEE_NOT_FOUND", "Assigné introuvable"));
    }

    private void validateDates(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TASK_INVALID_DATES",
                    "La date d'échéance doit être postérieure à la date de début");
        }
    }

    private BigDecimal normalizeHours(BigDecimal hours) {
        return hours != null && hours.signum() == 0 ? null : hours;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
