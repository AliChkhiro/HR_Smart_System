package com.apprh.backend.leaves.api;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.leaves.application.LeaveService;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.domain.LeaveType;
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

@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<LeaveResponse> list(@RequestParam(required = false) Long employeeId,
                                    @RequestParam(required = false) LeaveStatus status,
                                    @RequestParam(required = false) LeaveType type,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return leaveService.list(principal.id(), isManager(authentication), employeeId, status, type, from, to,
                PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public LeaveResponse get(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return leaveService.get(id, principal.id(), isManager(authentication));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(@Valid @RequestBody LeaveRequestRecord request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return leaveService.create(request, principal.id());
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'CHEF_PROJET')")
    public LeaveResponse review(@PathVariable Long id, @Valid @RequestBody LeaveReviewRequest request,
                                Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return leaveService.review(id, request, principal.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        leaveService.cancel(id, principal.id(), isManager(authentication));
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_RH")
                        || authority.getAuthority().equals("ROLE_CHEF_PROJET"));
    }
}
