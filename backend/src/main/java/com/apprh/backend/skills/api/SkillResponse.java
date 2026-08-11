package com.apprh.backend.skills.api;

import java.time.Instant;

public record SkillResponse(
        Long id,
        String name,
        String category,
        long employeeCount,
        Instant createdAt
) {
}
