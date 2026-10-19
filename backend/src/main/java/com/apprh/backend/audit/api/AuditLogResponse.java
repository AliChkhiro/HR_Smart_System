package com.apprh.backend.audit.api;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorEmail,
        String action,
        String entityType,
        String entityId,
        String detail,
        Instant createdAt
) {
}
