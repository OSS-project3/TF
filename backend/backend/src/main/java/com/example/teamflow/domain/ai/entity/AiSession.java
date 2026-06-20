package com.example.teamflow.domain.ai.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import com.example.teamflow.common.enums.AiSessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String feature;

    // RequirementAgent가 생성한 질문 목록 (JSON 직렬화)
    @Column(columnDefinition = "TEXT")
    private String questions;

    // 사용자가 제출한 답변 (JSON 직렬화)
    @Column(columnDefinition = "TEXT")
    private String answers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiSessionStatus status;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static AiSession create(Long memberId, String feature, String questions) {
        AiSession session = new AiSession();
        session.memberId = memberId;
        session.feature = feature;
        session.questions = questions;
        session.status = AiSessionStatus.QUESTIONING;
        session.expiresAt = LocalDateTime.now().plusMinutes(30);
        return session;
    }

    public void startProcessing(String answers) {
        this.answers = answers;
        this.status = AiSessionStatus.PROCESSING;
    }

    public void complete(Long projectId) {
        this.projectId = projectId;
        this.status = AiSessionStatus.COMPLETED;
    }

    public void fail() {
        this.status = AiSessionStatus.FAILED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
