package com.example.teamflow.api;

import com.example.teamflow.common.enums.MemberRole;
import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.common.response.PageResponse;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.domain.member.dto.MemberUpdateRequest;
import com.example.teamflow.domain.member.dto.PasswordChangeRequest;
import com.example.teamflow.domain.member.dto.TeamWorkloadResponse;
import com.example.teamflow.domain.member.dto.WorkloadResponse;
import com.example.teamflow.domain.member.service.AuthService;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Member", description = "멤버 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final ProjectService projectService;
    private final AuthService authService;

    @Operation(
            summary = "내 정보 조회",
            description = "JWT 토큰에서 memberId를 추출하여 현재 로그인한 멤버 정보를 반환합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMe(memberId)));
    }

    @Operation(
            summary = "멤버 목록 조회",
            description = "전체 멤버 목록을 반환합니다. `role` 파라미터로 역할별 필터링이 가능합니다."
    )
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<PageResponse<MemberResponse>>> getMembers(
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "역할 필터 (미입력 시 전체 반환)",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = MemberRole.class,
                            allowableValues = {"PM", "FRONTEND", "BACKEND", "DESIGNER", "QA"}
                    )
            )
            @RequestParam(required = false) MemberRole role) {
        List<MemberResponse> members = memberService.getMembers(role);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(members)));
    }

    @Operation(summary = "특정 멤버 조회", description = "memberId로 특정 멤버의 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMBER_NOT_FOUND")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(
            @Parameter(description = "조회할 멤버의 ID", required = true, example = "1")
            @PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(memberId)));
    }

    @Operation(summary = "특정 멤버 워크로드 조회", description = "기간 내 해당 멤버에게 배정된 태스크 기준으로 부하율을 계산합니다. from/to 미입력 시 전체 기간 기준.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMBER_NOT_FOUND")
    @GetMapping("/members/{memberId}/workload")
    public ResponseEntity<ApiResponse<WorkloadResponse>> getMemberWorkload(
            @Parameter(description = "멤버 ID", required = true, example = "2")
            @PathVariable Long memberId,
            @Parameter(in = ParameterIn.QUERY, description = "조회 시작일 (yyyy-MM-dd)", example = "2026-06-16")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(in = ParameterIn.QUERY, description = "조회 종료일 (yyyy-MM-dd)", example = "2026-06-22")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getWorkload(memberId, from, to)));
    }

    @Operation(summary = "내 프로필 수정", description = "이름·이니셜·역할·주간 가용 시간·스킬을 수정합니다. null 필드는 변경하지 않음. skills에 빈 배열 전달 시 전체 삭제. Google 가입 후 역할 선택(`role` 필드)에도 사용합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — 유효성 검사 실패")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMe(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMe(memberId, request)));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — 새 비밀번호 형식 오류")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "WRONG_PASSWORD — 현재 비밀번호 불일치")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PasswordChangeRequest request) {
        memberService.changePassword(memberId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "계정을 삭제하고 현재 토큰을 즉시 무효화합니다. 탈퇴 후 같은 토큰으로 요청 시 401 반환."
    )
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(
            @AuthenticationPrincipal Long memberId,
            HttpServletRequest request) {
        projectService.removeMemberFromAllProjects(memberId);
        memberService.deleteMe(memberId);
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            authService.logout(bearer.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "팀 워크로드 조회",
            description = """
                    팀 전체 또는 특정 프로젝트 멤버의 워크로드를 반환합니다.
                    - `projectId`: 해당 프로젝트 소속 멤버만 필터링 (미입력 시 전체 팀)
                    - `from` / `to`: 기간 필터 (미입력 시 전체 기간)
                    """
    )
    @GetMapping("/team/workloads")
    public ResponseEntity<ApiResponse<PageResponse<TeamWorkloadResponse>>> getTeamWorkloads(
            @Parameter(in = ParameterIn.QUERY, description = "프로젝트 ID 필터", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(in = ParameterIn.QUERY, description = "조회 시작일 (yyyy-MM-dd)", example = "2026-06-16")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(in = ParameterIn.QUERY, description = "조회 종료일 (yyyy-MM-dd)", example = "2026-06-22")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<Long> memberIds = (projectId != null) ? projectService.getMemberIds(projectId) : null;
        List<TeamWorkloadResponse> workloads = memberService.getTeamWorkloads(memberIds, from, to);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(workloads)));
    }
}
