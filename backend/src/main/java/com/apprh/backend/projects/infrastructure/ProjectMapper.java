package com.apprh.backend.projects.infrastructure;

import com.apprh.backend.projects.api.ProjectMemberResponse;
import com.apprh.backend.projects.api.ProjectResponse;
import com.apprh.backend.projects.domain.Project;
import com.apprh.backend.projects.domain.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "memberCount", ignore = true)
    ProjectResponse toResponse(Project project);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.user.firstName")
    @Mapping(target = "jobTitle", source = "employee.jobTitle")
    ProjectMemberResponse toMemberResponse(ProjectMember member);
}
