package com.example.teamflow.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "프로젝트 수정 요청 — 모든 필드 선택적, null은 수정하지 않음")
public record ProjectUpdateRequest(
        @Schema(description = "새 프로젝트 이름", example = "TeamFlow v2.1") @Size(max = 100, message = "프로젝트 이름은 100자 이내여야 합니다.") String name,
        @Schema(description = "새 목표", example = "기능 개선") @Size(max = 10000, message = "프로젝트 목표는 10000자 이내여야 합니다.") String goal,
        @Schema(description = "새 마감일 (yyyy-MM-dd)", example = "2027-03-01") @FutureOrPresent(message = "마감일은 오늘 이후여야 합니다.") LocalDate deadline
) {}
