package com.example.teamflow.domain.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "태스크 담당자 변경 요청")
public record TaskAssigneeUpdateRequest(
        @Schema(description = "담당자 멤버 ID 목록", example = "[3, 5]") @NotNull List<Long> assigneeIds
) {}
