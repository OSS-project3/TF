package com.example.teamflow.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "AI 작업 분해 요청")
public record DecomposeRequest(
        @Schema(description = "프로젝트 목표", example = "반응형 이커머스 쇼핑몰 개발") String goal,
        @Schema(description = "마감일 (yyyy-MM-dd)", example = "2026-08-01") LocalDate deadline,
        @Schema(description = "참여 멤버 ID 목록 (역할 참고용)", example = "[1, 2, 3]") List<Long> memberIds
) {}
