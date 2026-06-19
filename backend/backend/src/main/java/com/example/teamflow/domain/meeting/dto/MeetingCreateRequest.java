package com.example.teamflow.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "회의록 저장 요청 — AI 요약 결과 저장(manual=false)과 수기 등록(manual=true) 모두 이 엔드포인트 사용")
public record MeetingCreateRequest(
        @Schema(description = "회의 제목", example = "스프린트 체크인") @NotBlank String title,
        @Schema(description = "회의 날짜 (yyyy-MM-dd)", example = "2026-05-22") @NotNull LocalDate date,
        @Schema(description = "참석자 멤버 ID 목록", example = "[1, 2, 3, 4]") @NotNull List<Long> attendeeMemberIds,
        @Schema(description = "회의 원문 노트 (수기 등록 시 필수, AI 요약 시 선택)") String notes,
        @Schema(description = "AI 요약 항목 목록 (수기 등록 시 빈 배열 허용)") List<String> summary,
        @Schema(description = "액션 아이템(TODO) 목록") List<MeetingTodoRequest> todos,
        @Schema(description = "수기 등록 여부 (AI 요약=false, 수기=true)", example = "false") boolean manual
) {}
