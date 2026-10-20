package com.apprh.backend.rgpd.domain;

/**
 * Etats d'une demande d'effacement de compte (droit a l'effacement RGPD).
 */
public enum ErasureStatus {
    /** Demande deposee par l'utilisateur, en attente de decision. */
    PENDING,
    /** Demande validee : le compte a ete supprime (suppression douce). */
    APPROVED,
    /** Demande refusee par un administrateur. */
    REJECTED
}
