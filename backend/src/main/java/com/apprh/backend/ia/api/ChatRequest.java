package com.apprh.backend.ia.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Le message ne peut pas être vide")
        @Size(max = 2000, message = "Le message ne peut pas dépasser 2000 caractères")
        String message
) {
}