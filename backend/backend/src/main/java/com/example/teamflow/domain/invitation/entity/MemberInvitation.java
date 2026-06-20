package com.example.teamflow.domain.invitation.entity;

import com.example.teamflow.common.enums.MemberInviteStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이메일 기반 팀원 초대(참가 요청).
 * 토큰 링크 방식인 {@link Invitation} 과 달리, 특정 계정(이메일)을 대상으로 직접 발송한다.
 * 초대받은 멤버는 본인 계정에서 PENDING 요청을 조회하고 수락/거절할 수 있다.
 * 수락 시 멤버의 workspaceId 가 초대한 워크스페이스로 전환된다.
 */
@Entity
@Table(name = "member_invitation")
@Getter
@NoArgsConstructor
public class MemberInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 초대 대상 워크스페이스 */
    private Long workspaceId;

    /** 표시용 워크스페이스 이름 (생성 시점 스냅샷) */
    private String workspaceName;

    /** 초대를 보낸 PM */
    private Long inviterMemberId;

    /** 표시용 초대자 이름 (생성 시점 스냅샷) */
    private String inviterName;

    /** 초대 대상 멤버 */
    private Long inviteeMemberId;

    /** 초대 대상 이메일 */
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberInviteStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    public static MemberInvitation create(Long workspaceId, String workspaceName,
                                          Long inviterMemberId, String inviterName,
                                          Long inviteeMemberId, String inviteeEmail) {
        MemberInvitation inv = new MemberInvitation();
        inv.workspaceId = workspaceId;
        inv.workspaceName = workspaceName;
        inv.inviterMemberId = inviterMemberId;
        inv.inviterName = inviterName;
        inv.inviteeMemberId = inviteeMemberId;
        inv.inviteeEmail = inviteeEmail;
        inv.status = MemberInviteStatus.PENDING;
        inv.createdAt = LocalDateTime.now();
        return inv;
    }

    public boolean isPending() {
        return status == MemberInviteStatus.PENDING;
    }

    public void accept() {
        this.status = MemberInviteStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = MemberInviteStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }
}
