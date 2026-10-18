package com.apprh.backend.users.infrastructure;

import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import com.apprh.backend.users.domain.UserStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> activeAndFiltered(String search, UserRole role, boolean unassigned, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // Par défaut, les demandes d'inscription en attente (PENDING) ne figurent pas
                // dans la liste des comptes : elles sont traitées depuis l'écran de validation RH.
                predicates.add(cb.notEqual(root.get("status"), UserStatus.PENDING));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (unassigned) {
                Subquery<Long> subquery = query.subquery(Long.class);
                var employeeRoot = subquery.from(Employee.class);
                subquery.select(employeeRoot.get("id"));
                subquery.where(cb.equal(employeeRoot.get("user"), root),
                        cb.isNull(employeeRoot.get("deletedAt")));
                predicates.add(cb.not(cb.exists(subquery)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate email = cb.like(cb.lower(root.get("email")), pattern);
                Predicate firstName = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastName = cb.like(cb.lower(root.get("lastName")), pattern);
                predicates.add(cb.or(email, firstName, lastName));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
