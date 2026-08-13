package com.apprh.backend.leaves.infrastructure;

import com.apprh.backend.leaves.domain.LeaveRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    Optional<LeaveRequest> findByIdAndDeletedAtIsNull(Long id);

    List<LeaveRequest> findByEmployeeIdAndStatusInAndDeletedAtIsNull(
            Long employeeId, List<com.apprh.backend.leaves.domain.LeaveStatus> statuses);

    long countByStatusAndDeletedAtIsNull(com.apprh.backend.leaves.domain.LeaveStatus status);

    List<LeaveRequest> findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
            com.apprh.backend.leaves.domain.LeaveStatus status, Pageable pageable);

    List<LeaveRequest> findAllByEmployeeIdInAndStatusInAndDeletedAtIsNull(
            Collection<Long> employeeIds, Collection<com.apprh.backend.leaves.domain.LeaveStatus> statuses);
}
