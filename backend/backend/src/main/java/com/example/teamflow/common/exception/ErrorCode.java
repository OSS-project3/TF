package com.example.teamflow.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Member
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "멤버를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    WRONG_PASSWORD("WRONG_PASSWORD", "현재 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),

    // Project
    PROJECT_NOT_FOUND("PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_PROJECT_MEMBER("DUPLICATE_PROJECT_MEMBER", "이미 프로젝트에 소속된 멤버입니다.", HttpStatus.CONFLICT),

    // Task
    TASK_NOT_FOUND("TASK_NOT_FOUND", "작업을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CIRCULAR_TASK_DEPENDENCY("CIRCULAR_TASK_DEPENDENCY", "순환 선행 관계가 감지되었습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_TASK_STATUS_TRANSITION("INVALID_TASK_STATUS_TRANSITION", "허용되지 않는 상태 전이입니다.", HttpStatus.UNPROCESSABLE_ENTITY),

    // Meeting
    MEETING_NOT_FOUND("MEETING_NOT_FOUND", "회의록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // Schedule
    PROPOSAL_NOT_FOUND("PROPOSAL_NOT_FOUND", "일정 재최적화 제안을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // Auth
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.", HttpStatus.FORBIDDEN),

    // AI
    AI_DISABLED("AI_DISABLED", "AI 기능이 비활성화되어 있습니다. OPENAI_API_KEY를 설정하세요.", HttpStatus.SERVICE_UNAVAILABLE),
    AI_ERROR("AI_ERROR", "AI 처리 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
