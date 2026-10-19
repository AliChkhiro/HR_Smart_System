package com.apprh.backend.auth.application;

import com.apprh.backend.audit.application.AuditAction;
import com.apprh.backend.audit.application.AuditService;
import com.apprh.backend.auth.api.AuthResponse;
import com.apprh.backend.auth.api.LoginRequest;
import com.apprh.backend.auth.api.RegisterRequest;
import com.apprh.backend.auth.domain.RefreshToken;
import com.apprh.backend.auth.infrastructure.JwtProperties;
import com.apprh.backend.auth.infrastructure.JwtService;
import com.apprh.backend.auth.infrastructure.RefreshTokenRepository;
import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.notifications.application.NotificationService;
import com.apprh.backend.notifications.domain.NotificationType;
import com.apprh.backend.users.api.UserResponse;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;
import com.apprh.backend.users.infrastructure.UserMapper;
import com.apprh.backend.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            User user = userRepository.findByEmailAndDeletedAtIsNull(request.email().trim().toLowerCase())
                    .orElseThrow(() -> invalidCredentials());
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw invalidCredentials();
            }
            ensureAccountUsable(user);
            auditService.record(user.getId(), user.getEmail(), AuditAction.LOGIN_SUCCESS, "USER", user.getId(), "Connexion réussie");
            return issueTokens(user);
        } catch (ApiException ex) {
            if ("AUTH_INVALID_CREDENTIALS".equals(ex.getCode())) {
                auditLoginFailure(request);
            }
            throw ex;
        }
    }

    private void auditLoginFailure(LoginRequest request) {
        String email = request.email() == null ? null : request.email().trim().toLowerCase();
        Long userId = email == null ? null
                : userRepository.findByEmailAndDeletedAtIsNull(email).map(User::getId).orElse(null);
        auditService.record(userId, email, AuditAction.LOGIN_FAILURE, "USER", userId, "Échec de connexion");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_EMAIL_EXISTS",
                    "Un compte avec cet email existe déjà");
        }
        User user = User.builder()
                .email(email)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.EMPLOYEE)
                .status(UserStatus.PENDING)
                .build();
        User saved = userRepository.save(user);
        notifyRecruiters(saved);
        return userMapper.toResponse(saved);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(jwtService.sha256(refreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_REFRESH_TOKEN", "Jeton de rafraîchissement invalide"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_REFRESH_TOKEN", "Jeton de rafraîchissement expiré ou révoqué");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(stored.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_REFRESH_TOKEN", "Utilisateur introuvable"));
        ensureAccountUsable(user);
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(jwtService.sha256(refreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Utilisateur introuvable"));
        return userMapper.toResponse(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();
        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(jwtService.sha256(refreshToken))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()))
                .build();
        refreshTokenRepository.save(entity);
        return new AuthResponse(accessToken, refreshToken, userMapper.toResponse(user));
    }

    /**
     * Empêche l'émission de jetons pour un compte non opérationnel.
     * Un compte PENDING attend la validation RH ; DISABLED est un compte désactivé.
     */
    private void ensureAccountUsable(User user) {
        if (user.getStatus() == UserStatus.PENDING) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_PENDING",
                    "Votre compte est en attente de validation par les ressources humaines");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_DISABLED", "Ce compte est désactivé");
        }
    }

    private void notifyRecruiters(User user) {
        String message = "%s %s (%s) demande la création d'un compte"
                .formatted(user.getFirstName(), user.getLastName(), user.getEmail());
        for (User recruiter : userRepository.findByRoleInAndDeletedAtIsNull(List.of(UserRole.ADMIN, UserRole.RH))) {
            notificationService.create(recruiter.getId(), NotificationType.USER_REGISTRATION, message);
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Email ou mot de passe invalide");
    }
}
