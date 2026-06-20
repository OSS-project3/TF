package com.example.teamflow.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 태스크 분해 결과")
public record AiDecomposeResponse(
        @Schema(description = "분해된 태스크 목록") List<TaskItem> tasks
) {
    @Schema(description = "AI가 제안한 태스크")
    public record TaskItem(
            @Schema(description = "태스크 제목") String title,
            @Schema(description = "단계명", example = "개발") String phase,
            @Schema(description = "예상 시간(h)", example = "4") int estimatedHours,
            @Schema(description = "난이도", example = "MEDIUM") String difficulty,
            @Schema(description = "AI가 산정한 시작일 (yyyy-MM-dd)", example = "2026-06-21") String startDate,
            @Schema(description = "AI가 산정한 마감일 (yyyy-MM-dd)", example = "2026-06-24") String endDate
    ) {}
}
