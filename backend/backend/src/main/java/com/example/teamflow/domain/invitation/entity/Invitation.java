package com.example.teamflow.domain.invitation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitation")
@Getter
@NoArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    private Long workspaceId;

    private Long createdByMemberId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    public static Invitation create(Long workspaceId, Long createdByMemberId) {
        Invitation inv = new Invitation();
        inv.token = UUID.randomUUID().toString();
        inv.workspaceId = workspaceId;
        inv.createdByMemberId = createdByMemberId;
        inv.expiresAt = LocalDateTime.now().plusDays(7);
        return inv;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void consume() {
        this.usedAt = LocalDateTime.now();
    }
}
