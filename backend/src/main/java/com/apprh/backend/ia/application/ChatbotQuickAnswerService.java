package com.apprh.backend.ia.application;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.leaves.domain.LeaveRequest;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.projects.domain.ProjectMember;
import com.apprh.backend.projects.infrastructure.ProjectMemberRepository;
import com.apprh.backend.tasks.domain.TaskStatus;
import com.apprh.backend.tasks.infrastructure.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Réponses déterministes pour les questions de comptage courantes (congés, tâches,
 * projets). Ces réponses proviennent directement des données de l'utilisateur
 * authentifié (donc fiables à 100 %) et court-circuitent le LLM. Toute autre
 * question est laissée au modèle.
 */
@Service
@RequiredArgsConstructor
public class ChatbotQuickAnswerService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional(readOnly = true)
    public Optional<String> answer(UserPrincipal principal, String message) {
        String normalized = normalize(message);
        if (!isCountIntent(normalized)) {
            return Optional.empty();
        }
        return employeeRepository.findByUserIdAndDeletedAtIsNull(principal.id())
                .flatMap(employee -> build(employee.getId(), normalized));
    }

    private boolean isCountIntent(String normalized) {
        boolean asksCount = normalized.contains("combien")
                || normalized.contains("nombre")
                || normalized.contains("total");
        boolean wantsDetails = normalized.contains("lesquel")
                || normalized.contains("detail")
                || normalized.contains("liste");
        boolean aboutRhData = normalized.contains("conge")
                || normalized.contains("tache")
                || normalized.contains("projet");
        return asksCount && aboutRhData && !wantsDetails;
    }

    private Optional<String> build(Long employeeId, String normalized) {
        if (normalized.contains("conge")) {
            return Optional.of(leaveAnswer(employeeId, normalized));
        }
        if (normalized.contains("tache")) {
            return Optional.of(taskAnswer(employeeId, normalized));
        }
        if (normalized.contains("projet")) {
            return Optional.of(projectAnswer(employeeId));
        }
        return Optional.empty();
    }

    private String leaveAnswer(Long employeeId, String normalized) {
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdAndDeletedAtIsNull(employeeId);
        long approved = leaves.stream().filter(leave -> leave.getStatus() == LeaveStatus.APPROVED).count();
        long pending = leaves.stream().filter(leave -> leave.getStatus() == LeaveStatus.PENDING).count();
        long rejected = leaves.stream().filter(leave -> leave.getStatus() == LeaveStatus.REJECTED).count();
        long approvedDays = leaves.stream()
                .filter(leave -> leave.getStatus() == LeaveStatus.APPROVED)
                .mapToLong(this::inclusiveDays)
                .sum();

        if (normalized.contains("approuv")) {
            return "Vous avez %d congé(s) approuvé(s), soit %d jour(s).".formatted(approved, approvedDays);
        }
        if (normalized.contains("attente")) {
            return "Vous avez %d demande(s) de congé en attente.".formatted(pending);
        }
        if (normalized.contains("refus")) {
            return "Vous avez %d demande(s) de congé refusée(s).".formatted(rejected);
        }
        return "Vos congés : %d approuvé(s) (%d jour(s)), %d en attente et %d refusé(s), soit %d au total."
                .formatted(approved, approvedDays, pending, rejected, approved + pending + rejected);
    }

    private String taskAnswer(Long employeeId, String normalized) {
        if (normalized.contains("termin") || normalized.contains("complete") || normalized.contains("finie")
                || normalized.contains("finies") || normalized.contains("realise")) {
            long done = taskRepository.countByAssigneeIdAndDeletedAtIsNullAndStatus(employeeId, TaskStatus.DONE);
            return "Vous avez %d tâche(s) terminée(s).".formatted(done);
        }
        long open = taskRepository.countByAssigneeIdAndDeletedAtIsNullAndStatusNot(employeeId, TaskStatus.DONE);
        return "Vous avez %d tâche(s) en cours (non terminée(s)).".formatted(open);
    }

    private String projectAnswer(Long employeeId) {
        long count = projectMemberRepository.findAllByEmployeeId(employeeId).stream()
                .map(ProjectMember::getProject)
                .filter(project -> project.getDeletedAt() == null)
                .count();
        return "Vous participez à %d projet(s).".formatted(count);
    }

    private long inclusiveDays(LeaveRequest leave) {
        if (leave.getStartDate() == null || leave.getEndDate() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
