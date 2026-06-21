package com.example.teamflow.domain.task.repository;

import com.example.teamflow.common.enums.TaskStatus;
import com.example.teamflow.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.assigneeIds a WHERE t.projectId = :projectId " +
           "AND (:assigneeId IS NULL OR a = :assigneeId) " +
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

    @Query("SELECT DISTINCT t FROM Task t JOIN t.assigneeIds a WHERE a = :memberId " +
           "AND (:status IS NULL OR t.status = :status)")
    List<Task> findByAssigneeId(@Param("memberId") Long memberId,
                                @Param("status") TaskStatus status);

    @Query("SELECT DISTINCT t FROM Task t JOIN t.assigneeIds a WHERE a = :memberId " +
           "AND t.startDate <= :to AND t.endDate >= :from")
    List<Task> findByAssigneeAndDateRange(@Param("memberId") Long memberId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId " +
           "AND t.endDate < :today AND t.status <> 'DONE'")
    List<Task> findLateByProjectId(@Param("projectId") Long projectId,
                                   @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.status = 'BLOCKED'")
    List<Task> findBlockedByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.status = 'IN_PROGRESS'")
    List<Task> findInProgressByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT a, COUNT(t) FROM Task t JOIN t.assigneeIds a WHERE t.projectId = :projectId " +
           "AND t.status = 'IN_PROGRESS' GROUP BY a")
    List<Object[]> countInProgressByAssignee(@Param("projectId") Long projectId);

    @Query("SELECT t FROM Task t WHERE t.gitBranch = :gitBranch AND t.status <> 'DONE'")
    List<Task> findByGitBranchAndNotDone(@Param("gitBranch") String gitBranch);
}
