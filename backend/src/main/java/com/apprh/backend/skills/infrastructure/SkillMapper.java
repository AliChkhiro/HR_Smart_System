package com.apprh.backend.skills.infrastructure;

import com.apprh.backend.skills.api.SkillResponse;
import com.apprh.backend.skills.domain.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    @Mapping(target = "employeeCount", ignore = true)
    SkillResponse toResponse(Skill skill);
}
