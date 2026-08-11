package com.apprh.backend.employees.api;

public record EmployeeSkillResponse(
        Long skillId,
        String skillName,
        String category,
        int level
) {
}
