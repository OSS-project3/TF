package com.example.teamflow.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "프로젝트 멤버 일괄 교체 요청 — 기존 멤버 전체 제거 후 재설정")
public record ProjectMemberReplaceRequest(
        @Schema(description = "새 멤버 ID 목록 (빈 배열이면 전원 제거)", example = "[1, 2, 4]")
        @NotNull List<Long> memberIds
) {}
