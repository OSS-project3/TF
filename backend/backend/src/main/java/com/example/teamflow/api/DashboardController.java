package com.example.teamflow.api;

import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.domain.dashboard.dto.MemberDashboardResponse;
import com.example.teamflow.domain.dashboard.dto.PmDashboardResponse;
import com.example.teamflow.domain.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "대시보드 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "PM 대시보드",
            description = """
                    PM 전용 대시보드를 반환합니다.
                    - 활성 프로젝트 현황 (평균 진행률, 태스크 통계)
                    - 팀 전체 이번 주 평균 부하율
                    - 활성 프로젝트 목록 (진행률 · health 포함)
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @PreAuthorize("hasRole('PM')")
    @GetMapping("/pm")
    public ResponseEntity<ApiResponse<PmDashboardResponse>> getPmDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getPmDashboard()));
    }

    @Operation(
            summary = "멤버 대시보드",
            description = """
                    로그인한 멤버 전용 대시보드를 반환합니다.
                    - 이번 주 워크로드 (loadRate, 배정 시간 / 가용 시간)
                    - 내 태스크 (오늘 마감 / 이번 주 마감 / 이후)
                    - 가장 가까운 미완료 태스크 마감일
                    - 참여 중인 활성 프로젝트 요약 목록
                    """
    )
    @GetMapping("/member")
    public ResponseEntity<ApiResponse<MemberDashboardResponse>> getMemberDashboard(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getMemberDashboard(memberId)));
    }
}
