package com.example.teamflow.api;

import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.domain.ai.dto.DecomposeRequest;
import com.example.teamflow.domain.ai.dto.DecomposeResponse;
import com.example.teamflow.domain.ai.dto.MeetingSummaryRequest;
import com.example.teamflow.domain.ai.dto.MeetingSummaryResponse;
import com.example.teamflow.domain.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI 기능 API (OpenAI 연동). OPENAI_API_KEY 미설정 시 503 AI_DISABLED.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI 작업 분해", description = "프로젝트 목표·마감일·팀 구성으로 작업 목록을 제안합니다. 결과는 저장되지 않으며 프론트에서 확인 후 POST /projects/{id}/tasks로 저장합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분해 결과 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI_ERROR — OpenAI 호출/파싱 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI_DISABLED — OPENAI_API_KEY 미설정")
    @PostMapping("/decompositions")
    public ResponseEntity<ApiResponse<DecomposeResponse>> decompose(@RequestBody DecomposeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(aiService.decompose(request)));
    }

    @Operation(summary = "AI 회의 요약", description = "회의 원문에서 핵심 요약과 액션 아이템(TODO)을 추출합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 결과 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI_ERROR — OpenAI 호출/파싱 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI_DISABLED — OPENAI_API_KEY 미설정")
    @PostMapping("/meeting-summaries")
    public ResponseEntity<ApiResponse<MeetingSummaryResponse>> summarize(@RequestBody MeetingSummaryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(aiService.summarizeMeeting(request)));
    }
}
