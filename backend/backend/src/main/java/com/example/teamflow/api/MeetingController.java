package com.example.teamflow.api;

import com.example.teamflow.common.response.ApiResponse;
import com.example.teamflow.common.response.PageResponse;
import com.example.teamflow.domain.meeting.dto.MeetingCreateRequest;
import com.example.teamflow.domain.meeting.dto.MeetingCreateResponse;
import com.example.teamflow.domain.meeting.dto.MeetingResponse;
import com.example.teamflow.domain.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Meeting", description = "회의록 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(
            summary = "회의록 목록 조회",
            description = """
                    회의록 목록을 반환합니다. 날짜 내림차순 정렬.
                    - `projectId`: 해당 프로젝트 관련 TODO가 있는 회의록만 필터링
                    - `from` / `to`: 날짜 범위 필터 (yyyy-MM-dd)
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MeetingResponse>>> getMeetings(
            @Parameter(in = ParameterIn.QUERY, description = "프로젝트 ID 필터", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(in = ParameterIn.QUERY, description = "조회 시작일 (yyyy-MM-dd)", example = "2026-05-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(in = ParameterIn.QUERY, description = "조회 종료일 (yyyy-MM-dd)", example = "2026-06-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<MeetingResponse> meetings = meetingService.getMeetings(projectId, from, to);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(meetings)));
    }

    @Operation(summary = "회의록 상세 조회", description = "meetingId로 특정 회의록의 상세 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEETING_NOT_FOUND")
    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeeting(
            @Parameter(description = "회의록 ID", required = true, example = "1")
            @PathVariable Long meetingId) {
        return ResponseEntity.ok(ApiResponse.success(meetingService.getMeeting(meetingId)));
    }

    @Operation(summary = "회의록 저장", description = "`manual: false` — AI 요약 결과 저장 (summary·todos 포함). `manual: true` — 수기 등록 (summary 빈 배열 허용, notes 필수). 저장된 회의록 ID 반환.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "저장 성공 — meetingId 반환")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT — 필수 필드 누락")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMBER_NOT_FOUND (attendeeMemberIds) 또는 PROJECT_NOT_FOUND (todos.projectId)")
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingCreateResponse>> createMeeting(
            @Valid @RequestBody MeetingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meetingService.createMeeting(request)));
    }
}
