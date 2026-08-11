package com.apprh.backend.projects.application;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.projects.api.ProjectDetailResponse;
import com.apprh.backend.projects.api.ProjectMemberRequest;
import com.apprh.backend.projects.api.ProjectMemberResponse;
import com.apprh.backend.projects.api.ProjectRequest;
import com.apprh.backend.projects.api.ProjectResponse;
import com.apprh.backend.projects.api.ProjectUpdateRequest;
import com.apprh.backend.projects.domain.Project;
import com.apprh.backend.projects.domain.ProjectMember;
import com.apprh.backend.projects.domain.ProjectPriority;
import com.apprh.backend.projects.domain.ProjectStatus;
import com.apprh.backend.projects.infrastructure.ProjectMapper;
import com.apprh.backend.projects.infrastructure.ProjectMemberRepository;
import com.apprh.backend.projects.infrastructure.ProjectRepository;
import com.apprh.backend.projects.infrastructure.ProjectSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberRepository projectMemberRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(String search, ProjectStatus status, ProjectPriority priority, Pageable pageable) {
        Page<Project> page = projectRepository.findAll(
                ProjectSpecifications.activeAndFiltered(search, status, priority), pageable);
        Map<Long, Long> counts = projectMemberRepository.findAllByProjectIdIn(page.map(Project::getId).toList())
                .stream().collect(Collectors.groupingBy(member -> member.getProject().getId(), Collectors.counting()));
        return page.map(project -> {
            ProjectResponse response = projectMapper.toResponse(project);
            return new ProjectResponse(response.id(), response.name(), response.description(),
                    response.startDate(), response.endDate(), response.status(), response.priority(),
                    counts.getOrDefault(project.getId(), 0L), response.createdAt());
        });
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse get(Long id) {
        Project project = findActive(id);
        List<ProjectMemberResponse> members = projectMemberRepository.findAllByProjectId(id).stream()
                .sorted((a, b) -> a.getEmployee().getUser().getLastName().compareToIgnoreCase(
                        b.getEmployee().getUser().getLastName()))
                .map(projectMapper::toMemberResponse)
                .toList();
        return new ProjectDetailResponse(project.getId(), project.getName(), project.getDescription(),
                project.getStartDate(), project.getEndDate(), project.getStatus(), project.getPriority(),
                members, project.getCreatedAt());
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        String name = request.name().trim();
        checkNameAvailable(name);
        validateDates(request.startDate(), request.endDate());
        Project project = Project.builder()
                .name(name)
                .description(trimToNull(request.description()))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status() != null ? request.status() : ProjectStatus.PLANNED)
                .priority(request.priority() != null ? request.priority() : ProjectPriority.MEDIUM)
                .build();
        Project saved = projectRepository.save(project);
        ProjectResponse response = projectMapper.toResponse(saved);
        return new ProjectResponse(response.id(), response.name(), response.description(), response.startDate(),
                response.endDate(), response.status(), response.priority(), 0L, response.createdAt());
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        Project project = findActive(id);
        if (request.name() != null && !request.name().isBlank() && !request.name().trim().equalsIgnoreCase(project.getName())) {
            checkNameAvailable(request.name().trim());
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(trimToNull(request.description()));
        }
        LocalDate startDate = request.startDate() != null ? request.startDate() : project.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : project.getEndDate();
        if (request.startDate() != null || request.endDate() != null) {
            validateDates(startDate, endDate);
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            project.setEndDate(request.endDate());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        if (request.priority() != null) {
            project.setPriority(request.priority());
        }
        Project saved = projectRepository.save(project);
        return toResponseWithCount(saved, projectMemberRepository.countByProjectId(saved.getId()));
    }

    @Transactional
    public void delete(Long id) {
        Project project = findActive(id);
        projectMemberRepository.deleteAllByProjectId(id);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    @Transactional
    public ProjectDetailResponse addMember(Long projectId, ProjectMemberRequest request) {
        Project project = findActive(projectId);
        if (projectMemberRepository.existsByProjectIdAndEmployeeId(projectId, request.employeeId())) {
            throw new ApiException(HttpStatus.CONFLICT, "PROJECT_MEMBER_EXISTS",
                    "Cet employé est déjà membre du projet");
        }
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(request.employeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "EMPLOYEE_NOT_FOUND", "Employé introuvable"));
        projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .employee(employee)
                .role(request.role())
                .build());
        return get(projectId);
    }

    @Transactional
    public ProjectDetailResponse updateMemberRole(Long projectId, Long employeeId, ProjectMemberRequest request) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndEmployeeId(projectId, employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_MEMBER_NOT_FOUND",
                        "Cet employé n'est pas membre du projet"));
        member.setRole(request.role());
        projectMemberRepository.save(member);
        return get(projectId);
    }

    @Transactional
    public ProjectDetailResponse removeMember(Long projectId, Long employeeId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndEmployeeId(projectId, employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_MEMBER_NOT_FOUND",
                        "Cet employé n'est pas membre du projet"));
        projectMemberRepository.delete(member);
        return get(projectId);
    }

    private Project findActive(Long id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Projet introuvable"));
    }

    private void checkNameAvailable(String name) {
        if (projectRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PROJECT_NAME_EXISTS", "Un projet porte déjà ce nom");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROJECT_INVALID_DATES",
                    "La date de fin doit être postérieure à la date de début");
        }
    }

    private ProjectResponse toResponseWithCount(Project project, long memberCount) {
        ProjectResponse response = projectMapper.toResponse(project);
        return new ProjectResponse(response.id(), response.name(), response.description(), response.startDate(),
                response.endDate(), response.status(), response.priority(), memberCount, response.createdAt());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
