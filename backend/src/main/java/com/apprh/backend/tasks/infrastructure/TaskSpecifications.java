package com.apprh.backend.tasks.infrastructure;

import com.apprh.backend.tasks.domain.Task;
import com.apprh.backend.tasks.domain.TaskPriority;
import com.apprh.backend.tasks.domain.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> activeAndFiltered(String search, Long projectId, Long assigneeId,
                                                        TaskStatus status, TaskPriority priority,
                                                        LocalDate dueFrom, LocalDate dueTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (dueFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
            }
            if (dueTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueTo));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
