package com.apprh.backend.employees.infrastructure;

import com.apprh.backend.employees.api.EmployeeResponse;
import com.apprh.backend.employees.api.EmployeeSkillResponse;
import com.apprh.backend.employees.domain.Employee;
import com.apprh.backend.skills.domain.EmployeeSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "skills", ignore = true)
    EmployeeResponse toResponse(Employee employee);

    @Mapping(target = "skillId", source = "skill.id")
    @Mapping(target = "skillName", source = "skill.name")
    @Mapping(target = "category", source = "skill.category")
    EmployeeSkillResponse toSkillResponse(EmployeeSkill employeeSkill);
}
