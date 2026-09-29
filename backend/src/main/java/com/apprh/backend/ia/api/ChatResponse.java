package com.apprh.backend.ia.api;

public record ChatResponse(
        String reply,
        boolean aiAvailable
) {
}
