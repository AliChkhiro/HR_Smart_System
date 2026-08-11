package com.apprh.backend.projects.infrastructure;

import com.apprh.backend.projects.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByProjectId(Long projectId);

    List<ProjectMember> findAllByProjectIdIn(Collection<Long> projectIds);

    boolean existsByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    Optional<ProjectMember> findByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    void deleteAllByProjectId(Long projectId);

    long countByProjectId(Long projectId);
}
