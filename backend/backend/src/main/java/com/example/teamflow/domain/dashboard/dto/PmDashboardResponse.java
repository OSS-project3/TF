package com.example.teamflow.domain.dashboard.dto;

import com.example.teamflow.domain.project.dto.ProjectResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "PM 대시보드 응답")
public record PmDashboardResponse(
        @Schema(description = "활성 프로젝트 수", example = "3") int activeProjectCount,
        @Schema(description = "전체 프로젝트 평균 진행률 (0.0 ~ 1.0)", example = "0.52") double averageProgress,
        @Schema(description = "전체 태스크 수", example = "42") int totalTaskCount,
        @Schema(description = "완료된 태스크 수", example = "20") int doneTaskCount,
        @Schema(description = "지연된 태스크 수", example = "3") int lateTaskCount,
        @Schema(description = "팀 멤버 수", example = "5") int memberCount,
        @Schema(description = "팀 평균 부하율 (이번 주 기준)", example = "0.68") double averageLoadRate,
        @Schema(description = "활성 프로젝트 목록") List<ProjectResponse> projects
) {}
