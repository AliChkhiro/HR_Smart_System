package com.apprh.backend.notifications.domain;

public enum NotificationType {
    LEAVE_REQUEST,
    LEAVE_REVIEW,
    TASK_ASSIGNED,
    /** Nouvelle demande d'inscription à valider par les RH. */
    USER_REGISTRATION,
    /** Demande d'inscription validée. */
    USER_APPROVED,
    /** Demande d'inscription refusée. */
    USER_REJECTED,
    /** Nouvelle demande d'effacement de compte à valider par un administrateur. */
    ERASURE_REQUESTED,
    /** Décision rendue sur une demande d'effacement de compte. */
    ERASURE_DECIDED
}
