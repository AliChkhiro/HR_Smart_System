package com.apprh.backend.users.application;

import com.apprh.backend.common.exception.ApiException;
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

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String search, UserRole role, boolean unassigned, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.activeAndFiltered(search, role, unassigned), pageable)
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
        return userMapper.toResponse(userRepository.save(user));
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
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_CANNOT_DELETE_SELF", "Un utilisateur ne peut pas supprimer son propre compte");
        }
        User user = findActive(id);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    private User findActive(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Utilisateur introuvable"));
    }
}
