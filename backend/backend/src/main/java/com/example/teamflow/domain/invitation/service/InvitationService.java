package com.example.teamflow.domain.invitation.service;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.invitation.entity.Invitation;
import com.example.teamflow.domain.invitation.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;

    public String create(Long workspaceId, Long memberId) {
        Invitation invitation = Invitation.create(workspaceId, memberId);
        invitationRepository.save(invitation);
        return invitation.getToken();
    }

    public Long consume(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_INVALID));
        if (invitation.isUsed()) {
            throw new BusinessException(ErrorCode.INVITE_USED);
        }
        if (invitation.isExpired()) {
            throw new BusinessException(ErrorCode.INVITE_EXPIRED);
        }
        invitation.consume();
        return invitation.getWorkspaceId();
    }
}
