package com.example.teamflow.domain.task.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_dependency",
       uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "prerequisite_task_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "prerequisite_task_id", nullable = false)
    private Long prerequisiteTaskId;

    public static TaskDependency create(Long taskId, Long prerequisiteTaskId) {
        TaskDependency td = new TaskDependency();
        td.taskId = taskId;
        td.prerequisiteTaskId = prerequisiteTaskId;
        return td;
    }
}
