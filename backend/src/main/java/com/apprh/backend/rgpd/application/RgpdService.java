package com.apprh.backend.rgpd.application;

import com.apprh.backend.audit.application.AuditAction;
import com.apprh.backend.audit.application.AuditService;
import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.ia.domain.ChatMessage;
import com.apprh.backend.ia.infrastructure.ChatMessageRepository;
import com.apprh.backend.leaves.domain.LeaveRequest;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.notifications.application.NotificationService;
import com.apprh.backend.notifications.domain.NotificationType;
import com.apprh.backend.rgpd.api.ErasureRequestResponse;
import com.apprh.backend.rgpd.api.PersonalDataExport;
import com.apprh.backend.rgpd.domain.ErasureRequest;
import com.apprh.backend.rgpd.domain.ErasureStatus;
import com.apprh.backend.rgpd.infrastructure.ErasureRequestRepository;
import com.apprh.backend.skills.domain.EmployeeSkill;
import com.apprh.backend.skills.infrastructure.EmployeeSkillRepository;
import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Conformite RGPD : droit d'acces (export des donnees personnelles) et droit a
 * l'effacement (demande validee par un administrateur).
 */
@Service
@RequiredArgsConstructor
public class RgpdService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ErasureRequestRepository erasureRequestRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /**
     * Agrege les donnees personnelles de l'utilisateur connecte (droit d'acces).
     *
     * Transaction NON readOnly : l'export s'accompagne d'une ecriture d'audit
     * (RGPD_EXPORTED). Une transaction readOnly refuserait cet INSERT.
     */
    @Transactional
    public PersonalDataExport export(UserPrincipal principal) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Utilisateur introuvable"));

        auditService.record(AuditAction.RGPD_EXPORTED, "USER", user.getId(),
                "Export des données personnelles (droit d'accès)");

        Employee employee = employeeRepository.findByUserIdAndDeletedAtIsNull(user.getId()).orElse(null);
        return new PersonalDataExport(
                Instant.now(),
                new PersonalDataExport.Account(
                        user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                        user.getRole().name(), user.getStatus().name(), user.getCreatedAt()),
                employee == null ? null : toEmployeeProfile(employee),
                employee == null ? List.of() : toSkills(employee.getId()),
                employee == null ? List.of() : toLeaves(employee.getId()),
                toChatMessages(user.getId())
        );
    }

    /**
     * Depose une demande d'effacement pour l'utilisateur connecte. Une seule
     * demande en attente par utilisateur.
     */
    @Transactional
    public ErasureRequestResponse requestErasure(UserPrincipal principal, String reason) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Utilisateur introuvable"));
        erasureRequestRepository.findByUserIdAndStatus(user.getId(), ErasureStatus.PENDING).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "ERASURE_REQUEST_PENDING",
                    "Une demande d'effacement est déjà en attente de validation");
        });
        ErasureRequest request = ErasureRequest.builder()
                .userId(user.getId())
                .status(ErasureStatus.PENDING)
                .reason(trimToNull(reason))
                .build();
        ErasureRequest saved = erasureRequestRepository.save(request);
        auditService.record(AuditAction.RGPD_ERASURE_REQUESTED, "ERASURE_REQUEST", saved.getId(),
                "Demande d'effacement de compte déposée");
        notifyAdmins("Nouvelle demande d'effacement de compte : " + user.getEmail());
        return toResponse(saved, user.getEmail());
    }

    /** Demandes d'effacement de l'utilisateur connecte. */
    @Transactional(readOnly = true)
    public List<ErasureRequestResponse> myErasureRequests(UserPrincipal principal) {
        return erasureRequestRepository.findByUserIdOrderByRequestedAtDesc(principal.id()).stream()
                .map(request -> toResponse(request, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ErasureRequestResponse> listErasureRequests(ErasureStatus status, Pageable pageable) {
        Page<ErasureRequest> page = status == null
                ? erasureRequestRepository.findAllByOrderByRequestedAtDesc(pageable)
                : erasureRequestRepository.findByStatusOrderByRequestedAtAsc(status, pageable);
        return page.map(request -> toResponse(request, emailOf(request.getUserId())));
    }

    /**
     * Valide une demande d'effacement : le compte vise est supprime (suppression
     * douce). L'effacement n'est jamais automatique, il est toujours valide par
     * un administrateur.
     */
    @Transactional
    public ErasureRequestResponse approve(Long id, Long adminUserId) {
        ErasureRequest request = findPending(id);
        if (request.getUserId().equals(adminUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ERASURE_CANNOT_APPROVE_SELF",
                    "Vous ne pouvez pas valider l'effacement de votre propre compte");
        }
        User target = userRepository.findByIdAndDeletedAtIsNull(request.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Utilisateur introuvable"));
        target.setDeletedAt(Instant.now());
        userRepository.save(target);
        request.setStatus(ErasureStatus.APPROVED);
        request.setDecidedAt(Instant.now());
        request.setDecidedByUserId(adminUserId);
        ErasureRequest saved = erasureRequestRepository.save(request);
        auditService.record(AuditAction.RGPD_ERASURE_APPROVED, "ERASURE_REQUEST", saved.getId(),
                "Effacement de compte validé pour " + target.getEmail());
        return toResponse(saved, target.getEmail());
    }

    @Transactional
    public ErasureRequestResponse reject(Long id, Long adminUserId, String comment) {
        ErasureRequest request = findPending(id);
        request.setStatus(ErasureStatus.REJECTED);
        request.setDecidedAt(Instant.now());
        request.setDecidedByUserId(adminUserId);
        ErasureRequest saved = erasureRequestRepository.save(request);
        String email = emailOf(request.getUserId());
        auditService.record(AuditAction.RGPD_ERASURE_REJECTED, "ERASURE_REQUEST", saved.getId(),
                "Demande d'effacement refusée" + (trimToNull(comment) == null ? "" : " : " + trimToNull(comment)));
        notificationService.create(request.getUserId(), NotificationType.ERASURE_DECIDED,
                "Votre demande d'effacement de compte a été refusée"
                        + (trimToNull(comment) == null ? "" : " : " + trimToNull(comment)));
        return toResponse(saved, email);
    }

    private ErasureRequest findPending(Long id) {
        ErasureRequest request = erasureRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERASURE_REQUEST_NOT_FOUND",
                        "Demande d'effacement introuvable"));
        if (request.getStatus() != ErasureStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "ERASURE_REQUEST_ALREADY_DECIDED",
                    "Cette demande a déjà été traitée");
        }
        return request;
    }

    private void notifyAdmins(String message) {
        List<User> admins = userRepository.findByRoleInAndDeletedAtIsNull(List.of(UserRole.ADMIN));
        for (User admin : admins) {
            notificationService.create(admin.getId(), NotificationType.ERASURE_REQUESTED, message);
        }
    }

    private String emailOf(Long userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }

    private PersonalDataExport.EmployeeProfile toEmployeeProfile(Employee employee) {
        return new PersonalDataExport.EmployeeProfile(
                employee.getId(),
                employee.getJobTitle(),
                employee.getHireDate(),
                employee.getDepartment() == null ? null : employee.getDepartment().getName(),
                employee.getStatus().name());
    }

    private List<PersonalDataExport.SkillEntry> toSkills(Long employeeId) {
        return employeeSkillRepository.findAllByEmployeeId(employeeId).stream()
                .map(link -> new PersonalDataExport.SkillEntry(
                        link.getSkill().getName(), link.getSkill().getCategory(), link.getLevel()))
                .toList();
    }

    private List<PersonalDataExport.LeaveEntry> toLeaves(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdAndDeletedAtIsNull(employeeId).stream()
                .map(this::toLeaveEntry)
                .toList();
    }

    private PersonalDataExport.LeaveEntry toLeaveEntry(LeaveRequest leave) {
        return new PersonalDataExport.LeaveEntry(
                leave.getType().name(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getStatus().name(),
                leave.getReason(),
                leave.getCreatedAt());
    }

    private List<PersonalDataExport.ChatEntry> toChatMessages(Long userId) {
        return chatMessageRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(this::toChatEntry)
                .toList();
    }

    private PersonalDataExport.ChatEntry toChatEntry(ChatMessage message) {
        return new PersonalDataExport.ChatEntry(message.getRole(), message.getContent(), message.getCreatedAt());
    }

    private ErasureRequestResponse toResponse(ErasureRequest request, String userEmail) {
        return new ErasureRequestResponse(
                request.getId(), request.getUserId(), userEmail, request.getStatus(),
                request.getReason(), request.getRequestedAt(), request.getDecidedAt(),
                request.getDecidedByUserId());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
