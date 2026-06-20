package com.example.teamflow.api;

import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.common.security.WorkspaceContext;
import com.example.teamflow.domain.invitation.service.InvitationService;
import com.example.teamflow.domain.member.dto.LoginResponse;
import com.example.teamflow.domain.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Invitation", description = "팀원 초대")
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
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
}
