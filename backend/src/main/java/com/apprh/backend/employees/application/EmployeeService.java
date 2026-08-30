package com.apprh.backend.employees.application;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.departments.domain.Department;
import com.apprh.backend.departments.infrastructure.DepartmentRepository;
import com.apprh.backend.employees.api.EmployeeCreateRequest;
import com.apprh.backend.employees.api.EmployeeResponse;
import com.apprh.backend.employees.api.EmployeeSkillRequest;
import com.apprh.backend.employees.api.EmployeeSkillResponse;
import com.apprh.backend.employees.api.EmployeeUpdateRequest;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.domain.EmployeeStatus;
import com.apprh.backend.employees.infrastructure.EmployeeMapper;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.employees.infrastructure.EmployeeSpecifications;
import com.apprh.backend.skills.domain.EmployeeSkill;
import com.apprh.backend.skills.domain.Skill;
import com.apprh.backend.skills.infrastructure.EmployeeSkillRepository;
import com.apprh.backend.skills.infrastructure.SkillRepository;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(String search, Long departmentId, EmployeeStatus status, Pageable pageable) {
        Page<Employee> page = employeeRepository.findAll(
                EmployeeSpecifications.activeAndFiltered(search, departmentId, status), pageable);
        Map<Long, List<EmployeeSkill>> skillsByEmployee = employeeSkillRepository
                .findAllByEmployeeIdIn(page.map(Employee::getId).toList()).stream()
                .collect(Collectors.groupingBy(link -> link.getEmployee().getId()));
        return page.map(employee -> toResponseWithSkills(employee, skillsByEmployee.getOrDefault(employee.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        Employee employee = findActive(id);
        return toResponseWithSkills(employee, employeeSkillRepository.findAllByEmployeeId(id));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getByUser(Long userId) {
        Employee employee = employeeRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND",
                        "Aucun profil employé n'est associé à ce compte"));
        return toResponseWithSkills(employee, employeeSkillRepository.findAllByEmployeeId(employee.getId()));
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        if (employeeRepository.existsByUserIdAndDeletedAtIsNull(request.userId())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMPLOYEE_USER_EXISTS",
                    "Ce compte utilisateur est déjà rattaché à un employé");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "EMPLOYEE_USER_NOT_FOUND", "Utilisateur introuvable"));
        Employee employee = Employee.builder()
                .user(user)
                .department(findDepartment(request.departmentId()))
                .jobTitle(request.jobTitle().trim())
                .hireDate(request.hireDate())
                .status(EmployeeStatus.ACTIVE)
                .build();
        Employee saved = employeeRepository.save(employee);
        return toResponseWithSkills(saved, List.of());
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = findActive(id);
        if (request.departmentId() != null) {
            employee.setDepartment(findDepartment(request.departmentId()));
        }
        if (request.jobTitle() != null && !request.jobTitle().isBlank()) {
            employee.setJobTitle(request.jobTitle().trim());
        }
        if (request.hireDate() != null) {
            employee.setHireDate(request.hireDate());
        }
        if (request.status() != null) {
            employee.setStatus(request.status());
        }
        Employee saved = employeeRepository.save(employee);
        return toResponseWithSkills(saved, employeeSkillRepository.findAllByEmployeeId(id));
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findActive(id);
        employeeSkillRepository.deleteAllByEmployeeId(id);
        employee.setDeletedAt(Instant.now());
        employeeRepository.save(employee);
    }

    @Transactional
    public EmployeeResponse addSkill(Long employeeId, EmployeeSkillRequest request) {
        Employee employee = findActive(employeeId);
        Skill skill = skillRepository.findByIdAndDeletedAtIsNull(request.skillId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SKILL_NOT_FOUND", "Compétence introuvable"));
        if (employeeSkillRepository.existsByEmployeeIdAndSkillId(employeeId, skill.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMPLOYEE_SKILL_EXISTS",
                    "Cette compétence est déjà associée à l'employé");
        }
        EmployeeSkill link = EmployeeSkill.builder()
                .employee(employee)
                .skill(skill)
                .level(request.level())
                .build();
        employeeSkillRepository.save(link);
        return toResponseWithSkills(employee, employeeSkillRepository.findAllByEmployeeId(employeeId));
    }

    @Transactional
    public EmployeeResponse updateSkillLevel(Long employeeId, Long skillId, int level) {
        Employee employee = findActive(employeeId);
        EmployeeSkill link = employeeSkillRepository.findByEmployeeIdAndSkillId(employeeId, skillId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_SKILL_NOT_FOUND",
                        "Cette compétence n'est pas associée à l'employé"));
        link.setLevel(level);
        employeeSkillRepository.save(link);
        return toResponseWithSkills(employee, employeeSkillRepository.findAllByEmployeeId(employeeId));
    }

    @Transactional
    public EmployeeResponse removeSkill(Long employeeId, Long skillId) {
        Employee employee = findActive(employeeId);
        EmployeeSkill link = employeeSkillRepository.findByEmployeeIdAndSkillId(employeeId, skillId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_SKILL_NOT_FOUND",
                        "Cette compétence n'est pas associée à l'employé"));
        employeeSkillRepository.delete(link);
        return toResponseWithSkills(employee, employeeSkillRepository.findAllByEmployeeId(employeeId));
    }

    private Employee findActive(Long id) {
        return employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "Employé introuvable"));
    }

    private Department findDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .filter(department -> department.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "DEPARTMENT_NOT_FOUND", "Département introuvable"));
    }

    private EmployeeResponse toResponseWithSkills(Employee employee, List<EmployeeSkill> skills) {
        EmployeeResponse response = employeeMapper.toResponse(employee);
        List<EmployeeSkillResponse> skillResponses = skills.stream()
                .sorted(Comparator.comparing(link -> link.getSkill().getName()))
                .map(employeeMapper::toSkillResponse)
                .toList();
        return new EmployeeResponse(response.id(), response.userId(), response.firstName(), response.lastName(),
                response.email(), response.jobTitle(), response.hireDate(), response.departmentId(),
                response.departmentName(), response.status(), skillResponses, response.createdAt());
    }
}
