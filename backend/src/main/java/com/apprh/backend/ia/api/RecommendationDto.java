package com.apprh.backend.ia.api;

import java.util.List;

public record RecommendationDto(
        Long employeeId,
        String employeeName,
        double totalScore,
        List<CriterionScoreDto> criteria,
        String explanation
) {
}