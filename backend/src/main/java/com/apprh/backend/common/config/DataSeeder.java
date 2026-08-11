package com.apprh.backend.common.config;

import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final List<SeedAccount> ACCOUNTS = List.of(
            new SeedAccount("admin@apprh.local", "Admin", "Système", UserRole.ADMIN),
            new SeedAccount("rh@apprh.local", "Karim", "Benali", UserRole.RH),
            new SeedAccount("chefprojet@apprh.local", "Sara", "El Amrani", UserRole.CHEF_PROJET),
            new SeedAccount("employe@apprh.local", "Yassine", "Idrissi", UserRole.EMPLOYEE)
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        for (SeedAccount account : ACCOUNTS) {
            if (!userRepository.existsByEmailAndDeletedAtIsNull(account.email())) {
                userRepository.save(User.builder()
                        .email(account.email())
                        .firstName(account.firstName())
                        .lastName(account.lastName())
                        .passwordHash(passwordEncoder.encode(seedPassword))
                        .role(account.role())
                        .build());
            }
        }
    }

    private record SeedAccount(String email, String firstName, String lastName, UserRole role) {
    }
}
