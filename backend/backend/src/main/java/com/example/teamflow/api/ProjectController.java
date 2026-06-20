package com.example.teamflow.api;

import com.example.teamflow.common.enums.ProjectStatus;
import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.common.response.PageResponse;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.domain.project.dto.*;
import com.example.teamflow.domain.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Project", description = "프로젝트 API — PM 전용 엔드포인트는 설명에 [PM 전용] 표기")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
            summary = "프로젝트 목록 조회",
            description = """
                    전체 프로젝트 목록을 반환합니다.
                    - `memberId`: 해당 멤버가 속한 프로젝트만 필터링
                    - `status`: `ACTIVE` | `ARCHIVED` 로 상태 필터링
                    - `progress`: Task 집계 실시간 계산 (DB 컬럼 없음)
                    - `health`: Task 기반 계산 — `OK` | `WARN` | `BAD` | `IDLE`
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> getProjects(
            @Parameter(in = ParameterIn.QUERY, description = "멤버 ID 필터 (해당 멤버가 속한 프로젝트만)", example = "1")
            @RequestParam(required = false) Long memberId,
            @Parameter(in = ParameterIn.QUERY, description = "상태 필터",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"ACTIVE", "ARCHIVED"}))
            @RequestParam(required = false) ProjectStatus status) {
        List<ProjectResponse> projects = projectService.getProjects(memberId, status);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(projects)));
    }

    @Operation(summary = "프로젝트 생성 [PM 전용]", description = "새 프로젝트를 생성합니다. PM 역할 전용. `memberIds`: 초기 멤버 ID 목록 (빈 배열 허용). 생성된 프로젝트 ID 반환.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공 — projectId 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — 필수 필드 누락")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @PostMapping
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
            @Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(projectService.createProject(request)));
    }

    @Operation(summary = "프로젝트 상세 조회", description = "projectId로 특정 프로젝트의 상세 정보를 조회합니다. Task 집계 기반 progress·health 포함.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProject(projectId)));
    }

    @Operation(summary = "프로젝트 수정 [PM 전용]", description = "프로젝트 정보를 부분 수정합니다. PM 역할 전용. 수정 가능 필드: `name`, `goal`, `deadline`. null 필드는 변경하지 않음.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND")
    @PatchMapping("/{projectId}")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.updateProject(projectId, request)));
    }

    @Operation(summary = "프로젝트 아카이브 [PM 전용]", description = "프로젝트를 아카이브 처리합니다 (삭제가 아닌 status → ARCHIVED 변경). PM 역할 전용.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND")
    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<Void>> archiveProject(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId) {
        projectService.archiveProject(projectId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "프로젝트 멤버 목록 조회", description = "프로젝트에 소속된 멤버 목록을 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND")
    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<PageResponse<MemberResponse>>> getProjectMembers(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId) {
        List<MemberResponse> members = projectService.getProjectMembers(projectId);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(members)));
    }

    @Operation(summary = "프로젝트 멤버 일괄 교체 [PM 전용]", description = "프로젝트 멤버를 요청 목록으로 전체 교체합니다. PM 역할 전용. 기존 멤버를 모두 제거하고 `memberIds` 목록으로 다시 설정합니다 (PUT — 전체 덮어쓰기).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — memberIds 누락")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND 또는 MEMBER_NOT_FOUND")
    @PutMapping("/{projectId}/members")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<Void>> replaceProjectMembers(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberReplaceRequest request) {
        projectService.replaceProjectMembers(projectId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "프로젝트 멤버 추가 [PM 전용]", description = "특정 멤버를 프로젝트에 추가합니다. PM 역할 전용.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND 또는 MEMBER_NOT_FOUND")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "DUPLICATE_PROJECT_MEMBER — 이미 프로젝트 소속 멤버")
    @PostMapping("/{projectId}/members/{memberId}")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<Void>> addProjectMember(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "추가할 멤버 ID", required = true, example = "2")
            @PathVariable Long memberId) {
        projectService.addProjectMember(projectId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @Operation(summary = "프로젝트 멤버 제거 [PM 전용]", description = "특정 멤버를 프로젝트에서 제거합니다. PM 역할 전용.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN — PM 역할 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND 또는 MEMBER_NOT_FOUND")
    @DeleteMapping("/{projectId}/members/{memberId}")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<Void>> removeProjectMember(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "제거할 멤버 ID", required = true, example = "2")
            @PathVariable Long memberId) {
        projectService.removeProjectMember(projectId, memberId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
