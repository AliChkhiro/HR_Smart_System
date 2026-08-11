package com.apprh.backend.tasks.infrastructure;

import com.apprh.backend.tasks.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndDeletedAtIsNull(Long id);
}
