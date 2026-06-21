package com.example.teamflow.domain.ai.dto;

import com.example.teamflow.common.enums.AgentType;

import java.time.LocalDateTime;

public record AiActivityResponse(
        Long id,
        AgentType agentType,
        String label,
        Long projectId,
        String projectName,
        LocalDateTime createdAt
) {}
