package com.apprh.backend.rgpd.api;

import jakarta.validation.constraints.Size;

/**
 * Demande d'effacement de compte deposee par l'utilisateur connecte.
 * La raison est facultative mais recommandee.
 */
public record ErasureRequestCreate(
        @Size(max = 500, message = "La raison ne peut pas dépasser 500 caractères")
        String reason
) {
}
