package com.example.teamflow.domain.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_member",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public static ProjectMember create(Long projectId, Long memberId) {
        ProjectMember pm = new ProjectMember();
        pm.projectId = projectId;
        pm.memberId = memberId;
        return pm;
    }
}
