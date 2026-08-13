package com.apprh.backend.tasks.infrastructure;

import com.apprh.backend.tasks.domain.Task;
import com.apprh.backend.tasks.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select t.status, count(t) from Task t
            where t.deletedAt is null
            group by t.status
            """)
    List<Object[]> countByStatus();

    @Query("""
            select t from Task t
            where t.deletedAt is null and t.status <> :done and t.dueDate is not null and t.dueDate < :today
            order by t.dueDate asc
            """)
    List<Task> findOverdue(@Param("done") TaskStatus done, @Param("today") LocalDate today);

    @Query("""
            select t from Task t
            where t.deletedAt is null and t.status <> :done and t.dueDate is not null
              and t.dueDate >= :from and t.dueDate <= :to
            order by t.dueDate asc
            """)
    List<Task> findDueBetween(@Param("done") TaskStatus done, @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    @Query("""
            select t.assignee.id, t.status, count(t) from Task t
            where t.assignee.id in :ids and t.deletedAt is null
            group by t.assignee.id, t.status
            """)
    List<Object[]> countGroupedByAssignee(@Param("ids") Collection<Long> ids);
}
