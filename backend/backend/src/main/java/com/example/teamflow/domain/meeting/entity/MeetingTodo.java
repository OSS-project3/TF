package com.example.teamflow.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "meeting_todo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingTodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "assignee_id", nullable = false)
    private Long assigneeId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "applied_task_id")
    private Long appliedTaskId;

    public static MeetingTodo create(Meeting meeting, Long assigneeId, Long projectId,
                                     String title, LocalDate dueDate) {
        MeetingTodo todo = new MeetingTodo();
        todo.meeting = meeting;
        todo.assigneeId = assigneeId;
        todo.projectId = projectId;
        todo.title = title;
        todo.dueDate = dueDate;
        return todo;
    }

    public void applyTask(Long taskId) {
        this.appliedTaskId = taskId;
    }
}
