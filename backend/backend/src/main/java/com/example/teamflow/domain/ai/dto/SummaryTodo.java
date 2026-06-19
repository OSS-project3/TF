package com.example.teamflow.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI가 추출한 액션 아이템")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SummaryTodo(
        @Schema(description = "할 일 내용") String title,
        @Schema(description = "담당자 이름 힌트 (없으면 빈 문자열)") String assignee,
        @Schema(description = "기한 yyyy-MM-dd (없으면 빈 문자열)") String dueDate
) {}
