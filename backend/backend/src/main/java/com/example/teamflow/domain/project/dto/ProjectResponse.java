package com.example.teamflow.domain.project.dto;

import com.example.teamflow.common.enums.ProjectHealth;
import com.example.teamflow.domain.project.entity.Project;
import com.example.teamflow.domain.task.dto.TaskAggregation;

import java.time.LocalDate;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String goal,
        LocalDate deadline,
        String status,
        double progress,
        List<Long> memberIds,
        int taskCount,
        int doneTaskCount,
        int lateTaskCount,
        String health
) {
    public static ProjectResponse of(Project project, List<Long> memberIds, TaskAggregation agg) {
        double progress = agg.total() == 0 ? 0.0 : (double) agg.done() / agg.total();
        ProjectHealth health = computeHealth(agg, progress);
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getGoal(),
                project.getDeadline(),
                project.getStatus().name(),
                Math.round(progress * 100.0) / 100.0,
                memberIds,
                agg.total(),
                agg.done(),
                agg.late(),
                health.name()
        );
    }

    private static ProjectHealth computeHealth(TaskAggregation agg, double progress) {
        if (agg.total() == 0) return ProjectHealth.IDLE;
        if (agg.late() > 0 && progress < 0.3) return ProjectHealth.BAD;
        if (agg.late() > 0 || progress < 0.5) return ProjectHealth.WARN;
        return ProjectHealth.OK;
    }
}
