package com.apprh.backend.departments.infrastructure;

import com.apprh.backend.departments.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    Optional<Department> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
