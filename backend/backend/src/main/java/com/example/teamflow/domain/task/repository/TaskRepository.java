package com.example.teamflow.domain.task.repository;

import com.example.teamflow.common.enums.TaskStatus;
import com.example.teamflow.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId " +
           "AND (:assigneeId IS NULL OR t.assigneeId = :assigneeId) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:phase IS NULL OR t.phase = :phase)")
    List<Task> findByProjectIdWithFilters(
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId,
            @Param("status") TaskStatus status,
            @Param("phase") String phase);

    int countByProjectId(Long projectId);

    int countByProjectIdAndStatus(Long projectId, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.projectId = :projectId " +
           "AND t.endDate < :today AND t.status <> 'DONE'")
    int countLate(@Param("projectId") Long projectId, @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.assigneeId = :memberId " +
           "AND (:status IS NULL OR t.status = :status)")
    List<Task> findByAssigneeId(@Param("memberId") Long memberId,
                                @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.assigneeId = :memberId " +
           "AND t.startDate <= :to AND t.endDate >= :from")
    List<Task> findByAssigneeAndDateRange(@Param("memberId") Long memberId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);
}
