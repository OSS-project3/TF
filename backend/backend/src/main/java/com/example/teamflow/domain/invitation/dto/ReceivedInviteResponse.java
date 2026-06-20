package com.example.teamflow.domain.invitation.dto;

import com.example.teamflow.domain.invitation.entity.MemberInvitation;

import java.time.LocalDateTime;

/** 초대받은 멤버가 본인 계정에서 조회하는 참가 요청 항목. */
public record ReceivedInviteResponse(
        Long id,
        String inviterName,
        String workspaceName,
        LocalDateTime createdAt
) {
    public static ReceivedInviteResponse from(MemberInvitation inv) {
        return new ReceivedInviteResponse(
                inv.getId(), inv.getInviterName(), inv.getWorkspaceName(), inv.getCreatedAt());
    }
}
