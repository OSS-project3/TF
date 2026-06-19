package com.example.teamflow.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "팀 워크로드 응답 — 멤버 1인의 워크로드 요약")
public record TeamWorkloadResponse(
        @Schema(description = "멤버 ID", example = "2") Long memberId,
        @Schema(description = "멤버 이름", example = "김민준") String memberName,
        @Schema(description = "역할", example = "FRONTEND") String role,
        @Schema(description = "주간 가용 시간(h)", example = "40") int capacityHours,
        @Schema(description = "배정 시간(h)", example = "28") int assignedHours,
        @Schema(description = "부하율", example = "0.7") double loadRate,
        @Schema(description = "배정 태스크 수", example = "5") int taskCount,
        @Schema(description = "참여 프로젝트 수", example = "2") int projectCount,
        @Schema(description = "보유 스킬 목록", example = "[\"React\", \"TypeScript\"]") List<String> skills
) {}
