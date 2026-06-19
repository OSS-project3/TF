package com.example.teamflow.domain.project.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import com.example.teamflow.common.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String goal;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    public static Project create(String name, String goal, LocalDate deadline) {
        Project project = new Project();
        project.name = name;
        project.goal = goal;
        project.deadline = deadline;
        project.status = ProjectStatus.ACTIVE;
        return project;
    }

    public void updateName(String name) {
        if (name != null) this.name = name;
    }

    public void updateGoal(String goal) {
        if (goal != null) this.goal = goal;
    }

    public void updateDeadline(LocalDate deadline) {
        if (deadline != null) this.deadline = deadline;
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
    }
}
