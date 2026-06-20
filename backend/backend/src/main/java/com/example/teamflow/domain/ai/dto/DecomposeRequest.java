package com.example.teamflow.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "AI 작업 분해 요청")
public record DecomposeRequest(
        @Schema(description = "프로젝트 목표", example = "결제 성공률 12% 향상") @NotBlank String goal,
        @Schema(description = "마감일", example = "2026-08-01") LocalDate deadline,
        @Schema(description = "참여 멤버 ID 목록", example = "[1, 2, 3]") List<Long> memberIds
) {}
