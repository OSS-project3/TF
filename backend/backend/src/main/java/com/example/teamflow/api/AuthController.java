package com.example.teamflow.api;

import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.domain.member.dto.GoogleLoginRequest;
import com.example.teamflow.domain.member.dto.LoginRequest;
import com.example.teamflow.domain.member.dto.LoginResponse;
import com.example.teamflow.domain.member.dto.RegisterRequest;
import com.example.teamflow.domain.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새 멤버를 등록하고 JWT access token을 즉시 반환합니다. 가입 직후 별도 로그인 없이 반환된 토큰으로 바로 사용 가능.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가입 성공 — accessToken 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — 필수 필드 누락 또는 형식 오류")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "DUPLICATE_EMAIL — 이미 사용 중인 이메일")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request)));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다. 성공 시 JWT access token 반환 (유효 기간 1시간). 이후 모든 요청에 `Authorization: Bearer {accessToken}` 헤더 필수.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공 — accessToken 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — 필수 필드 누락")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS — 이메일 또는 비밀번호 불일치")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(summary = "Google 로그인 / 회원가입", description = "Google ID 토큰으로 로그인합니다. 계정이 없으면 자동 가입 후 워크스페이스가 생성됩니다. 신규 가입 시 `needsRoleSetup: true`가 반환되면 `/setup-role` 화면에서 역할을 선택해야 합니다. `inviteToken`을 함께 전달하면 해당 워크스페이스로 자동 합류합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인/가입 성공 — accessToken 반환. 신규 가입이면 needsRoleSetup=true")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "GOOGLE_AUTH_INVALID — ID 토큰 검증 실패 또는 clientId 불일치")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "GOOGLE_AUTH_DISABLED — 서버에 GOOGLE_CLIENT_ID 미설정")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.googleLogin(request)));
    }

    @Operation(summary = "로그아웃", description = "현재 토큰을 서버 블랙리스트에 등록하여 즉시 무효화합니다. 이후 같은 토큰으로 요청 시 401 반환. 토큰 만료 시점까지 블랙리스트 유지 (서버 재시작 시 초기화).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공 (토큰이 없거나 이미 만료된 경우에도 200 반환)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            authService.logout(bearer.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.success());
    }
}
