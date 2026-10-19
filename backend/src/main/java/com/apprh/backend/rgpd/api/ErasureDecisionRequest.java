package com.apprh.backend.rgpd.api;

import jakarta.validation.constraints.Size;

/**
 * Decision administrateur sur une demande d'effacement. Le commentaire est
 * facultatif (utile en cas de refus).
 */
public record ErasureDecisionRequest(
        @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
        String comment
) {
}
