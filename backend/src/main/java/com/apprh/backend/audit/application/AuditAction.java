package com.apprh.backend.audit.application;

/**
 * Actions sensibles journalisees dans l'audit. Vocabulary volontairement
 * restreint aux operations a forte valeur de conformite.
 */
public enum AuditAction {

    /** Authentification reussie. */
    LOGIN_SUCCESS("LOGIN_SUCCESS"),
    /** Echec d'authentification (mauvais identifiants). */
    LOGIN_FAILURE("LOGIN_FAILURE"),
    /** Creation d'une entite. */
    ENTITY_CREATED("ENTITY_CREATED"),
    /** Modification d'une entite. */
    ENTITY_UPDATED("ENTITY_UPDATED"),
    /** Suppression douce d'une entite. */
    ENTITY_DELETED("ENTITY_DELETED"),
    /** Approbation d'un compte en attente. */
    USER_APPROVED("USER_APPROVED"),
    /** Rejet d'un compte en attente. */
    USER_REJECTED("USER_REJECTED"),
    /** Approbation d'une demande de congés. */
    LEAVE_APPROVED("LEAVE_APPROVED"),
    /** Rejet d'une demande de congés. */
    LEAVE_REJECTED("LEAVE_REJECTED"),
    /** Annulation d'une demande de congés. */
    LEAVE_CANCELLED("LEAVE_CANCELLED"),
    /** Export RGPD des donnees personnelles. */
    RGPD_EXPORTED("RGPD_EXPORTED"),
    /** Demande d'effacement de compte de l'utilisateur. */
    RGPD_ERASURE_REQUESTED("RGPD_ERASURE_REQUESTED"),
    /** Validation administrateur d'une demande d'effacement. */
    RGPD_ERASURE_APPROVED("RGPD_ERASURE_APPROVED"),
    /** Rejet d'une demande d'effacement. */
    RGPD_ERASURE_REJECTED("RGPD_ERASURE_REJECTED");

    private final String value;

    AuditAction(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
