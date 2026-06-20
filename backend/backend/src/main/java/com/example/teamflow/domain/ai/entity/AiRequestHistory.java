package com.example.teamflow.domain.ai.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import com.example.teamflow.common.enums.AgentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_request_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequestHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "project_id")
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 30)
    private AgentType agentType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String response;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    public static AiRequestHistory create(Long memberId, Long projectId, AgentType agentType,
                                          String prompt, String response, Integer tokenUsage) {
        AiRequestHistory h = new AiRequestHistory();
        h.memberId = memberId;
        h.projectId = projectId;
        h.agentType = agentType;
        h.prompt = prompt;
        h.response = response;
        h.tokenUsage = tokenUsage;
        return h;
    }
}
