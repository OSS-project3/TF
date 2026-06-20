package com.example.teamflow.domain.member.dto;

import com.example.teamflow.common.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "회원가입 요청")
public record RegisterRequest(
        @Schema(description = "이름", example = "김민서") @NotBlank String name,
        @Schema(description = "이메일", example = "minseo@teamflow.dev") @Email @NotBlank String email,
        @Schema(description = "비밀번호 (8자 이상)", example = "password123") @NotBlank @Size(min = 8) String password,
        @Schema(description = "역할", example = "PM") @NotNull MemberRole role,
        @Schema(description = "이니셜 (최대 2자)", example = "민") @NotBlank @Size(max = 2) String initial,
        @Schema(description = "주간 가용 시간(h)", example = "40") @Min(1) int weeklyCapacityHours,
        @Schema(description = "보유 스킬 목록", example = "[\"기획\", \"문서\"]") List<String> skills,
        @Schema(description = "초대 토큰 (초대받은 경우)", example = "550e8400-e29b-41d4-a716-446655440000") String inviteToken
) {}
