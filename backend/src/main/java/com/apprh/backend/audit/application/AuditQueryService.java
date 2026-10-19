package com.apprh.backend.audit.application;

import com.apprh.backend.audit.api.AuditLogResponse;
import com.apprh.backend.audit.domain.AuditLog;
import com.apprh.backend.audit.infrastructure.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultation du journal d'audit (lecture seule, ADMIN).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository repository;

    public Page<AuditLogResponse> list(String entityType, String entityId, Long actorUserId, Pageable pageable) {
        Page<AuditLog> page;
        if (entityType != null && entityId != null) {
            page = repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
        } else if (actorUserId != null) {
            page = repository.findByActorUserIdOrderByCreatedAtDesc(actorUserId, pageable);
        } else {
            page = repository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
