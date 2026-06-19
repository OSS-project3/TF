package com.example.teamflow.domain.meeting.dto;

import com.example.teamflow.domain.meeting.entity.Meeting;
import com.example.teamflow.domain.meeting.entity.MeetingAttendee;
import com.example.teamflow.domain.meeting.entity.MeetingSummaryItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "회의록 응답")
public record MeetingResponse(
        @Schema(description = "회의록 ID") Long id,
        @Schema(description = "회의 제목") String title,
        @Schema(description = "회의 날짜 (yyyy-MM-dd)") LocalDate date,
        @Schema(description = "참석자 멤버 ID 목록") List<Long> attendeeMemberIds,
        @Schema(description = "회의 원문 노트") String notes,
        @Schema(description = "AI 요약 항목 목록 (순서 있음)") List<String> summary,
        @Schema(description = "액션 아이템(TODO) 목록") List<MeetingTodoResponse> todos,
        @Schema(description = "수기 등록 여부") boolean manual
) {
    public static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDate(),
                meeting.getAttendees().stream().map(MeetingAttendee::getMemberId).toList(),
                meeting.getNotes(),
                meeting.getSummaryItems().stream().map(MeetingSummaryItem::getContent).toList(),
                meeting.getTodos().stream().map(MeetingTodoResponse::from).toList(),
                meeting.isManual()
        );
    }
}
