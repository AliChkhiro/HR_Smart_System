package com.apprh.backend;

import com.apprh.backend.audit.application.AuditService;
import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.email.application.EmailService;
import com.apprh.backend.notifications.application.NotificationService;
import com.apprh.backend.notifications.domain.NotificationType;
import com.apprh.backend.users.api.UserResponse;
import com.apprh.backend.users.application.UserService;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;
import com.apprh.backend.users.infrastructure.UserMapper;
import com.apprh.backend.users.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountApprovalServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private User pendingUser() {
        return User.builder()
                .id(7L)
                .email("candidat@example.com")
                .firstName("Candide")
                .lastName("Martin")
                .passwordHash("hash")
                .role(UserRole.EMPLOYEE)
                .status(UserStatus.PENDING)
                .build();
    }

    private void stubMapping() {
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.getRole(), user.getStatus(), user.getCreatedAt());
        });
    }

    @Test
    void approveActivatesPendingAccountAndNotifiesApplicant() {
        User user = pendingUser();
        when(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubMapping();

        UserResponse response = userService.approve(7L);

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(notificationService).create(eq(7L), eq(NotificationType.USER_APPROVED), any());
        verify(emailService).send(eq("candidat@example.com"), any(), any());
    }

    @Test
    void rejectDisablesPendingAccountAndNotifiesApplicant() {
        User user = pendingUser();
        when(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubMapping();

        UserResponse response = userService.reject(7L);

        assertThat(response.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        verify(notificationService).create(eq(7L), eq(NotificationType.USER_REJECTED), any());
        verify(emailService).send(eq("candidat@example.com"), any(), any());
    }

    @Test
    void approveRejectsAccountThatIsNotPending() {
        User user = pendingUser();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.approve(7L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("attente");
        verifyNoInteractions(notificationService, emailService);
    }

    @Test
    void rejectRejectsAccountThatIsNotPending() {
        User user = pendingUser();
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.reject(7L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("attente");
        verifyNoInteractions(notificationService, emailService);
    }
}
