package com.apprh.backend.common.config;

import com.apprh.backend.departments.domain.Department;
import com.apprh.backend.departments.infrastructure.DepartmentRepository;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.skills.domain.EmployeeSkill;
import com.apprh.backend.skills.domain.Skill;
import com.apprh.backend.skills.infrastructure.EmployeeSkillRepository;
import com.apprh.backend.skills.infrastructure.SkillRepository;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final List<SeedAccount> ACCOUNTS = List.of(
            new SeedAccount("admin@apprh.local", "Admin", "Système", UserRole.ADMIN),
            new SeedAccount("rh@apprh.local", "Karim", "Benali", UserRole.RH),
            new SeedAccount("chefprojet@apprh.local", "Sara", "El Amrani", UserRole.CHEF_PROJET),
            new SeedAccount("employe@apprh.local", "Yassine", "Idrissi", UserRole.EMPLOYEE)
    );

    private static final List<SeedDepartment> DEPARTMENTS = List.of(
            new SeedDepartment("Développement", "Équipes d'ingénierie et développement logiciel"),
            new SeedDepartment("Ressources Humaines", "Gestion du personnel et des opérations RH"),
            new SeedDepartment("Commercial", "Ventes, relation client et prospection"),
            new SeedDepartment("Production", "Exécution des projets et livraison")
    );

    private static final List<SeedSkill> SKILLS = List.of(
            new SeedSkill("Java", "DEVELOPPEMENT"),
            new SeedSkill("Spring Boot", "DEVELOPPEMENT"),
            new SeedSkill("Angular", "DEVELOPPEMENT"),
            new SeedSkill("PostgreSQL", "BASE_DE_DONNEES"),
            new SeedSkill("Docker", "DEVOPS"),
            new SeedSkill("Gestion de projet", "GESTION"),
            new SeedSkill("Gestion des RH", "RH"),
            new SeedSkill("Négociation", "COMMERCIAL")
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

    @Value("${app.seed.password}")
    private String seedPassword;

    @Override
    @Transactional
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

        Map<String, Department> departments = Map.of(
                DEPARTMENTS.get(0).name(), findOrCreateDepartment(DEPARTMENTS.get(0)),
                DEPARTMENTS.get(1).name(), findOrCreateDepartment(DEPARTMENTS.get(1)),
                DEPARTMENTS.get(2).name(), findOrCreateDepartment(DEPARTMENTS.get(2)),
                DEPARTMENTS.get(3).name(), findOrCreateDepartment(DEPARTMENTS.get(3))
        );

        Map<String, Skill> skills = Map.of(
                SKILLS.get(0).name(), findOrCreateSkill(SKILLS.get(0)),
                SKILLS.get(1).name(), findOrCreateSkill(SKILLS.get(1)),
                SKILLS.get(2).name(), findOrCreateSkill(SKILLS.get(2)),
                SKILLS.get(3).name(), findOrCreateSkill(SKILLS.get(3)),
                SKILLS.get(4).name(), findOrCreateSkill(SKILLS.get(4)),
                SKILLS.get(5).name(), findOrCreateSkill(SKILLS.get(5)),
                SKILLS.get(6).name(), findOrCreateSkill(SKILLS.get(6)),
                SKILLS.get(7).name(), findOrCreateSkill(SKILLS.get(7))
        );

        seedEmployee(ACCOUNTS.get(0), departments.get("Développement"), "Administrateur Système",
                LocalDate.of(2024, 1, 15));
        seedEmployee(ACCOUNTS.get(1), departments.get("Ressources Humaines"), "Responsable RH",
                LocalDate.of(2023, 5, 2));
        seedEmployee(ACCOUNTS.get(2), departments.get("Développement"), "Chef de Projet",
                LocalDate.of(2023, 9, 18));
        seedEmployee(ACCOUNTS.get(3), departments.get("Développement"), "Développeur Full-stack",
                LocalDate.of(2025, 3, 10));

        seedEmployeeSkill("employe@apprh.local", skills.get("Java"), 4);
        seedEmployeeSkill("employe@apprh.local", skills.get("Spring Boot"), 3);
        seedEmployeeSkill("employe@apprh.local", skills.get("Angular"), 3);
        seedEmployeeSkill("employe@apprh.local", skills.get("PostgreSQL"), 3);
        seedEmployeeSkill("employe@apprh.local", skills.get("Docker"), 2);
        seedEmployeeSkill("chefprojet@apprh.local", skills.get("Gestion de projet"), 5);
        seedEmployeeSkill("chefprojet@apprh.local", skills.get("Java"), 2);
        seedEmployeeSkill("rh@apprh.local", skills.get("Gestion des RH"), 5);
        seedEmployeeSkill("admin@apprh.local", skills.get("Docker"), 3);
    }

    private Department findOrCreateDepartment(SeedDepartment seed) {
        return departmentRepository.findByNameIgnoreCaseAndDeletedAtIsNull(seed.name())
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name(seed.name())
                        .description(seed.description())
                        .build()));
    }

    private Skill findOrCreateSkill(SeedSkill seed) {
        return skillRepository.findByNameIgnoreCaseAndDeletedAtIsNull(seed.name())
                .orElseGet(() -> skillRepository.save(Skill.builder()
                        .name(seed.name())
                        .category(seed.category())
                        .build()));
    }

    private void seedEmployee(SeedAccount account, Department department, String jobTitle, LocalDate hireDate) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(account.email()).orElseThrow();
        if (!employeeRepository.existsByUserIdAndDeletedAtIsNull(user.getId())) {
            employeeRepository.save(Employee.builder()
                    .user(user)
                    .department(department)
                    .jobTitle(jobTitle)
                    .hireDate(hireDate)
                    .build());
        }
    }

    private void seedEmployeeSkill(String email, Skill skill, int level) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        Employee employee = employeeRepository.findByUserIdAndDeletedAtIsNull(user.getId()).orElseThrow();
        if (!employeeSkillRepository.existsByEmployeeIdAndSkillId(employee.getId(), skill.getId())) {
            employeeSkillRepository.save(EmployeeSkill.builder()
                    .employee(employee)
                    .skill(skill)
                    .level(level)
                    .build());
        }
    }

    private record SeedAccount(String email, String firstName, String lastName, UserRole role) {
    }

    private record SeedDepartment(String name, String description) {
    }

    private record SeedSkill(String name, String category) {
    }
}
