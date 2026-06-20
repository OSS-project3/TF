package com.example.teamflow.domain.task.dto;

import com.example.teamflow.domain.task.entity.Task;

import java.time.LocalDate;
import java.util.List;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String phase,
        int estimatedHours,
        String difficulty,
        String status,
        Long assigneeId,
        List<Long> dependencyTaskIds,
        LocalDate startDate,
        LocalDate endDate,
        boolean isCriticalPath,
        boolean isLateRisk,
        String gitBranch
) {
    public static TaskResponse of(Task task, List<Long> dependencyTaskIds) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getPhase(),
                task.getEstimatedHours(),
                task.getDifficulty().name(),
                task.getStatus().name(),
                task.getAssigneeId(),
                dependencyTaskIds,
                task.getStartDate(),
                task.getEndDate(),
                task.isCriticalPath(),
                task.isLateRisk(),
                task.getGitBranch()
        );
    }
}
