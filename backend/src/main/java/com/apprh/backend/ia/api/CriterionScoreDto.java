package com.apprh.backend.ia.api;

public record CriterionScoreDto(
        String name,
        double score,
        String explanation
) {
}