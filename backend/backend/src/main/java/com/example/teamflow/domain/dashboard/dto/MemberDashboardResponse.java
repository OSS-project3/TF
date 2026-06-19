package com.example.teamflow.domain.dashboard.dto;

import com.example.teamflow.domain.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "멤버 대시보드 응답")
public record MemberDashboardResponse(
        @Schema(description = "이번 주 부하율", example = "0.7") double loadRate,
        @Schema(description = "이번 주 배정 시간(h)", example = "28") int assignedHours,
        @Schema(description = "주간 가용 시간(h)", example = "40") int capacityHours,
        @Schema(description = "참여 중인 프로젝트 수", example = "2") int projectCount,
        @Schema(description = "배정된 태스크 수", example = "8") int taskCount,
        @Schema(description = "완료한 태스크 수", example = "3") int doneTaskCount,
        @Schema(description = "가장 가까운 마감일 (미완료 태스크 기준)", example = "2026-06-18") LocalDate nextDueDate,
        @Schema(description = "오늘 마감 태스크") List<TaskResponse> todayTasks,
        @Schema(description = "이번 주 마감 태스크 (오늘 제외)") List<TaskResponse> thisWeekTasks,
        @Schema(description = "이후 마감 태스크") List<TaskResponse> laterTasks,
        @Schema(description = "참여 중인 프로젝트 요약 목록") List<ProjectSummary> projects
) {}
