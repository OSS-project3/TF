package com.example.teamflow.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "프로젝트 생성 요청")
public record ProjectCreateRequest(
        @Schema(description = "프로젝트 이름", example = "TeamFlow v2") @NotBlank String name,
        @Schema(description = "프로젝트 목표", example = "2026년 상반기 팀 협업 툴 출시") @NotBlank String goal,
        @Schema(description = "마감일 (yyyy-MM-dd)", example = "2026-12-31") @NotNull LocalDate deadline,
        @Schema(description = "초기 멤버 ID 목록 (빈 배열 허용)", example = "[1, 2, 3]") List<Long> memberIds
) {}
