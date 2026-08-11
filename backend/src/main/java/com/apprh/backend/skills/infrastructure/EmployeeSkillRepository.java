package com.apprh.backend.skills.infrastructure;

import com.apprh.backend.skills.domain.EmployeeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, Long> {

    List<EmployeeSkill> findAllByEmployeeId(Long employeeId);

    List<EmployeeSkill> findAllByEmployeeIdIn(Collection<Long> employeeIds);

    boolean existsByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    Optional<EmployeeSkill> findByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    void deleteAllByEmployeeId(Long employeeId);

    long countBySkillId(Long skillId);
}
