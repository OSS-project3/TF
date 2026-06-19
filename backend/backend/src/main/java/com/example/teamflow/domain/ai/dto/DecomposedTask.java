package com.example.teamflow.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI가 제안한 작업 (DB 저장 안 됨)")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecomposedTask(
        @Schema(description = "작업 제목") String title,
        @Schema(description = "단계(phase)", example = "개발") String phase,
        @Schema(description = "예상 시간(h)", example = "8") int estimatedHours,
        @Schema(description = "난이도", example = "MEDIUM") String difficulty,
        @Schema(description = "선행 작업 인덱스(보통 빈 배열)") List<Long> dependencyTaskIds
) {}
