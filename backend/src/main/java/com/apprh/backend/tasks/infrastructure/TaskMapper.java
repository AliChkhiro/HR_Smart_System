package com.apprh.backend.tasks.infrastructure;

import com.apprh.backend.tasks.api.TaskResponse;
import com.apprh.backend.tasks.domain.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "assigneeId", source = "assignee.id")
    @Mapping(target = "assigneeName", source = "assignee.user.firstName")
    @Mapping(target = "skillIds", expression = "java(task.getSkills().stream().map(s -> s.getId()).toList())")
    TaskResponse toResponse(Task task);
}
