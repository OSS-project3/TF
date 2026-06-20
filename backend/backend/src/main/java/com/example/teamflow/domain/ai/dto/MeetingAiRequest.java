package com.example.teamflow.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회의 노트 직접 요약 요청")
public record MeetingAiRequest(
        @Schema(description = "회의 원문 노트", example = "오늘 스프린트 회의에서...") @NotBlank String notes,
        @Schema(description = "프로젝트 ID (입력 시 해당 팀원 이름을 AI 컨텍스트에 포함)", example = "1") Long projectId
) {}
