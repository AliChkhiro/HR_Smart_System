package com.apprh.backend.tasks.api;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.api.RecommendationDto;
import com.apprh.backend.ia.api.TaskRecommendRequest;
import com.apprh.backend.ia.application.RecommendationService;
import com.apprh.backend.tasks.application.TaskService;
import com.apprh.backend.tasks.domain.TaskPriority;
import com.apprh.backend.tasks.domain.TaskStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final RecommendationService recommendationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<TaskResponse> list(@RequestParam(required = false) String search,
                                   @RequestParam(required = false) Long projectId,
                                   @RequestParam(required = false) Long assigneeId,
                                   @RequestParam(required = false) TaskStatus status,
                                   @RequestParam(required = false) TaskPriority priority,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return taskService.list(search, projectId, assigneeId, status, priority, dueFrom, dueTo,
                PageRequest.of(page, Math.min(size, 100)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request);
    }

    @PostMapping("/recommend")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    public List<RecommendationDto> recommend(@Valid @RequestBody TaskRecommendRequest request) {
        return recommendationService.recommend(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request,
                                     Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        boolean manager = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_RH")
                        || authority.getAuthority().equals("ROLE_CHEF_PROJET"));
        return taskService.updateStatus(id, request.status(), principal.id(), manager);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
