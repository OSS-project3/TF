package com.example.teamflow.domain.task.service;

import com.example.teamflow.common.enums.TaskStatus;
import com.example.teamflow.domain.task.dto.TaskAggregation;
import com.example.teamflow.domain.task.dto.TaskSummary;
import com.example.teamflow.domain.task.entity.Task;
import com.example.teamflow.domain.task.repository.TaskExecutionLogRepository;
import com.example.teamflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskAggregationService {

    private final TaskRepository taskRepository;
    private final TaskExecutionLogRepository taskExecutionLogRepository;

    @Transactional(readOnly = true)
    public TaskAggregation getAggregation(Long projectId) {
        int total = taskRepository.countByProjectId(projectId);
        int done = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);
        int late = taskRepository.countLate(projectId, LocalDate.now());
        return new TaskAggregation(total, done, late);
    }

    @Transactional(readOnly = true)
    public List<TaskSummary> findByAssigneeAndDateRange(Long memberId, LocalDate from, LocalDate to) {
        List<Task> tasks = (from != null && to != null)
                ? taskRepository.findByAssigneeAndDateRange(memberId, from, to)
                : taskRepository.findByAssigneeId(memberId, null);
        return tasks.stream()
                .map(t -> new TaskSummary(t.getEstimatedHours(), t.getProjectId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Task> findLateByProjectId(Long projectId) {
        return taskRepository.findLateByProjectId(projectId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Task> findBlockedByProjectId(Long projectId) {
        return taskRepository.findBlockedByProjectId(projectId);
    }

    // N일 이상 IN_PROGRESS 상태에서 변화 없는 태스크 (정체 태스크)
    @Transactional(readOnly = true)
    public List<Task> findStuckInProgress(Long projectId, int stuckDays) {
        List<Task> inProgressTasks = taskRepository.findInProgressByProjectId(projectId);
        if (inProgressTasks.isEmpty()) {
            return List.of();
        }

        List<Long> inProgressIds = inProgressTasks.stream().map(Task::getId).toList();
        LocalDateTime threshold = LocalDateTime.now().minusDays(stuckDays);

        // threshold 이후 변경 이력이 있는 taskId 집합
        Set<Long> recentlyChangedIds = taskExecutionLogRepository
                .findTaskIdsWithRecentActivity(inProgressIds, threshold)
                .stream().collect(Collectors.toSet());

        // 최근 변경 없는 것만 필터링 → 정체 태스크
        return inProgressTasks.stream()
                .filter(t -> !recentlyChangedIds.contains(t.getId()))
                .toList();
    }

    // 담당자별 IN_PROGRESS 태스크 수 (워크로드 불균형 감지용)
    @Transactional(readOnly = true)
    public List<Object[]> countInProgressByAssignee(Long projectId) {
        return taskRepository.countInProgressByAssignee(projectId);
    }
}
