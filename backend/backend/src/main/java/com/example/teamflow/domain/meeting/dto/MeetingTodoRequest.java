package com.example.teamflow.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "회의 TODO 항목 요청")
public record MeetingTodoRequest(
        @Schema(description = "담당자 멤버 ID", example = "2") @NotNull Long assigneeId,
        @Schema(description = "대상 프로젝트 ID", example = "1") @NotNull Long projectId,
        @Schema(description = "할 일 내용", example = "결제 화면 컴포넌트 1차 완료") @NotBlank String title,
        @Schema(description = "기한 (yyyy-MM-dd)", example = "2026-05-27") LocalDate dueDate
) {}
