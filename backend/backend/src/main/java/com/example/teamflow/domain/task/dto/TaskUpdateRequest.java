package com.example.teamflow.domain.task.dto;

import com.example.teamflow.common.enums.TaskDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "태스크 수정 요청 — 모든 필드 선택적, null은 수정하지 않음")
public record TaskUpdateRequest(
        @Schema(description = "새 제목", example = "로그인 API 구현 완료") String title,
        @Schema(description = "새 페이즈", example = "QA") String phase,
        @Schema(description = "새 예상 소요 시간(시간)", example = "12") Integer estimatedHours,
        @Schema(description = "새 난이도", allowableValues = {"EASY", "MEDIUM", "HARD"}) TaskDifficulty difficulty,
        @Schema(description = "새 시작일 (yyyy-MM-dd)", example = "2026-06-15") LocalDate startDate,
        @Schema(description = "새 마감일 (yyyy-MM-dd)", example = "2026-06-25") LocalDate endDate,
        @Schema(description = "크리티컬 패스 여부") Boolean isCriticalPath,
        @Schema(description = "지연 위험 여부") Boolean isLateRisk,
        @Schema(description = "연동할 GitHub 브랜치명 (머지 시 자동 완료)", example = "feat/user-crud") String gitBranch
) {}
