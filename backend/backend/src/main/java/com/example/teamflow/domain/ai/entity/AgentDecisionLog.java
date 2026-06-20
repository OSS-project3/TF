package com.example.teamflow.domain.ai.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import com.example.teamflow.common.enums.AgentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "agent_decision_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentDecisionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 30)
    private AgentType agentType;

    // RiskAgent가 반환한 risks JSON 배열을 전체 저장
    @Column(name = "risks_json", columnDefinition = "TEXT")
    private String risksJson;

    public static AgentDecisionLog create(Long projectId, AgentType agentType, String risksJson) {
        AgentDecisionLog log = new AgentDecisionLog();
        log.projectId = projectId;
        log.agentType = agentType;
        log.risksJson = risksJson;
        return log;
    }
}
