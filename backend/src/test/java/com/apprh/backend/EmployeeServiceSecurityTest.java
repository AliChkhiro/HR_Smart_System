package com.apprh.backend;

import com.apprh.backend.audit.application.AuditService;
import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.departments.infrastructure.DepartmentRepository;
import com.apprh.backend.employees.api.EmployeeCreateRequest;
import com.apprh.backend.employees.api.EmployeeResponse;
import com.apprh.backend.employees.api.EmployeeSkillRequest;
import com.apprh.backend.employees.api.EmployeeUpdateRequest;
import com.apprh.backend.employees.application.EmployeeService;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.domain.EmployeeStatus;
import com.apprh.backend.employees.infrastructure.EmployeeMapper;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.skills.infrastructure.EmployeeSkillRepository;
import com.apprh.backend.skills.infrastructure.SkillRepository;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;
import com.apprh.backend.users.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * P4 : verification du controle d'autorisation place directement dans
 * {@link EmployeeService} (defense en profondeur).
 *
 * <p>Ces tests appellent le service directement, sans passer par les
 * controleurs, afin de prouver que le contournement du {@code @PreAuthorize}
 * HTTP ne suffit pas a modifier une fiche employe.</p>
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceSecurityTest {

    private static final long EMPLOYEE_ID = 7L;
    private static final long ACTOR_ID = 42L;
    private static final long TARGET_USER_ID = 3L;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private EmployeeSkillRepository employeeSkillRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private EmployeeService employeeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- Acces autorises : ADMIN et RH conserves ---------------------------

    @Test
    void adminCanUpdateEmployee() {
        authenticate(UserRole.ADMIN);
        Employee employee = anEmployee();
        when(employeeRepository.findByIdAndDeletedAtIsNull(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeSkillRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());
        when(employeeMapper.toResponse(any(Employee.class))).thenReturn(aResponse());

        assertThatCode(() -> employeeService.update(EMPLOYEE_ID, updateRequest("Lead Engineer")))
                .doesNotThrowAnyException();

        assertThat(employee.getJobTitle()).isEqualTo("Lead Engineer");
        verify(employeeRepository).save(employee);
    }

    @Test
    void rhCanUpdateEmployee() {
        authenticate(UserRole.RH);
        Employee employee = anEmployee();
        when(employeeRepository.findByIdAndDeletedAtIsNull(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeSkillRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());
        when(employeeMapper.toResponse(any(Employee.class))).thenReturn(aResponse());

        assertThatCode(() -> employeeService.update(EMPLOYEE_ID, updateRequest("Responsable RH")))
                .doesNotThrowAnyException();

        verify(employeeRepository).save(employee);
    }

    // --- Acces interdits : EMPLOYEE ---------------------------------------

    @Test
    void employeeCannotCreateEmployee() {
        authenticate(UserRole.EMPLOYEE);

        assertThatThrownBy(() -> employeeService.create(
                new EmployeeCreateRequest(TARGET_USER_ID, null, "Intrusion", LocalDate.of(2026, 1, 1))))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void employeeCannotUpdateEmployee() {
        authenticate(UserRole.EMPLOYEE);

        assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, updateRequest("Intrusion")))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void employeeCannotDeleteEmployee() {
        authenticate(UserRole.EMPLOYEE);

        assertThatThrownBy(() -> employeeService.delete(EMPLOYEE_ID))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void employeeCannotAddSkill() {
        authenticate(UserRole.EMPLOYEE);

        assertThatThrownBy(() -> employeeService.addSkill(EMPLOYEE_ID, new EmployeeSkillRequest(1L, 5)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeSkillRepository);
    }

    @Test
    void employeeCannotUpdateSkillLevel() {
        authenticate(UserRole.EMPLOYEE);

        assertThatThrownBy(() -> employeeService.updateSkillLevel(EMPLOYEE_ID, 1L, 5))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeSkillRepository);
    }

    @Test
    void employeeCannotRemoveSkill() {
        authenticate(UserRole.EMPLOYEE);

        assertThatThrownBy(() -> employeeService.removeSkill(EMPLOYEE_ID, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeSkillRepository);
    }

    @Test
    void chefProjetCannotUpdateEmployee() {
        // CHEF_PROJET n'a jamais eu ce droit : la regle existante est preservee.
        authenticate(UserRole.CHEF_PROJET);

        assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, updateRequest("Intrusion")))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void unauthenticatedCannotUpdateEmployee() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, updateRequest("Intrusion")))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(employeeRepository);
    }

    // --- Consultation preservee pour un EMPLOYEE --------------------------

    @Test
    void employeeCanStillReadEmployeeRecord() {
        authenticate(UserRole.EMPLOYEE);
        EmployeeResponse response = aResponse();
        when(employeeRepository.findByIdAndDeletedAtIsNull(EMPLOYEE_ID)).thenReturn(Optional.of(anEmployee()));
        when(employeeSkillRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());
        when(employeeMapper.toResponse(any(Employee.class))).thenReturn(response);

        assertThat(employeeService.get(EMPLOYEE_ID)).isEqualTo(response);
    }

    // --- Helpers ----------------------------------------------------------

    private void authenticate(UserRole role) {
        UserPrincipal principal = new UserPrincipal(
                ACTOR_ID, "acteur@apprh.local", "Acteur", "Test", role, UserStatus.ACTIVE, "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private EmployeeUpdateRequest updateRequest(String jobTitle) {
        return new EmployeeUpdateRequest(null, jobTitle, null, null);
    }

    private Employee anEmployee() {
        User user = User.builder()
                .id(TARGET_USER_ID)
                .email("cible@apprh.local")
                .firstName("Cible")
                .lastName("Test")
                .passwordHash("hash")
                .role(UserRole.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        return Employee.builder()
                .id(EMPLOYEE_ID)
                .user(user)
                .jobTitle("Développeur")
                .hireDate(LocalDate.of(2024, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .build();
    }

    private EmployeeResponse aResponse() {
        return new EmployeeResponse(EMPLOYEE_ID, TARGET_USER_ID, "Cible", "Test", "cible@apprh.local",
                "Développeur", LocalDate.of(2024, 1, 1), null, null, EmployeeStatus.ACTIVE, List.of(), null);
    }
}
