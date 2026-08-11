package com.apprh.backend.projects.infrastructure;

import com.apprh.backend.projects.domain.Project;
import com.apprh.backend.projects.domain.ProjectPriority;
import com.apprh.backend.projects.domain.ProjectStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> activeAndFiltered(String search, ProjectStatus status, ProjectPriority priority) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
