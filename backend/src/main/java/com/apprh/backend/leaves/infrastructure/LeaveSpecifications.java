package com.apprh.backend.leaves.infrastructure;

import com.apprh.backend.leaves.domain.LeaveRequest;
import com.apprh.backend.leaves.domain.LeaveStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class LeaveSpecifications {

    private LeaveSpecifications() {
    }

    public static Specification<LeaveRequest> active() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<LeaveRequest> employeeId(Long employeeId) {
        return (root, query, cb) -> employeeId == null ? cb.conjunction()
                : cb.equal(root.get("employee").get("id"), employeeId);
    }

    public static Specification<LeaveRequest> status(LeaveStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<LeaveRequest> type(com.apprh.backend.leaves.domain.LeaveType type) {
        return (root, query, cb) -> type == null ? cb.conjunction()
                : cb.equal(root.get("type"), type);
    }

    public static Specification<LeaveRequest> from(LocalDate from) {
        return (root, query, cb) -> from == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("endDate"), from);
    }

    public static Specification<LeaveRequest> to(LocalDate to) {
        return (root, query, cb) -> to == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("startDate"), to);
    }
}
