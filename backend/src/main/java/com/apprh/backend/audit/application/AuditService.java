package com.apprh.backend.audit.application;

import com.apprh.backend.audit.domain.AuditLog;
import com.apprh.backend.audit.infrastructure.AuditLogRepository;
import com.apprh.backend.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Journal d'audit : ecriture explicite des seules actions critiques.
 *
 * Deux modes d'ecriture :
 *  - {@link #record(AuditAction, String, Object, String)} : l'auteur est resolu
 *    depuis le contexte de securite (cas des operations authentifiees) ;
 *  - {@link #record(Long, String, AuditAction, String, Object, String)} : auteur
 *    explicite (cas des connexions, ou aucun contexte n'existe encore).
 *
 * L'ecriture est toujours NON bloquante : une erreur de journalisation ne doit
 * jamais faire echouer l'operation metier qui l'a declenchee.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private static final int DETAIL_MAX_LENGTH = 1000;

    private final AuditLogRepository repository;

    /**
     * Journalise une action en resolvant l'auteur depuis le contexte de securite.
     * Ne leve jamais d'exception.
     */
    public void record(AuditAction action, String entityType, Object entityId, String detail) {
        Long actorUserId = null;
        String actorEmail = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            actorUserId = principal.id();
            actorEmail = principal.email();
        }
        record(actorUserId, actorEmail, action, entityType, entityId, detail);
    }

    /**
     * Journalise une action avec un auteur explicite. Ne leve jamais d'exception.
     *
     * @param actorUserId identifiant de l'auteur, ou null pour un acteur systeme
     * @param actorEmail  email de l'auteur, preserve meme si le compte est supprime
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorUserId, String actorEmail,
                       AuditAction action, String entityType, Object entityId, String detail) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorUserId(actorUserId)
                    .actorEmail(actorEmail)
                    .action(action.value())
                    .entityType(entityType)
                    .entityId(entityId == null ? null : String.valueOf(entityId))
                    .detail(truncate(detail))
                    .build();
            repository.save(entry);
        } catch (Exception ex) {
            // Une erreur d'audit ne doit pas casser l'operation metier.
            log.warn("Échec d'écriture de l'audit (action={}, entity={}): {}",
                    action, entityType, ex.getMessage());
        }
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= DETAIL_MAX_LENGTH ? detail : detail.substring(0, DETAIL_MAX_LENGTH);
    }
}
