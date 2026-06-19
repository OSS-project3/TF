package com.example.teamflow.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "멤버 워크로드 응답")
public record WorkloadResponse(
        @Schema(description = "멤버 ID", example = "2") Long memberId,
        @Schema(description = "주간 가용 시간(h)", example = "40") int capacityHours,
        @Schema(description = "배정된 태스크 예상 시간 합계(h)", example = "28") int assignedHours,
        @Schema(description = "부하율 (assignedHours / capacityHours)", example = "0.7") double loadRate,
        @Schema(description = "참여 프로젝트 수", example = "2") int projectCount,
        @Schema(description = "배정된 태스크 수", example = "5") int taskCount
) {}
