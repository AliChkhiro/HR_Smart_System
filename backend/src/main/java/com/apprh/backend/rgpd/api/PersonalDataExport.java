package com.apprh.backend.rgpd.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Export des donnees personnelles de l'utilisateur connecte (droit d'acces,
 * RGPD article 15). Objet JSON telechargeable, agrege a la demande.
 */
public record PersonalDataExport(
        Instant generatedAt,
        Account account,
        EmployeeProfile employee,
        List<SkillEntry> skills,
        List<LeaveEntry> leaves,
        List<ChatEntry> chatMessages
) {

    public record Account(
            Long id,
            String email,
            String firstName,
            String lastName,
            String role,
            String status,
            Instant createdAt
    ) {
    }

    public record EmployeeProfile(
            Long id,
            String jobTitle,
            LocalDate hireDate,
            String department,
            String status
    ) {
    }

    public record SkillEntry(
            String name,
            String category,
            int level
    ) {
    }

    public record LeaveEntry(
            String type,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String reason,
            Instant createdAt
    ) {
    }

    public record ChatEntry(
            String role,
            String content,
            Instant createdAt
    ) {
    }
}
