package com.example.teamflow.api;

import com.example.teamflow.common.enums.TaskStatus;
import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.common.response.PageResponse;
import com.example.teamflow.domain.task.dto.*;
import com.example.teamflow.domain.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Task", description = "태스크 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "프로젝트 태스크 목록 조회",
            description = """
                    특정 프로젝트의 태스크 목록을 반환합니다.
                    - `assigneeId`: 담당자 필터
                    - `status`: `TODO` | `IN_PROGRESS` | `DONE` | `BLOCKED` 필터
                    - `phase`: 페이즈(단계) 이름으로 필터 (예: "기획", "개발")
                    - `isCriticalPath`, `isLateRisk`: 태스크 특성 플래그
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND")
    @GetMapping("/api/v1/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getTasksByProject(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(in = ParameterIn.QUERY, description = "담당자 멤버 ID 필터", example = "2")
            @RequestParam(required = false) Long assigneeId,
            @Parameter(in = ParameterIn.QUERY, description = "상태 필터",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"TODO", "IN_PROGRESS", "DONE", "BLOCKED"}))
            @RequestParam(required = false) TaskStatus status,
            @Parameter(in = ParameterIn.QUERY, description = "페이즈 이름 필터", example = "기획")
            @RequestParam(required = false) String phase) {
        List<TaskResponse> tasks = taskService.getTasksByProject(projectId, assigneeId, status, phase);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(tasks)));
    }

    @Operation(
            summary = "태스크 생성",
            description = """
                    프로젝트에 새 태스크를 생성합니다.
                    - `difficulty`: `EASY` | `MEDIUM` | `HARD`
                    - `dependencyTaskIds`: 선행 태스크 ID 목록 (선행 태스크 완료 후 시작 가능)
                    - `startDate` / `endDate`: ISO-8601 날짜 형식 (`yyyy-MM-dd`)
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — 필수 필드 누락")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND 또는 MEMBER_NOT_FOUND (assigneeId 지정 시)")
    @PostMapping("/api/v1/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<TaskCreateResponse>> createTask(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(taskService.createTask(projectId, request)));
    }

    @Operation(
            summary = "태스크 수정",
            description = """
                    태스크 정보를 부분 수정합니다.
                    - 모든 필드 선택적 (null인 필드는 수정하지 않음)
                    - 수정 가능 필드: `title`, `phase`, `estimatedHours`, `difficulty`, `startDate`, `endDate`, `isCriticalPath`, `isLateRisk`
                    - 상태 변경은 `PATCH /tasks/{id}/status` 사용
                    - 담당자 변경은 `PATCH /tasks/{id}/assignee` 사용
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "TASK_NOT_FOUND")
    @PatchMapping("/api/v1/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @Parameter(description = "태스크 ID", required = true, example = "10")
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.updateTask(taskId, request)));
    }

    @Operation(
            summary = "태스크 상태 변경",
            description = "태스크의 진행 상태를 변경합니다. `status`: `TODO` | `IN_PROGRESS` | `DONE` | `BLOCKED`"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — status 누락")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "TASK_NOT_FOUND")
    @PatchMapping("/api/v1/tasks/{taskId}/status")
    public ResponseEntity<ApiResponse<Void>> changeStatus(
            @Parameter(description = "태스크 ID", required = true, example = "10")
            @PathVariable Long taskId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        taskService.changeStatus(taskId, request.status(), memberId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "태스크 담당자 변경",
            description = "태스크의 담당 멤버를 변경합니다. `assigneeId`에 변경할 멤버 ID를 지정합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "TASK_NOT_FOUND 또는 MEMBER_NOT_FOUND")
    @PatchMapping("/api/v1/tasks/{taskId}/assignee")
    public ResponseEntity<ApiResponse<Void>> changeAssignee(
            @Parameter(description = "태스크 ID", required = true, example = "10")
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssigneeUpdateRequest request) {
        taskService.changeAssignee(taskId, request.assigneeIds());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "태스크 삭제", description = "태스크를 삭제합니다. 연결된 선행 관계 및 실행 로그도 함께 삭제됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "TASK_NOT_FOUND")
    @DeleteMapping("/api/v1/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @Parameter(description = "태스크 ID", required = true, example = "10")
            @PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "내 태스크 조회",
            description = """
                    JWT 토큰 기반으로 현재 로그인한 멤버의 태스크를 날짜별로 그룹화하여 반환합니다.
                    - `today`: 오늘 마감 태스크
                    - `thisWeek`: 이번 주 마감 태스크 (오늘 제외)
                    - `later`: 이후 마감 태스크
                    - `status` 파라미터로 추가 필터링 가능
                    """
    )
    @GetMapping("/api/v1/me/tasks")
    public ResponseEntity<ApiResponse<MyTasksResponse>> getMyTasks(
            @AuthenticationPrincipal Long memberId,
            @Parameter(in = ParameterIn.QUERY, description = "상태 필터 (미입력 시 전체)",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"TODO", "IN_PROGRESS", "DONE", "BLOCKED"}))
            @RequestParam(required = false) TaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getMyTasks(memberId, status)));
    }
}
