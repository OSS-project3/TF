package com.example.teamflow.domain.invitation.service;

import com.example.teamflow.common.enums.MemberInviteStatus;
import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.invitation.dto.ReceivedInviteResponse;
import com.example.teamflow.domain.invitation.entity.MemberInvitation;
import com.example.teamflow.domain.invitation.repository.MemberInvitationRepository;
import com.example.teamflow.domain.member.dto.LoginResponse;
import com.example.teamflow.domain.member.entity.Member;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.workspace.entity.Workspace;
import com.example.teamflow.domain.workspace.service.WorkspaceService;
import com.example.teamflow.infra.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이메일 기반 팀원 초대(참가 요청) 서비스.
 * - PM이 이메일로 초대를 보내면 대상 멤버의 계정에 PENDING 요청이 생성된다.
 * - 초대받은 멤버는 본인 계정에서 요청을 조회하고 수락/거절한다.
 * - 수락 시 멤버의 workspaceId 가 초대 워크스페이스로 전환되고 새 JWT가 발급된다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MemberInviteService {

    private final MemberInvitationRepository memberInvitationRepository;
    private final MemberService memberService;
    private final WorkspaceService workspaceService;
    private final JwtTokenProvider jwtTokenProvider;

    /** PM이 이메일로 팀원을 초대한다. */
    public void invite(Long workspaceId, Long inviterMemberId, String email) {
        Member inviter = memberService.findById(inviterMemberId);
        Member invitee = memberService.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_TARGET_NOT_FOUND));

        if (invitee.getId().equals(inviterMemberId)) {
            throw new BusinessException(ErrorCode.INVITE_SELF);
        }
        if (workspaceId.equals(invitee.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.INVITE_ALREADY_MEMBER);
        }
        if (memberInvitationRepository.existsByWorkspaceIdAndInviteeMemberIdAndStatus(
                workspaceId, invitee.getId(), MemberInviteStatus.PENDING)) {
            throw new BusinessException(ErrorCode.INVITE_ALREADY_SENT);
        }

        Workspace workspace = workspaceService.getById(workspaceId);
        memberInvitationRepository.save(MemberInvitation.create(
                workspaceId, workspace.getName(),
                inviterMemberId, inviter.getName(),
                invitee.getId(), invitee.getEmail()));
    }

    /** 내가 받은 미처리(PENDING) 참가 요청 목록. */
    @Transactional(readOnly = true)
    public List<ReceivedInviteResponse> listReceived(Long memberId) {
        return memberInvitationRepository
                .findByInviteeMemberIdAndStatusOrderByCreatedAtDesc(memberId, MemberInviteStatus.PENDING)
                .stream()
                .map(ReceivedInviteResponse::from)
                .toList();
    }

    /** 참가 요청 수락 → 워크스페이스 전환 + 새 JWT 발급. */
    public LoginResponse accept(Long inviteId, Long memberId) {
        MemberInvitation invitation = findOwnedPending(inviteId, memberId);
        invitation.accept();

        Member member = memberService.findById(memberId);
        member.setWorkspaceId(invitation.getWorkspaceId());

        String token = jwtTokenProvider.generateToken(
                memberId, member.getRole(), invitation.getWorkspaceId());
        return new LoginResponse(token);
    }

    /** 참가 요청 거절. */
    public void reject(Long inviteId, Long memberId) {
        MemberInvitation invitation = findOwnedPending(inviteId, memberId);
        invitation.reject();
    }

    private MemberInvitation findOwnedPending(Long inviteId, Long memberId) {
        MemberInvitation invitation = memberInvitationRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));
        if (!invitation.getInviteeMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVITE_FORBIDDEN);
        }
        if (!invitation.isPending()) {
            throw new BusinessException(ErrorCode.INVITE_NOT_PENDING);
        }
        return invitation;
    }
}
