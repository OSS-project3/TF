package com.example.teamflow.domain.task.repository;

import com.example.teamflow.domain.task.entity.TaskExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskExecutionLogRepository extends JpaRepository<TaskExecutionLog, Long> {

    // IN_PROGRESS 상태인 태스크 중, 특정 시점 이후 상태 변경이 있었던 taskId 목록
    @Query("SELECT DISTINCT l.taskId FROM TaskExecutionLog l " +
           "WHERE l.taskId IN :taskIds AND l.createdAt >= :since")
    List<Long> findTaskIdsWithRecentActivity(@Param("taskIds") List<Long> taskIds,
                                             @Param("since") LocalDateTime since);

    void deleteAllByTaskId(Long taskId);
}
