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
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final MemberInviteService memberInviteService;
    private final AuthService authService;

    @Operation(summary = "초대 링크 생성 [PM 전용]", description = "현재 워크스페이스의 UUID 초대 토큰을 생성합니다. 토큰 유효 기간은 7일이며 1회만 사용 가능합니다. 프론트엔드는 이 토큰으로 `{origin}/signup?token={token}` 또는 `{origin}/login?token={token}` 초대 URL을 조합합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 생성 성공 — { token: UUID }")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createInvitation(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        Long workspaceId = WorkspaceContext.get();
        String token = invitationService.create(workspaceId, memberId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("token", token)));
    }

    @Operation(summary = "초대 수락 (기존 계정)", description = "이미 계정이 있는 사용자가 초대 토큰으로 워크스페이스에 합류합니다. 토큰을 소비하고 workspaceId가 갱신된 새 JWT를 반환합니다. 프론트엔드는 로그인 성공 직후 토큰이 URL에 있으면 이 API를 자동 호출합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "합류 성공 — 새 accessToken(workspaceId 갱신) 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "INVITE_INVALID — 존재하지 않는 토큰")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "INVITE_EXPIRED | INVITE_USED — 만료되었거나 이미 사용된 토큰")
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<LoginResponse>> acceptInvitation(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(authService.acceptInvitation(body.get("token"), memberId)));
    }

    @Operation(summary = "이메일로 팀원 초대 [PM 전용]",
            description = "팀원의 계정 이메일을 입력하면 해당 사용자의 계정에 워크스페이스 참가 요청(PENDING)이 생성됩니다. 대상 멤버는 `GET /invitations/received`로 요청을 확인하고 수락/거절할 수 있습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "초대 요청 생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMBER_NOT_FOUND — 해당 이메일의 계정 없음")
    @PreAuthorize("hasRole('PM')")
    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Void>> inviteByEmail(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberInviteRequest request) {
        memberInviteService.invite(WorkspaceContext.get(), memberId, request.email());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "내가 받은 참가 요청 목록",
            description = "현재 로그인한 멤버에게 도착한 미처리(PENDING) 워크스페이스 참가 요청을 반환합니다. 각 요청에는 workspaceName·inviterName·inviteId가 포함됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록 반환 (없으면 빈 배열)")
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<ReceivedInviteResponse>>> getReceived(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(memberInviteService.listReceived(memberId)));
    }

    @Operation(summary = "참가 요청 수락",
            description = "참가 요청을 수락하여 해당 워크스페이스로 전환합니다. workspaceId가 갱신된 새 JWT를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공 — 새 accessToken(workspaceId 갱신) 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 요청을 찾을 수 없음")
    @PostMapping("/received/{inviteId}/accept")
    public ResponseEntity<ApiResponse<LoginResponse>> acceptReceived(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long inviteId) {
        return ResponseEntity.ok(ApiResponse.success(memberInviteService.accept(inviteId, memberId)));
    }

    @Operation(summary = "참가 요청 거절", description = "참가 요청을 거절합니다. 거절된 요청은 REJECTED 상태로 변경되며 목록에서 사라집니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 요청을 찾을 수 없음")
    @PostMapping("/received/{inviteId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReceived(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long inviteId) {
        memberInviteService.reject(inviteId, memberId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
