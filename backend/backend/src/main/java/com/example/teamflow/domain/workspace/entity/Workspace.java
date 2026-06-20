package com.example.teamflow.domain.workspace.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace")
@Getter
@NoArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Long createdByMemberId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static Workspace create(String name, Long createdByMemberId) {
        Workspace ws = new Workspace();
        ws.name = name;
        ws.createdByMemberId = createdByMemberId;
        ws.createdAt = LocalDateTime.now();
        return ws;
    }
}
