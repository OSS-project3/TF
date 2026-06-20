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
    INVALID_DEADLINE("INVALID_DEADLINE", "마감일은 오늘부터 5년 이내여야 합니다.", HttpStatus.BAD_REQUEST),

    // Task
    TASK_NOT_FOUND("TASK_NOT_FOUND", "작업을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CIRCULAR_TASK_DEPENDENCY("CIRCULAR_TASK_DEPENDENCY", "순환 선행 관계가 감지되었습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_TASK_STATUS_TRANSITION("INVALID_TASK_STATUS_TRANSITION", "허용되지 않는 상태 전이입니다.", HttpStatus.UNPROCESSABLE_ENTITY),

    // Meeting
    MEETING_NOT_FOUND("MEETING_NOT_FOUND", "회의록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // Schedule
    PROPOSAL_NOT_FOUND("PROPOSAL_NOT_FOUND", "일정 재최적화 제안을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // AI
    AI_SERVICE_ERROR("AI_SERVICE_ERROR", "AI 서비스 호출 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_PARSE_FAILED("AI_PARSE_FAILED", "AI 응답 파싱에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_SESSION_NOT_FOUND("AI_SESSION_NOT_FOUND", "AI 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AI_SESSION_EXPIRED("AI_SESSION_EXPIRED", "AI 세션이 만료되었습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    AI_SESSION_INVALID("AI_SESSION_INVALID", "유효하지 않은 AI 세션 상태입니다.", HttpStatus.UNPROCESSABLE_ENTITY),

    // Webhook
    WEBHOOK_SIGNATURE_INVALID("WEBHOOK_SIGNATURE_INVALID", "웹훅 서명이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // Invitation
    INVITE_INVALID("INVITE_INVALID", "유효하지 않은 초대 코드입니다.", HttpStatus.BAD_REQUEST),
    INVITE_EXPIRED("INVITE_EXPIRED", "만료된 초대 코드입니다.", HttpStatus.BAD_REQUEST),
    INVITE_USED("INVITE_USED", "이미 사용된 초대 코드입니다.", HttpStatus.BAD_REQUEST),
    INVITE_NOT_FOUND("INVITE_NOT_FOUND", "초대를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVITE_TARGET_NOT_FOUND("INVITE_TARGET_NOT_FOUND", "해당 이메일의 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVITE_ALREADY_MEMBER("INVITE_ALREADY_MEMBER", "이미 워크스페이스에 속한 멤버입니다.", HttpStatus.CONFLICT),
    INVITE_ALREADY_SENT("INVITE_ALREADY_SENT", "이미 초대를 보낸 사용자입니다.", HttpStatus.CONFLICT),
    INVITE_SELF("INVITE_SELF", "자기 자신은 초대할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVITE_FORBIDDEN("INVITE_FORBIDDEN", "해당 초대에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVITE_NOT_PENDING("INVITE_NOT_PENDING", "이미 처리된 초대입니다.", HttpStatus.CONFLICT),

    // Workspace
    WORKSPACE_NOT_FOUND("WORKSPACE_NOT_FOUND", "워크스페이스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // Auth
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
