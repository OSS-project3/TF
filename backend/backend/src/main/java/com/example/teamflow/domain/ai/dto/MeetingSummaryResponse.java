package com.example.teamflow.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 회의 요약 응답")
@JsonIgnoreProperties(ignoreUnknown = true)
public record MeetingSummaryResponse(
        @Schema(description = "핵심 요약 문장 목록") List<String> summary,
        @Schema(description = "액션 아이템 목록") List<SummaryTodo> todos
) {}
