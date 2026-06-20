package com.example.teamflow.domain.ai.dto;

public record AiAgentResult<T>(
        T data,
        String promptUsed,
        String rawResponse,
        int tokenUsage
) {}
