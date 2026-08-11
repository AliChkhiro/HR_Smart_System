package com.apprh.backend.notifications.api;

import com.apprh.backend.notifications.domain.NotificationType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        Instant createdAt
) {
}
