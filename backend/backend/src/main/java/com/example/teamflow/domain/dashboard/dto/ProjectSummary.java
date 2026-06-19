package com.example.teamflow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대시보드용 프로젝트 요약")
public record ProjectSummary(
        @Schema(description = "프로젝트 ID", example = "1") Long id,
        @Schema(description = "프로젝트 이름", example = "TeamFlow MVP") String name,
        @Schema(description = "진행률 (0.0 ~ 1.0)", example = "0.65") double progress,
        @Schema(description = "프로젝트 상태", example = "OK") String health
) {}
