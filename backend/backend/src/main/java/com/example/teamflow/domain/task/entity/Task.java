package com.example.teamflow.domain.task.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import com.example.teamflow.common.enums.TaskDifficulty;
import com.example.teamflow.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_assignee", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "assignee_id")
    private List<Long> assigneeIds = new ArrayList<>();

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(nullable = false)
    private int estimatedHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_critical_path", nullable = false)
    private boolean criticalPath;

    @Column(name = "is_late_risk", nullable = false)
    private boolean lateRisk;

    @Column(name = "git_branch", length = 100)
    private String gitBranch;

    public static Task create(Long projectId, String title, String phase,
                              int estimatedHours, TaskDifficulty difficulty,
                              List<Long> assigneeIds, LocalDate startDate, LocalDate endDate) {
        Task task = new Task();
        task.projectId = projectId;
        task.title = title;
        task.phase = phase;
        task.estimatedHours = estimatedHours;
        task.difficulty = difficulty;
        if (assigneeIds != null) task.assigneeIds.addAll(assigneeIds);
        task.startDate = startDate;
        task.endDate = endDate;
        task.status = TaskStatus.TODO;
        task.criticalPath = false;
        task.lateRisk = false;
        return task;
    }

    public void changeStatus(TaskStatus newStatus) {
        this.status = newStatus;
    }

    public void updateAssignees(List<Long> memberIds) {
        if (memberIds != null) {
            this.assigneeIds.clear();
            this.assigneeIds.addAll(memberIds);
        }
    }

    public void updateTitle(String title) {
        if (title != null) this.title = title;
    }

    public void updatePhase(String phase) {
        if (phase != null) this.phase = phase;
    }

    public void updateEstimatedHours(Integer hours) {
        if (hours != null) this.estimatedHours = hours;
    }

    public void updateDifficulty(TaskDifficulty difficulty) {
        if (difficulty != null) this.difficulty = difficulty;
    }

    public void updateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
    }

    public void updateFlags(Boolean criticalPath, Boolean lateRisk) {
        if (criticalPath != null) this.criticalPath = criticalPath;
        if (lateRisk != null) this.lateRisk = lateRisk;
    }

    public void linkGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }
}
