package com.example.teamflow.domain.invitation.repository;

import com.example.teamflow.common.enums.MemberInviteStatus;
import com.example.teamflow.domain.invitation.entity.MemberInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberInvitationRepository extends JpaRepository<MemberInvitation, Long> {

    List<MemberInvitation> findByInviteeMemberIdAndStatusOrderByCreatedAtDesc(
            Long inviteeMemberId, MemberInviteStatus status);

    boolean existsByWorkspaceIdAndInviteeMemberIdAndStatus(
            Long workspaceId, Long inviteeMemberId, MemberInviteStatus status);
}
