package com.example.teamflow.domain.task.service;

import com.example.teamflow.common.enums.TaskStatus;
import com.example.teamflow.domain.task.dto.TaskAggregation;
import com.example.teamflow.domain.task.dto.TaskSummary;
import com.example.teamflow.domain.task.entity.Task;
import com.example.teamflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskAggregationService {

    private final TaskRepository taskRepository;

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
}
