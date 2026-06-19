package com.example.teamflow.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 작업 분해 응답")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecomposeResponse(
        @Schema(description = "AI 분석 메시지 목록") List<String> reasoningMessages,
        @Schema(description = "제안된 작업 목록") List<DecomposedTask> tasks
) {}
