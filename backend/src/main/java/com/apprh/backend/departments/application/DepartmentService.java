package com.apprh.backend.departments.application;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.departments.api.DepartmentRequest;
import com.apprh.backend.departments.api.DepartmentResponse;
import com.apprh.backend.departments.domain.Department;
import com.apprh.backend.departments.infrastructure.DepartmentMapper;
import com.apprh.backend.departments.infrastructure.DepartmentRepository;
import com.apprh.backend.departments.infrastructure.DepartmentSpecifications;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> list(String search, Pageable pageable) {
        Page<Department> page = departmentRepository.findAll(DepartmentSpecifications.activeAndFiltered(search), pageable);
        return page.map(department -> withEmployeeCount(departmentMapper.toResponse(department)));
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) {
        return withEmployeeCount(departmentMapper.toResponse(findActive(id)));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        checkNameAvailable(name, null);
        Department department = Department.builder()
                .name(name)
                .description(trimToNull(request.description()))
                .manager(findManager(request.managerId()))
                .build();
        return withEmployeeCount(departmentMapper.toResponse(departmentRepository.save(department)));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findActive(id);
        if (request.name() != null && !request.name().isBlank() && !request.name().trim().equalsIgnoreCase(department.getName())) {
            checkNameAvailable(request.name().trim(), id);
            department.setName(request.name().trim());
        }
        if (request.description() != null) {
            department.setDescription(trimToNull(request.description()));
        }
        department.setManager(findManager(request.managerId()));
        return withEmployeeCount(departmentMapper.toResponse(departmentRepository.save(department)));
    }

    @Transactional
    public void delete(Long id) {
        Department department = findActive(id);
        long assigned = employeeRepository.countByDepartmentIdAndDeletedAtIsNull(id);
        if (assigned > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "DEPARTMENT_HAS_EMPLOYEES",
                    "Ce département compte encore " + assigned + " employé(s) actif(s)");
        }
        department.setDeletedAt(Instant.now());
        departmentRepository.save(department);
    }

    private Department findActive(Long id) {
        return departmentRepository.findById(id)
                .filter(department -> department.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "Département introuvable"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void checkNameAvailable(String name, Long excludeId) {
        if (excludeId == null) {
            if (departmentRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
                throw new ApiException(HttpStatus.CONFLICT, "DEPARTMENT_NAME_EXISTS", "Un département porte déjà ce nom");
            }
            return;
        }
        Department existing = departmentRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name).orElse(null);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "DEPARTMENT_NAME_EXISTS", "Un département porte déjà ce nom");
        }
    }

    private User findManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return userRepository.findByIdAndDeletedAtIsNull(managerId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "MANAGER_NOT_FOUND", "Responsable introuvable"));
    }

    private DepartmentResponse withEmployeeCount(DepartmentResponse response) {
        Map<Long, Long> counts = employeeCounts(List.of(response.id()));
        return new DepartmentResponse(response.id(), response.name(), response.description(),
                response.managerId(), response.managerName(),
                counts.getOrDefault(response.id(), 0L), response.createdAt());
    }

    private Map<Long, Long> employeeCounts(List<Long> departmentIds) {
        return employeeRepository.countByDepartmentIdInAndDeletedAtIsNull(departmentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
