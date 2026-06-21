package com.example.teamflow.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "AI 태스크 분해 단일 호출 요청")
public record AiDecomposeRequest(
        @Schema(description = "프로젝트 목표 또는 기능 설명", example = "팀 일정 관리 시스템 개발") @NotBlank String goal,
        @Schema(description = "마감일 (yyyy-MM-dd)", example = "2026-08-31") String deadline,
        @Schema(description = "참여 멤버 ID 목록", example = "[1, 2, 3]") List<Long> memberIds,
        @Schema(description = "현재 구현 현황 (AI 단계·일정 결정에 활용)", example = "프론트엔드(UI) 완료. 백엔드 API 개발 필요.") String projectContext
) {}
