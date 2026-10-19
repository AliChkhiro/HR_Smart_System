package com.apprh.backend.users.application;

import com.apprh.backend.audit.application.AuditAction;
import com.apprh.backend.audit.application.AuditService;
import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.email.application.EmailService;
import com.apprh.backend.notifications.application.NotificationService;
import com.apprh.backend.notifications.domain.NotificationType;
import com.apprh.backend.users.api.UserCreateRequest;
import com.apprh.backend.users.api.UserResponse;
import com.apprh.backend.users.api.UserUpdateRequest;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;
import com.apprh.backend.users.infrastructure.UserMapper;
import com.apprh.backend.users.infrastructure.UserRepository;
import com.apprh.backend.users.infrastructure.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String search, UserRole role, boolean unassigned, UserStatus status, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.activeAndFiltered(search, role, unassigned, status), pageable)
                .map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_EMAIL_EXISTS", "Un utilisateur avec cet email existe déjà");
        }
        User user = User.builder()
                .email(email)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        auditService.record(saved.getId(), saved.getEmail(), AuditAction.ENTITY_CREATED, "USER", saved.getId(),
                "Création de compte (rôle " + saved.getRole() + ")");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, Long currentUserId) {
        User user = findActive(id);
        if (request.role() != null && request.role() != user.getRole()) {
            if (id.equals(currentUserId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "USER_CANNOT_CHANGE_OWN_ROLE", "Un utilisateur ne peut pas modifier son propre rôle");
            }
            user.setRole(request.role());
        }
        if (request.status() != null && request.status() != user.getStatus()) {
            if (id.equals(currentUserId) && request.status() == UserStatus.DISABLED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "USER_CANNOT_DISABLE_SELF", "Un utilisateur ne peut pas désactiver son propre compte");
            }
            user.setStatus(request.status());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        User saved = userRepository.save(user);
        auditService.record(saved.getId(), saved.getEmail(), AuditAction.ENTITY_UPDATED, "USER", saved.getId(),
                "Modification de compte");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_CANNOT_DELETE_SELF", "Un utilisateur ne peut pas supprimer son propre compte");
        }
        User user = findActive(id);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        auditService.record(AuditAction.ENTITY_DELETED, "USER", id, "Suppression douce d'un compte");
    }

    /**
     * Valide une demande d'inscription en attente (PENDING → ACTIVE) et en informe l'intéressé.
     */
    @Transactional
    public UserResponse approve(Long id) {
        User user = requirePending(id);
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        auditService.record(saved.getId(), saved.getEmail(), AuditAction.USER_APPROVED, "USER", saved.getId(),
                "Approbation du compte");
        notifyDecision(saved, NotificationType.USER_APPROVED,
                "Votre compte a été validé. Vous pouvez maintenant vous connecter à AppRH.");
        return userMapper.toResponse(saved);
    }

    /**
     * Refuse une demande d'inscription en attente (PENDING → DISABLED) et en informe l'intéressé.
     */
    @Transactional
    public UserResponse reject(Long id) {
        User user = requirePending(id);
        user.setStatus(UserStatus.DISABLED);
        User saved = userRepository.save(user);
        auditService.record(saved.getId(), saved.getEmail(), AuditAction.USER_REJECTED, "USER", saved.getId(),
                "Rejet du compte");
        notifyDecision(saved, NotificationType.USER_REJECTED,
                "Votre demande de création de compte n'a pas été validée. Contactez les ressources humaines.");
        return userMapper.toResponse(saved);
    }

    private User requirePending(Long id) {
        User user = findActive(id);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_NOT_PENDING",
                    "Cette demande d'inscription n'est pas en attente de validation");
        }
        return user;
    }

    private void notifyDecision(User user, NotificationType type, String message) {
        notificationService.create(user.getId(), type, message);
        emailService.send(user.getEmail(), "AppRH — Votre demande de compte", message);
    }

    private User findActive(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Utilisateur introuvable"));
    }
}
