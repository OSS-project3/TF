package com.example.teamflow.domain.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** PM이 이메일로 팀원을 초대할 때의 요청 본문. */
public record MemberInviteRequest(
        @NotBlank @Email String email
) {}
