package com.apprh.backend.departments.infrastructure;

import com.apprh.backend.departments.api.DepartmentResponse;
import com.apprh.backend.departments.domain.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", source = "manager.firstName")
    @Mapping(target = "employeeCount", ignore = true)
    DepartmentResponse toResponse(Department department);
}
