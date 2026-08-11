package com.apprh.backend.leaves.application;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.leaves.api.LeaveRequestRecord;
import com.apprh.backend.leaves.api.LeaveResponse;
import com.apprh.backend.leaves.api.LeaveReviewRequest;
import com.apprh.backend.leaves.domain.LeaveRequest;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.domain.LeaveType;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.leaves.infrastructure.LeaveSpecifications;
import com.apprh.backend.notifications.application.NotificationService;
import com.apprh.backend.notifications.domain.NotificationType;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<UserRole> MANAGER_ROLES =
            List.of(UserRole.ADMIN, UserRole.RH, UserRole.CHEF_PROJET);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<LeaveResponse> list(Long currentUserId, boolean manager, Long employeeId, LeaveStatus status,
                                    LeaveType type, LocalDate from, LocalDate to, Pageable pageable) {
        Long scope = manager ? employeeId : employeeOf(currentUserId).getId();
        Specification<LeaveRequest> spec = Specification
                .where(LeaveSpecifications.active())
                .and(LeaveSpecifications.employeeId(scope))
                .and(LeaveSpecifications.status(status))
                .and(LeaveSpecifications.type(type))
                .and(LeaveSpecifications.from(from))
                .and(LeaveSpecifications.to(to));
        return leaveRequestRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LeaveResponse get(Long id, Long currentUserId, boolean manager) {
        LeaveRequest leave = findActive(id);
        Long ownerId = leave.getEmployee().getId();
        if (!manager && !ownerId.equals(employeeOf(currentUserId).getId())) {
            throw notFound();
        }
        return toResponse(leave);
    }

    @Transactional
    public LeaveResponse create(LeaveRequestRecord request, Long currentUserId) {
        Employee employee = employeeOf(currentUserId);
        validateDates(request.startDate(), request.endDate());
        ensureNoOverlap(employee.getId(), request.startDate(), request.endDate());
        LeaveRequest leave = LeaveRequest.builder()
                .employee(employee)
                .type(request.type())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(trimToNull(request.reason()))
                .build();
        LeaveRequest saved = leaveRequestRepository.save(leave);
        notifyManagers("%s %s a demandé des congés (%s) du %s au %s".formatted(
                employee.getUser().getFirstName(), employee.getUser().getLastName(),
                request.type(), request.startDate().format(DATE_FORMAT), request.endDate().format(DATE_FORMAT)));
        return toResponse(saved);
    }

    @Transactional
    public LeaveResponse review(Long id, LeaveReviewRequest request, Long currentUserId) {
        LeaveRequest leave = findActive(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "LEAVE_NOT_PENDING",
                    "Seule une demande en attente peut être traitée");
        }
        if (request.status() != LeaveStatus.APPROVED && request.status() != LeaveStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LEAVE_INVALID_REVIEW",
                    "La décision doit être APPROVED ou REJECTED");
        }
        Employee reviewer = employeeOf(currentUserId);
        leave.setStatus(request.status());
        leave.setReviewer(reviewer);
        leave.setReviewDate(Instant.now());
        leave.setReviewComment(trimToNull(request.comment()));
        LeaveRequest saved = leaveRequestRepository.save(leave);
        String decision = request.status() == LeaveStatus.APPROVED ? "approuvée" : "rejetée";
        String comment = leave.getReviewComment() == null ? "" : " : " + leave.getReviewComment();
        notificationService.create(leave.getEmployee().getUser().getId(), NotificationType.LEAVE_REVIEW,
                "Votre demande de congés (%s) a été %s%s".formatted(
                        leave.getType(), decision, comment));
        return toResponse(saved);
    }

    @Transactional
    public void cancel(Long id, Long currentUserId, boolean manager) {
        LeaveRequest leave = findActive(id);
        Long ownerId = leave.getEmployee().getId();
        if (!manager && !ownerId.equals(employeeOf(currentUserId).getId())) {
            throw notFound();
        }
        if (!manager && leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "LEAVE_NOT_CANCELLABLE",
                    "Une demande déjà traitée ne peut plus être annulée");
        }
        if (!manager) {
            leave.setStatus(LeaveStatus.CANCELLED);
        }
        leave.setDeletedAt(Instant.now());
        leaveRequestRepository.save(leave);
    }

    private LeaveRequest findActive(Long id) {
        return leaveRequestRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(this::notFound);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "LEAVE_NOT_FOUND", "Demande de congés introuvable");
    }

    private Employee employeeOf(Long userId) {
        return employeeRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "LEAVE_EMPLOYEE_REQUIRED",
                        "Aucun profil employé n'est associé à ce compte"));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LEAVE_INVALID_DATES",
                    "La date de fin doit être postérieure ou égale à la date de début");
        }
    }

    private void ensureNoOverlap(Long employeeId, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> active = leaveRequestRepository.findByEmployeeIdAndStatusInAndDeletedAtIsNull(
                employeeId, List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED));
        boolean overlap = active.stream().anyMatch(existing ->
                !startDate.isAfter(existing.getEndDate()) && !endDate.isBefore(existing.getStartDate()));
        if (overlap) {
            throw new ApiException(HttpStatus.CONFLICT, "LEAVE_OVERLAP",
                    "La période demandée chevauche une demande existante");
        }
    }

    private void notifyManagers(String message) {
        userRepository.findByRoleInAndDeletedAtIsNull(MANAGER_ROLES)
                .forEach(user -> notificationService.create(user.getId(), NotificationType.LEAVE_REQUEST, message));
    }

    private LeaveResponse toResponse(LeaveRequest leave) {
        Employee reviewer = leave.getReviewer();
        return LeaveResponse.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getUser().getFirstName() + " " + leave.getEmployee().getUser().getLastName())
                .type(leave.getType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .status(leave.getStatus())
                .reason(leave.getReason())
                .reviewerId(reviewer == null ? null : reviewer.getId())
                .reviewerName(reviewer == null ? null
                        : reviewer.getUser().getFirstName() + " " + reviewer.getUser().getLastName())
                .reviewDate(leave.getReviewDate())
                .reviewComment(leave.getReviewComment())
                .createdAt(leave.getCreatedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
