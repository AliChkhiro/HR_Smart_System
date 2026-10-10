package com.apprh.backend.ia.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank(message = "Le message ne peut pas être vide")
        @Size(max = 2000, message = "Le message ne peut pas dépasser 2000 caractères")
        String message,

        @Size(max = 12, message = "L'historique est limité à 12 messages")
        List<@Valid HistoryMessage> history
) {
    public record HistoryMessage(
            @NotBlank(message = "Le rôle est obligatoire")
            @Pattern(regexp = "user|assistant", message = "Rôle d'historique invalide")
            String role,

            @NotBlank(message = "Le contenu d'historique ne peut pas être vide")
            @Size(max = 4000, message = "Un message d'historique ne peut pas dépasser 4000 caractères")
            String content
    ) {
    }
}
