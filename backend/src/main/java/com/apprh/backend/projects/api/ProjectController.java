package com.apprh.backend.projects.api;

import com.apprh.backend.projects.application.ProjectService;
import com.apprh.backend.projects.domain.ProjectPriority;
import com.apprh.backend.projects.domain.ProjectStatus;
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
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ProjectResponse> list(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) ProjectStatus status,
                                      @RequestParam(required = false) ProjectPriority priority,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return projectService.list(search, status, priority, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProjectDetailResponse get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailResponse addMember(@PathVariable Long id, @Valid @RequestBody ProjectMemberRequest request) {
        return projectService.addMember(id, request);
    }

    @PatchMapping("/{id}/members/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    public ProjectDetailResponse updateMember(@PathVariable Long id, @PathVariable Long employeeId,
                                              @Valid @RequestBody ProjectMemberRequest request) {
        return projectService.updateMemberRole(id, employeeId, request);
    }

    @DeleteMapping("/{id}/members/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    public ProjectDetailResponse removeMember(@PathVariable Long id, @PathVariable Long employeeId) {
        return projectService.removeMember(id, employeeId);
    }
}
