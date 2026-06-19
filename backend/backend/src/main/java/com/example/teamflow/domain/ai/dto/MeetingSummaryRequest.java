package com.example.teamflow.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 회의 요약 요청")
public record MeetingSummaryRequest(
        @Schema(description = "회의 원문 노트") String notes,
        @Schema(description = "대상 프로젝트 ID (선택)", example = "1") Long projectId
) {}
