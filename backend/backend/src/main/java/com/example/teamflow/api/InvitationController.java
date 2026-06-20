package com.example.teamflow.api;

import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.common.security.WorkspaceContext;
import com.example.teamflow.domain.invitation.dto.MemberInviteRequest;
import com.example.teamflow.domain.invitation.dto.ReceivedInviteResponse;
import com.example.teamflow.domain.invitation.service.InvitationService;
import com.example.teamflow.domain.invitation.service.MemberInviteService;
import com.example.teamflow.domain.member.dto.LoginResponse;
import com.example.teamflow.domain.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Invitation", description = "팀원 초대")
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final MemberInviteService memberInviteService;
    private final AuthService authService;

    @Operation(summary = "초대 링크 생성", description = "현재 워크스페이스의 초대 토큰을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createInvitation(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        Long workspaceId = WorkspaceContext.get();
        String token = invitationService.create(workspaceId, memberId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("token", token)));
    }

    @Operation(summary = "초대 수락 (기존 계정)", description = "이미 계정이 있는 사용자가 초대 토큰으로 워크스페이스에 합류합니다. 새 JWT(workspaceId 갱신)를 반환합니다.")
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<LoginResponse>> acceptInvitation(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(authService.acceptInvitation(body.get("token"), memberId)));
    }

    @Operation(summary = "이메일로 팀원 초대 (PM 전용)",
            description = "팀원의 계정 이메일을 입력하면, 해당 사용자의 계정에 워크스페이스 참가 요청(PENDING)이 생성됩니다.")
    @PreAuthorize("hasRole('PM')")
    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Void>> inviteByEmail(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberInviteRequest request) {
        memberInviteService.invite(WorkspaceContext.get(), memberId, request.email());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "내가 받은 참가 요청 목록",
            description = "현재 로그인한 멤버에게 도착한 미처리(PENDING) 워크스페이스 참가 요청을 반환합니다.")
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<ReceivedInviteResponse>>> getReceived(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(memberInviteService.listReceived(memberId)));
    }

    @Operation(summary = "참가 요청 수락",
            description = "참가 요청을 수락하여 해당 워크스페이스로 전환합니다. 새 JWT(workspaceId 갱신)를 반환합니다.")
    @PostMapping("/received/{inviteId}/accept")
    public ResponseEntity<ApiResponse<LoginResponse>> acceptReceived(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long inviteId) {
        return ResponseEntity.ok(ApiResponse.success(memberInviteService.accept(inviteId, memberId)));
    }

    @Operation(summary = "참가 요청 거절", description = "참가 요청을 거절합니다.")
    @PostMapping("/received/{inviteId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReceived(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long inviteId) {
        memberInviteService.reject(inviteId, memberId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
