package com.example.teamflow.domain.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "태스크 담당자 변경 요청")
public record TaskAssigneeUpdateRequest(
        @Schema(description = "새 담당자 멤버 ID", example = "3") @NotNull Long assigneeId
) {}
