package com.example.teamflow.domain.task.dto;

import com.example.teamflow.common.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "태스크 상태 변경 요청")
public record TaskStatusUpdateRequest(
        @Schema(description = "변경할 상태", allowableValues = {"TODO", "IN_PROGRESS", "DONE", "BLOCKED"}, example = "IN_PROGRESS")
        @NotNull TaskStatus status
) {}
