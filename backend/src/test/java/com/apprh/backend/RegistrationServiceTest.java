package com.apprh.backend;

import com.apprh.backend.auth.api.RegisterRequest;
import com.apprh.backend.auth.application.AuthService;
import com.apprh.backend.auth.infrastructure.JwtProperties;
import com.apprh.backend.auth.infrastructure.JwtService;
import com.apprh.backend.auth.infrastructure.RefreshTokenRepository;
import com.apprh.backend.notifications.application.NotificationService;
import com.apprh.backend.notifications.domain.NotificationType;
import com.apprh.backend.users.api.UserResponse;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;
import com.apprh.backend.users.infrastructure.UserMapper;
import com.apprh.backend.users.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesPendingEmployeeAndNotifiesRecruiters() {
        RegisterRequest request = new RegisterRequest("Nouveau@Example.com", "Nou", "Veau", "secret123");
        when(userRepository.existsByEmailAndDeletedAtIsNull("nouveau@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        User admin = User.builder().id(1L).email("admin@apprh.local").role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE).build();
        User rh = User.builder().id(2L).email("rh@apprh.local").role(UserRole.RH)
                .status(UserStatus.ACTIVE).build();
        when(userRepository.findByRoleInAndDeletedAtIsNull(any())).thenReturn(List.of(admin, rh));
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.getRole(), user.getStatus(), user.getCreatedAt());
        });

        UserResponse response = authService.register(request);

        assertThat(response.status()).isEqualTo(UserStatus.PENDING);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getRole()).isEqualTo(UserRole.EMPLOYEE);
        assertThat(saved.getEmail()).isEqualTo("nouveau@example.com");
        verify(notificationService).create(eq(1L), eq(NotificationType.USER_REGISTRATION), any());
        verify(notificationService).create(eq(2L), eq(NotificationType.USER_REGISTRATION), any());
    }
}
