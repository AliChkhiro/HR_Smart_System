package com.apprh.backend.ia.application;

import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.ia.api.CriterionScoreDto;
import com.apprh.backend.ia.api.RecommendationDto;
import com.apprh.backend.ia.api.TaskRecommendRequest;
import com.apprh.backend.ia.infrastructure.IaClient;
import com.apprh.backend.leaves.domain.LeaveRequest;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.skills.domain.EmployeeSkill;
import com.apprh.backend.skills.infrastructure.EmployeeSkillRepository;
import com.apprh.backend.tasks.domain.TaskStatus;
import com.apprh.backend.tasks.infrastructure.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final TaskRepository taskRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final IaClient iaClient;
    private final LocalScoringEngine localScoringEngine;

    @Transactional(readOnly = true)
    public List<RecommendationDto> recommend(TaskRecommendRequest request) {
        List<Employee> employees = employeeRepository.findAllByDeletedAtIsNull();
        Map<Long, LocalScoringEngine.EmployeeProfileData> profiles = buildProfiles(employees);
        try {
            List<IaClient.RemoteRecommendation> remote = iaClient.recommend(toRemoteRequest(request, profiles));
            return enrich(remote, profiles);
        } catch (Exception e) {
            log.warn("ia-service indisponible ({}) — scoring local de secours", e.getMessage());
            return localScoringEngine.recommend(request, profiles);
        }
    }

    private Map<Long, LocalScoringEngine.EmployeeProfileData> buildProfiles(List<Employee> employees) {
        List<Long> ids = employees.stream().map(Employee::getId).toList();

        Map<Long, List<Long>> skills = new HashMap<>();
        for (EmployeeSkill es : employeeSkillRepository.findAllByEmployeeIdIn(ids)) {
            skills.computeIfAbsent(es.getEmployee().getId(), k -> new ArrayList<>())
                    .add(es.getSkill().getId());
        }

        Map<Long, Map<TaskStatus, Long>> taskCounts = new HashMap<>();
        for (Object[] row : taskRepository.countGroupedByAssignee(ids)) {
            Long employeeId = (Long) row[0];
            TaskStatus status = (TaskStatus) row[1];
            Long count = (Long) row[2];
            taskCounts.computeIfAbsent(employeeId, k -> new HashMap<>()).put(status, count);
        }

        Map<Long, List<LeaveRequest>> leaves = leaveRequestRepository
                .findAllByEmployeeIdInAndStatusInAndDeletedAtIsNull(ids, List.of(LeaveStatus.APPROVED))
                .stream()
                .collect(Collectors.groupingBy(l -> l.getEmployee().getId()));

        Map<Long, LocalScoringEngine.EmployeeProfileData> profiles = new HashMap<>();
        for (Employee employee : employees) {
            Map<TaskStatus, Long> counts = taskCounts.getOrDefault(employee.getId(), Map.of());
            long open = counts.entrySet().stream()
                    .filter(e -> e.getKey() != TaskStatus.DONE)
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            long done = counts.getOrDefault(TaskStatus.DONE, 0L);
            double reliability = done + open == 0 ? 1.0 : (double) done / (done + open);
            profiles.put(employee.getId(), new LocalScoringEngine.EmployeeProfileData(
                    employee.getUser().getFirstName() + " " + employee.getUser().getLastName(),
                    skills.getOrDefault(employee.getId(), List.of()),
                    open,
                    leaves.getOrDefault(employee.getId(), List.of()),
                    reliability));
        }
        return profiles;
    }

    private IaClient.RemoteRequest toRemoteRequest(TaskRecommendRequest request,
                                                   Map<Long, LocalScoringEngine.EmployeeProfileData> profiles) {
        List<IaClient.EmployeeProfile> remoteEmployees = profiles.entrySet().stream()
                .map(e -> new IaClient.EmployeeProfile(
                        e.getKey(),
                        e.getValue().skillIds(),
                        (int) e.getValue().ongoingTaskCount(),
                        e.getValue().leavePeriods().stream()
                                .map(leave -> new IaClient.LeavePeriod(
                                        leave.getStartDate().toString(), leave.getEndDate().toString()))
                                .toList(),
                        e.getValue().reliability()))
                .toList();
        IaClient.TaskProfile task = new IaClient.TaskProfile(
                0,
                request.skillIds() == null ? List.of() : request.skillIds(),
                request.startDate() == null ? null : request.startDate().toString(),
                request.dueDate() == null ? null : request.dueDate().toString(),
                request.priority() == null ? 2 : switch (request.priority()) {
                    case LOW -> 1;
                    case MEDIUM -> 2;
                    case HIGH -> 3;
                    case URGENT -> 4;
                });
        return new IaClient.RemoteRequest(task, remoteEmployees);
    }

    private List<RecommendationDto> enrich(List<IaClient.RemoteRecommendation> remote,
                                           Map<Long, LocalScoringEngine.EmployeeProfileData> profiles) {
        return remote.stream().map(r -> {
            String name = "?";
            LocalScoringEngine.EmployeeProfileData profile = profiles.get(r.employee_id());
            if (profile != null) {
                name = profile.employeeName();
            }
            return new RecommendationDto(
                    r.employee_id(),
                    name,
                    r.total_score(),
                    r.criteria().stream()
                            .map(c -> new CriterionScoreDto(c.name(), c.score(), c.explanation()))
                            .toList(),
                    r.explanation());
        }).toList();
    }
}