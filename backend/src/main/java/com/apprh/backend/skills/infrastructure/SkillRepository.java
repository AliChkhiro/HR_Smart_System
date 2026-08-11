package com.apprh.backend.skills.infrastructure;

import com.apprh.backend.skills.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long>, JpaSpecificationExecutor<Skill> {

    Optional<Skill> findByIdAndDeletedAtIsNull(Long id);

    Optional<Skill> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
