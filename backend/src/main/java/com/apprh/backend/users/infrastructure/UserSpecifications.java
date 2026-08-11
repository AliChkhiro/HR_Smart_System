package com.apprh.backend.users.infrastructure;

import com.apprh.backend.users.domain.User;
import com.apprh.backend.users.domain.UserRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> activeAndFiltered(String search, UserRole role) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
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
