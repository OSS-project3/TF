package com.example.teamflow.domain.member.dto;

import com.example.teamflow.common.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "내 프로필 수정 요청 — null 필드는 변경하지 않음")
public record MemberUpdateRequest(
        @Schema(description = "변경할 이름", example = "김민서") @Size(min = 1) String name,
        @Schema(description = "변경할 이니셜 (최대 2자)", example = "민") @Size(max = 2) String initial,
        @Schema(description = "변경할 주간 가용 시간(h)", example = "35") @Min(1) Integer weeklyCapacityHours,
        @Schema(description = "변경할 스킬 목록 (null이면 기존 유지, 빈 배열이면 전체 삭제)", example = "[\"React\", \"TypeScript\"]") List<String> skills,
        @Schema(description = "변경할 역할", example = "PM") MemberRole role
) {}
