package com.apprh.backend.departments.infrastructure;

import com.apprh.backend.departments.domain.Department;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DepartmentSpecifications {

    private DepartmentSpecifications() {
    }

    public static Specification<Department> activeAndFiltered(String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate name = cb.like(cb.lower(root.get("name")), pattern);
                Predicate description = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(name, description));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
