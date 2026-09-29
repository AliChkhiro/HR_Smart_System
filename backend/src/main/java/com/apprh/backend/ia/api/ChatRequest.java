package com.apprh.backend.ia.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Le message ne peut pas etre vide")
        @Size(max = 2000, message = "Le message ne peut pas depasser 2000 caracteres")
        String message
) {
}
