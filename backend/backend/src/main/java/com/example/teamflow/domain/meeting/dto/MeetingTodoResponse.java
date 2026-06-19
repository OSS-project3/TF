package com.example.teamflow.domain.meeting.dto;

import com.example.teamflow.domain.meeting.entity.MeetingTodo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "회의 TODO 항목")
public record MeetingTodoResponse(
        @Schema(description = "TODO ID") Long id,
        @Schema(description = "담당자 멤버 ID") Long assigneeId,
        @Schema(description = "대상 프로젝트 ID") Long projectId,
        @Schema(description = "할 일 내용") String title,
        @Schema(description = "기한 (yyyy-MM-dd)") LocalDate dueDate,
        @Schema(description = "등록된 Task ID. null이면 미적용") Long appliedTaskId
) {
    public static MeetingTodoResponse from(MeetingTodo todo) {
        return new MeetingTodoResponse(
                todo.getId(),
                todo.getAssigneeId(),
                todo.getProjectId(),
                todo.getTitle(),
                todo.getDueDate(),
                todo.getAppliedTaskId()
        );
    }
}
