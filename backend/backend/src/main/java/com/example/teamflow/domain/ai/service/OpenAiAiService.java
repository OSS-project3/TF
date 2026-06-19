package com.example.teamflow.domain.ai.service;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.ai.client.OpenAiClient;
import com.example.teamflow.domain.ai.dto.*;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.domain.member.service.MemberService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenAiAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAiService.class);

    private final OpenAiClient openAiClient;
    private final MemberService memberService;
    private final ObjectMapper objectMapper;

    @Override
    public DecomposeResponse decompose(DecomposeRequest request) {
        String roles = "정보 없음";
        if (request.memberIds() != null && !request.memberIds().isEmpty()) {
            List<MemberResponse> members = memberService.getMembersByIds(request.memberIds());
            if (!members.isEmpty()) {
                roles = members.stream()
                        .map(m -> m.name() + "(" + m.role() + ")")
                        .collect(Collectors.joining(", "));
            }
        }

        String system = """
                당신은 소프트웨어 프로젝트 관리 전문가 AI입니다.
                프로젝트 목표·마감일·팀 구성을 바탕으로 실행 가능한 작업(task) 목록을 만듭니다.
                반드시 아래 JSON 스키마로만 응답하세요(다른 텍스트 금지).
                {
                  "reasoningMessages": [짧은 한국어 분석 문장 2~3개],
                  "tasks": [
                    {
                      "title": "작업 제목(한국어)",
                      "phase": "단계(기획|디자인|개발|테스트|배포 중 하나 또는 적절한 단계명)",
                      "estimatedHours": 정수(2~40),
                      "difficulty": "EASY|MEDIUM|HARD 중 하나",
                      "dependencyTaskIds": []
                    }
                  ]
                }
                작업은 6~10개로, 마감일과 팀 역할을 고려해 현실적으로 작성하세요.
                """;

        String user = """
                프로젝트 목표: %s
                마감일: %s
                팀 구성(역할): %s
                """.formatted(
                nullSafe(request.goal()),
                request.deadline() != null ? request.deadline().toString() : "미정",
                roles
        );

        String json = openAiClient.completeJson(system, user);
        try {
            return mapper().readValue(json, DecomposeResponse.class);
        } catch (Exception e) {
            log.error("작업 분해 응답 파싱 실패: {}", json, e);
            throw new BusinessException(ErrorCode.AI_ERROR);
        }
    }

    @Override
    public MeetingSummaryResponse summarizeMeeting(MeetingSummaryRequest request) {
        String system = """
                당신은 회의록 요약 전문가 AI입니다.
                회의 원문에서 핵심 요약과 액션 아이템(TODO)을 추출합니다.
                반드시 아래 JSON 스키마로만 응답하세요(다른 텍스트 금지).
                {
                  "summary": [핵심 요약/결정 사항 문장(한국어) 3~6개],
                  "todos": [
                    {
                      "title": "할 일 내용(한국어)",
                      "assignee": "담당자 이름(원문에 있으면, 없으면 빈 문자열)",
                      "dueDate": "yyyy-MM-dd(언급되면, 없으면 빈 문자열)"
                    }
                  ]
                }
                """;

        String user = "회의 원문:\n" + nullSafe(request.notes());

        String json = openAiClient.completeJson(system, user);
        try {
            return mapper().readValue(json, MeetingSummaryResponse.class);
        } catch (Exception e) {
            log.error("회의 요약 응답 파싱 실패: {}", json, e);
            throw new BusinessException(ErrorCode.AI_ERROR);
        }
    }

    private ObjectMapper mapper() {
        return objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
