package com.example.teamflow.domain.member.service;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.common.enums.MemberRole;
import com.example.teamflow.domain.invitation.service.InvitationService;
import com.example.teamflow.domain.invitation.service.MemberInviteService;
import com.example.teamflow.domain.member.dto.GoogleLoginRequest;
import com.example.teamflow.infra.google.GoogleAuthService;
import com.example.teamflow.infra.google.GoogleTokenInfo;
import com.example.teamflow.domain.member.dto.LoginRequest;
import com.example.teamflow.domain.member.dto.LoginResponse;
import com.example.teamflow.domain.member.dto.RegisterRequest;
import com.example.teamflow.domain.member.entity.Member;
import com.example.teamflow.domain.member.repository.MemberRepository;
import com.example.teamflow.domain.workspace.entity.Workspace;
import com.example.teamflow.domain.workspace.service.WorkspaceService;
import com.example.teamflow.infra.security.JwtTokenProvider;
import com.example.teamflow.infra.security.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final WorkspaceService workspaceService;
    private final InvitationService invitationService;
    private final MemberInviteService memberInviteService;
    private final GoogleAuthService googleAuthService;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (memberRepository.findByEmail(req.email()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 임시로 workspaceId 없이 저장한 뒤, workspaceId 결정 후 갱신
        String initial = (req.initial() != null && !req.initial().isBlank())
                ? req.initial()
                : req.name().substring(0, Math.min(2, req.name().length()));
        Member member = Member.create(
                req.name(), req.role(), initial,
                req.weeklyCapacityHours(), req.email(),
                passwordEncoder.encode(req.password()),
                null);
        List<String> skills = req.skills() != null ? req.skills() : List.of();
        skills.forEach(member::addSkill);
        memberRepository.save(member);

        // 워크스페이스 결정
        Long workspaceId;
        if (StringUtils.hasText(req.inviteToken())) {
            workspaceId = invitationService.consume(req.inviteToken());
        } else {
            Workspace workspace = workspaceService.create(req.name() + "의 워크스페이스", member.getId());
            workspaceId = workspace.getId();
        }
        member.setWorkspaceId(workspaceId);

        // 미가입 상태로 받은 이메일 초대 연결 (같은 워크스페이스면 자동 수락)
        memberInviteService.claimEmailInvitations(member.getEmail(), member.getId(), workspaceId);

        return new LoginResponse(jwtTokenProvider.generateToken(member.getId(), member.getRole(), workspaceId));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(member.getId(), member.getRole(), member.getWorkspaceId());
        return new LoginResponse(token);
    }

    @Transactional
    public LoginResponse acceptInvitation(String inviteToken, Long memberId) {
        Long workspaceId = invitationService.consume(inviteToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.setWorkspaceId(workspaceId);
        return new LoginResponse(jwtTokenProvider.generateToken(memberId, member.getRole(), workspaceId));
    }

    @Transactional
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenInfo info = googleAuthService.verify(request.idToken());
        String email = info.email();
        String name = StringUtils.hasText(info.name()) ? info.name()
                : StringUtils.hasText(info.givenName()) ? info.givenName()
                : email.split("@")[0];

        Optional<Member> existing = memberRepository.findByEmail(email);
        if (existing.isPresent()) {
            Member member = existing.get();
            return new LoginResponse(jwtTokenProvider.generateToken(
                    member.getId(), member.getRole(), member.getWorkspaceId()));
        }

        // 신규 사용자 — Google 계정으로 자동 가입 (역할은 /setup-role에서 선택)
        String initial = name.substring(0, Math.min(2, name.length()));
        Member member = Member.create(name, MemberRole.FRONTEND, initial, 40, email,
                UUID.randomUUID().toString(), null);
        memberRepository.save(member);

        Long workspaceId;
        if (StringUtils.hasText(request.inviteToken())) {
            workspaceId = invitationService.consume(request.inviteToken());
        } else {
            Workspace workspace = workspaceService.create(name + "의 워크스페이스", member.getId());
            workspaceId = workspace.getId();
        }
        member.setWorkspaceId(workspaceId);
        memberInviteService.claimEmailInvitations(email, member.getId(), workspaceId);

        return new LoginResponse(
                jwtTokenProvider.generateToken(member.getId(), member.getRole(), workspaceId),
                true);
    }

    public void logout(String token) {
        if (jwtTokenProvider.validateToken(token)) {
            tokenBlacklist.add(jwtTokenProvider.getJti(token), jwtTokenProvider.getExpiration(token));
        }
    }
}
