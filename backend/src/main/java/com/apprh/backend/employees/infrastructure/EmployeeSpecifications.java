package com.apprh.backend.employees.infrastructure;

import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.employees.domain.EmployeeStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> activeAndFiltered(String search, Long departmentId, EmployeeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate firstName = cb.like(cb.lower(root.get("user").get("firstName")), pattern);
                Predicate lastName = cb.like(cb.lower(root.get("user").get("lastName")), pattern);
                Predicate email = cb.like(cb.lower(root.get("user").get("email")), pattern);
                Predicate jobTitle = cb.like(cb.lower(root.get("jobTitle")), pattern);
                predicates.add(cb.or(firstName, lastName, email, jobTitle));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
