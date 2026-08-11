package com.apprh.backend.notifications.api;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.notifications.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return notificationService.list(principal.id(), PageRequest.of(page, Math.min(size, 50)))
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .message(n.getMessage())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public long unreadCount(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return notificationService.unreadCount(principal.id());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public void markRead(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        notificationService.markRead(principal.id(), id);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public void markAllRead(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        notificationService.markAllRead(principal.id());
    }
}
