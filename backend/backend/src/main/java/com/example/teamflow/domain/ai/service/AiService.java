package com.example.teamflow.domain.ai.service;

import com.example.teamflow.domain.ai.dto.DecomposeRequest;
import com.example.teamflow.domain.ai.dto.DecomposeResponse;
import com.example.teamflow.domain.ai.dto.MeetingSummaryRequest;
import com.example.teamflow.domain.ai.dto.MeetingSummaryResponse;

/**
 * AI 기능. 구현체 교체 가능성(규칙기반 ↔ LLM)을 위해 인터페이스로 선언.
 */
public interface AiService {

    /** 프로젝트 목표 → 작업 목록 제안 (DB 저장 안 함). */
    DecomposeResponse decompose(DecomposeRequest request);

    /** 회의 원문 → 요약 + 액션 아이템 추출. */
    MeetingSummaryResponse summarizeMeeting(MeetingSummaryRequest request);
}
