package com.example.teamflow.api;

import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.domain.ai.agent.TaskDecomposeAgent;
import com.example.teamflow.domain.ai.dto.*;
import com.example.teamflow.domain.ai.facade.AiProjectFacade;
import com.example.teamflow.domain.ai.service.MeetingAiService;
import com.example.teamflow.domain.ai.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI", description = "AI Project Manager API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class AiController {

    private final MeetingAiService meetingAiService;
    private final AiProjectFacade aiProjectFacade;
    private final MonitoringService monitoringService;
    private final TaskDecomposeAgent taskDecomposeAgent;

    @Operation(
            summary = "회의록 AI 요약",
            description = "회의 원문(notes)과 기존 항목을 분석하여 핵심 결정사항과 TODO를 생성합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEETING_NOT_FOUND")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI_SERVICE_ERROR | AI_PARSE_FAILED")
    @PostMapping("/api/v1/meetings/{meetingId}/ai-summary")
    public ResponseEntity<ApiResponse<AiSummaryResponse>> summarizeMeeting(
            @Parameter(description = "회의록 ID", required = true, example = "1")
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(meetingAiService.summarize(meetingId, memberId)));
    }

    @Operation(
            summary = "프로젝트 위험 요소 조회",
            description = "RiskAgent가 분석한 가장 최근 위험 요소 목록을 반환합니다. " +
                    "분석 이력이 없으면 빈 배열을 반환합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND")
    @GetMapping("/api/v1/projects/{projectId}/risks")
    public ResponseEntity<ApiResponse<AiRiskResponse>> getRisks(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.getRisks(projectId)));
    }

    @Operation(
            summary = "AI 프로젝트 생성 시작",
            description = "기능 설명을 입력하면 RequirementAgent가 추가 질문 3~5개를 생성합니다. " +
                    "반환된 sessionId로 `/answers` 엔드포인트에 답변을 제출하세요."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI_SERVICE_ERROR | AI_PARSE_FAILED")
    @PostMapping("/api/v1/projects/ai-generate")
    public ResponseEntity<ApiResponse<AiGenerateStartResponse>> startGeneration(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody AiGenerateStartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                aiProjectFacade.startGeneration(request.feature(), memberId)));
    }

    @Operation(
            summary = "모니터링 수동 실행",
            description = "PM 전용. 전체 활성 프로젝트 위험 분석을 즉시 실행합니다. 발표/테스트 시 사용."
    )
    @PreAuthorize("hasRole('PM')")
    @PostMapping("/api/v1/admin/monitoring/trigger")
    public ResponseEntity<ApiResponse<Void>> triggerMonitoring() {
        monitoringService.runDailyMonitoring();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "회의 노트 직접 AI 요약",
            description = "기존 회의록 ID 없이 회의 노트를 직접 전달하면 요약과 TODO를 생성합니다. " +
                    "프론트엔드 회의록 작성 화면(실시간 요약)에서 사용합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI_SERVICE_ERROR | AI_PARSE_FAILED")
    @PostMapping("/api/v1/ai/meeting-summaries")
    public ResponseEntity<ApiResponse<AiSummaryResponse>> summarizeMeetingNotes(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MeetingAiRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                meetingAiService.summarizeFromNotes(request.notes(), request.projectId(), memberId)));
    }

    @Operation(
            summary = "AI 태스크 분해 (단일 호출)",
            description = "프로젝트 목표를 입력하면 태스크 목록을 즉시 반환합니다. " +
                    "Q&A 세션 없이 한 번에 결과를 받습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI_SERVICE_ERROR | AI_PARSE_FAILED")
    @PostMapping("/api/v1/ai/decompositions")
    public ResponseEntity<ApiResponse<AiDecomposeResponse>> decompose(
            @Valid @RequestBody AiDecomposeRequest request) {
        java.time.LocalDate deadline = (request.deadline() != null && !request.deadline().isBlank())
                ? java.time.LocalDate.parse(request.deadline()) : null;
        TaskDecomposeAgent.DecomposeResult result =
                taskDecomposeAgent.decompose(request.goal(), java.util.Map.of(),
                        java.time.LocalDate.now(), deadline).data();
        java.util.List<AiDecomposeResponse.TaskItem> tasks = result.tasks().stream()
                .map(t -> new AiDecomposeResponse.TaskItem(
                        t.title(), t.phase(), t.estimatedHours(), t.difficulty(),
                        t.startDate(), t.endDate()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(new AiDecomposeResponse(tasks)));
    }

    @Operation(
            summary = "AI 프로젝트 생성 완료",
            description = "RequirementAgent가 생성한 질문에 대한 답변을 제출합니다. " +
                    "TaskAgent → AssignmentAgent 순으로 실행되어 프로젝트와 태스크를 자동 생성합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AI_SESSION_NOT_FOUND")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "AI_SESSION_EXPIRED | AI_SESSION_INVALID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI_SERVICE_ERROR | AI_PARSE_FAILED")
    @PostMapping("/api/v1/projects/ai-generate/{sessionId}/answers")
    public ResponseEntity<ApiResponse<AiGenerateCompleteResponse>> completeGeneration(
            @Parameter(description = "AI 세션 ID", required = true, example = "1")
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody AiGenerateAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                aiProjectFacade.completeGeneration(sessionId, request.answers(), memberId)));
    }
}
