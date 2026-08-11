package com.apprh.backend.employees.api;

import com.apprh.backend.employees.application.EmployeeService;
import com.apprh.backend.employees.domain.EmployeeStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<EmployeeResponse> list(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) Long departmentId,
                                       @RequestParam(required = false) EmployeeStatus status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return employeeService.list(search, departmentId, status, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }

    @PostMapping("/{id}/skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse addSkill(@PathVariable Long id, @Valid @RequestBody EmployeeSkillRequest request) {
        return employeeService.addSkill(id, request);
    }

    @PatchMapping("/{id}/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public EmployeeResponse updateSkill(@PathVariable Long id, @PathVariable Long skillId,
                                        @Valid @RequestBody EmployeeSkillRequest request) {
        return employeeService.updateSkillLevel(id, skillId, request.level());
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public EmployeeResponse removeSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return employeeService.removeSkill(id, skillId);
    }
}
