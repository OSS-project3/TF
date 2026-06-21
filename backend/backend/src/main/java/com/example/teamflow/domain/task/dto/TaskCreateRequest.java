package com.example.teamflow.domain.task.dto;

import com.example.teamflow.common.enums.TaskDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "태스크 생성 요청")
public record TaskCreateRequest(
        @Schema(description = "태스크 제목", example = "로그인 API 구현") @NotBlank String title,
        @Schema(description = "페이즈(단계) 이름", example = "개발") @NotBlank String phase,
        @Schema(description = "예상 소요 시간(시간, 선택)", example = "8") Integer estimatedHours,
        @Schema(description = "난이도", allowableValues = {"EASY", "MEDIUM", "HARD"}, example = "MEDIUM")
        @NotNull TaskDifficulty difficulty,
        @Schema(description = "담당자 멤버 ID 목록 (미지정 가능)", example = "[2, 3]") List<Long> assigneeIds,
        @Schema(description = "시작일 (yyyy-MM-dd)", example = "2026-06-10") LocalDate startDate,
        @Schema(description = "마감일 (yyyy-MM-dd)", example = "2026-06-20") LocalDate endDate,
        @Schema(description = "선행 태스크 ID 목록", example = "[5, 6]") List<Long> dependencyTaskIds,
        @Schema(description = "연동할 GitHub 브랜치명 (머지 시 자동 완료)", example = "feat/user-crud") String gitBranch
) {}
