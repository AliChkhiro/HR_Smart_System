package com.apprh.backend.ia.application;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.leaves.domain.LeaveRequest;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.tasks.domain.Task;
import com.apprh.backend.tasks.domain.TaskStatus;
import com.apprh.backend.tasks.infrastructure.TaskRepository;
import com.apprh.backend.users.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Outils serveur ("tools") du chatbot : consultations en lecture seule, routées
 * de façon déterministe et strictement scopées RBAC.
 *
 * <p>Le LLM ne choisit ni n'exécute ces outils et ne décide jamais des
 * autorisations : le routage est déterministe et chaque outil vérifie le rôle de
 * l'utilisateur (issu du JWT) avant de renvoyer la moindre donnée. Une demande
 * hors périmètre retourne un refus explicite plutôt qu'un accès.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatbotToolService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<UserRole> LEAVE_VALIDATORS = Set.of(UserRole.RH, UserRole.ADMIN);
    private static final Set<UserRole> MANAGERS = Set.of(UserRole.RH, UserRole.ADMIN, UserRole.CHEF_PROJET);
    private static final int MAX_ITEMS = 10;

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public Optional<String> answer(UserPrincipal principal, String message) {
        String normalized = normalize(message);
        if (asksUnassignedTasks(normalized)) {
            return Optional.of(unassignedTasks(principal));
        }
        if (asksTaskList(normalized)) {
            return employeeRepository.findByUserIdAndDeletedAtIsNull(principal.id())
                    .map(employee -> myOpenTasks(employee.getId()));
        }
        if (asksPendingLeaves(normalized)) {
            return Optional.of(pendingLeaves(principal));
        }
        if (asksLeaveList(normalized)) {
            return employeeRepository.findByUserIdAndDeletedAtIsNull(principal.id())
                    .map(employee -> myLeaves(employee.getId()));
        }
        return Optional.empty();
    }

    // --- Routage déterministe ---

    private boolean asksUnassignedTasks(String n) {
        return n.contains("tache") && (n.contains("non assigne") || n.contains("sans assigne")
                || n.contains("non attribue") || n.contains("pas assigne"));
    }

    private boolean asksTaskList(String n) {
        return n.contains("tache") && listIntent(n);
    }

    private boolean asksPendingLeaves(String n) {
        return n.contains("conge") && (n.contains("en attente") || n.contains("a valider")
                || n.contains("demandes"));
    }

    private boolean asksLeaveList(String n) {
        return n.contains("conge") && listIntent(n);
    }

    private boolean listIntent(String n) {
        return n.contains("liste") || n.contains("lesquel") || n.contains("detail")
                || n.contains("affiche") || n.contains("montre") || n.contains("donne moi")
                || n.contains("quelles sont") || n.contains("quels sont");
    }

    // --- Outils (lecture seule, scopés RBAC) ---

    private String unassignedTasks(UserPrincipal principal) {
        if (!MANAGERS.contains(principal.role())) {
            return "Cette information est réservée aux managers et aux RH.";
        }
        List<Task> tasks = taskRepository
                .findTop10ByAssigneeIsNullAndDeletedAtIsNullAndStatusNotOrderByDueDateAsc(TaskStatus.DONE);
        if (tasks.isEmpty()) {
            return "Aucune tâche non assignée en cours.";
        }
        return "Tâches non assignées en cours (%d) :\n%s".formatted(tasks.size(), describeTasks(tasks));
    }

    private String pendingLeaves(UserPrincipal principal) {
        if (LEAVE_VALIDATORS.contains(principal.role())) {
            List<LeaveRequest> leaves = leaveRequestRepository
                    .findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(LeaveStatus.PENDING, PageRequest.of(0, MAX_ITEMS));
            if (leaves.isEmpty()) {
                return "Aucune demande de congé en attente.";
            }
            String list = leaves.stream().map(this::describeTeamLeave).collect(Collectors.joining("\n"));
            return "Demandes de congé en attente (%d) :\n%s".formatted(leaves.size(), list);
        }
        return employeeRepository.findByUserIdAndDeletedAtIsNull(principal.id())
                .map(employee -> {
                    List<LeaveRequest> leaves = leaveRequestRepository
                            .findByEmployeeIdAndDeletedAtIsNull(employee.getId()).stream()
                            .filter(leave -> leave.getStatus() == LeaveStatus.PENDING)
                            .limit(MAX_ITEMS)
                            .toList();
                    if (leaves.isEmpty()) {
                        return "Vous n'avez aucune demande de congé en attente.";
                    }
                    String list = leaves.stream().map(this::describeLeave).collect(Collectors.joining("\n"));
                    return "Vos demandes de congé en attente (%d) :\n%s".formatted(leaves.size(), list);
                })
                .orElse("Aucune demande de congé en attente.");
    }

    private String myOpenTasks(Long employeeId) {
        List<Task> tasks = taskRepository
                .findTop10ByAssigneeIdAndDeletedAtIsNullAndStatusNotOrderByDueDateAsc(employeeId, TaskStatus.DONE);
        if (tasks.isEmpty()) {
            return "Vous n'avez aucune tâche en cours.";
        }
        return "Vos tâches en cours (%d) :\n%s".formatted(tasks.size(), describeTasks(tasks));
    }

    private String myLeaves(Long employeeId) {
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdAndDeletedAtIsNull(employeeId);
        if (leaves.isEmpty()) {
            return "Vous n'avez aucune demande de congé.";
        }
        List<LeaveRequest> limited = leaves.stream().limit(MAX_ITEMS).toList();
        return "Vos congés (%d) :\n%s".formatted(limited.size(),
                limited.stream().map(this::describeLeave).collect(Collectors.joining("\n")));
    }

    private String describeTasks(List<Task> tasks) {
        return tasks.stream().map(this::describeTask).collect(Collectors.joining("\n"));
    }

    private String describeTask(Task task) {
        String due = task.getDueDate() == null ? "sans échéance" : "échéance " + task.getDueDate().format(DATE_FORMAT);
        String project = task.getProject() == null ? "" : " (projet " + task.getProject().getName() + ")";
        return "- %s [%s, %s] %s%s".formatted(task.getName(), task.getStatus(), task.getPriority(), due, project);
    }

    private String describeLeave(LeaveRequest leave) {
        return "- %s du %s au %s (%s)".formatted(
                leave.getType(),
                leave.getStartDate().format(DATE_FORMAT),
                leave.getEndDate().format(DATE_FORMAT),
                leave.getStatus());
    }

    private String describeTeamLeave(LeaveRequest leave) {
        String name = leave.getEmployee().getUser().getFirstName() + " "
                + leave.getEmployee().getUser().getLastName();
        return "- %s : %s du %s au %s".formatted(name, leave.getType(),
                leave.getStartDate().format(DATE_FORMAT), leave.getEndDate().format(DATE_FORMAT));
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
