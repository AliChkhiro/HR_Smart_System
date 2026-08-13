package com.apprh.backend.projects.infrastructure;

import com.apprh.backend.projects.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    Optional<Project> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    long countByDeletedAtIsNull();
}
