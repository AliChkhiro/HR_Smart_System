package com.apprh.backend.employees.infrastructure;

import com.apprh.backend.employees.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByIdAndDeletedAtIsNull(Long id);

    Optional<Employee> findByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByUserIdAndDeletedAtIsNull(Long userId);

    long countByDepartmentIdAndDeletedAtIsNull(Long departmentId);

    @Query("""
            select e.department.id, count(e) from Employee e
            where e.department.id in :ids and e.deletedAt is null
            group by e.department.id
            """)
    List<Object[]> countByDepartmentIdInAndDeletedAtIsNull(Collection<Long> ids);
}
