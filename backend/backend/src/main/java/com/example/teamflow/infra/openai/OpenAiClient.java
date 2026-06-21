package com.example.teamflow.infra.openai;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;
    @Value("${openai.api-key:}")
    private String apiKey;

    public OpenAiClient(@Qualifier("openAiRestClient") RestClient restClient,
                        ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public record ChatResult(String content, int totalTokens) {}

    public ChatResult chat(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            return buildMockResponse(systemPrompt, userMessage);
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 2000
        );

        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            int totalTokens = root.path("usage").path("total_tokens").asInt(0);

            log.debug("OpenAI 호출 완료 — 토큰: {}", totalTokens);
            return new ChatResult(content, totalTokens);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    // API 키 미설정 시 목 응답 — 시스템 프롬프트로 응답 형식 추론
    private ChatResult buildMockResponse(String systemPrompt, String userMessage) {
        log.warn("OpenAI API 키 미설정 — 목 응답 반환");
        String content;
        if (systemPrompt.contains("태스크 분해") || systemPrompt.contains("startDate")) {
            // "구현 기능:" 줄에서 첫 번째 값만 추출 (줄바꿈이 포함되면 JSON이 깨짐)
            String goal = userMessage.lines()
                    .filter(l -> l.startsWith("구현 기능:"))
                    .map(l -> l.replace("구현 기능:", "").trim())
                    .findFirst()
                    .orElse("팀 프로젝트");
            if (goal.isBlank()) goal = "팀 프로젝트";
            // JSON 안전 처리 (따옴표·역슬래시 이스케이프)
            goal = goal.replace("\\", "\\\\").replace("\"", "\\\"");

            // 백엔드 전용 여부 감지 (goal 전체 + 현재 구현 현황 라인)
            boolean backendOnly = List.of(
                    "프론트엔드 완료", "프론트엔드는 완성", "프론트엔드가 완성", "프론트엔드 구현 완료",
                    "프론트엔드(ui) 완료", "ui 완료", "ui 완성",
                    "백엔드만 구현", "백엔드만 개발", "백엔드 개발만", "백엔드만 필요",
                    "백엔드 api만", "서버만", "서버 개발만"
            ).stream().anyMatch(kw -> userMessage.toLowerCase().contains(kw.toLowerCase()));

            LocalDate d = LocalDate.now();
            String d0  = d.toString();
            String d1  = d.plusDays(1).toString();
            String d3  = d.plusDays(3).toString();
            String d5  = d.plusDays(5).toString();
            String d6  = d.plusDays(6).toString();
            String d9  = d.plusDays(9).toString();
            String d11 = d.plusDays(11).toString();
            String d13 = d.plusDays(13).toString();
            String d15 = d.plusDays(15).toString();
            String d17 = d.plusDays(17).toString();

            if (backendOnly) {
                content = """
                    {
                      "projectName": "%s",
                      "projectGoal": "%s 백엔드 구현",
                      "tasks": [
                        {"title": "API 설계 및 DB 스키마 작성", "phase": "API 설계",      "estimatedHours": 8,  "difficulty": "MEDIUM", "startDate": "%s", "endDate": "%s"},
                        {"title": "핵심 비즈니스 로직 구현",     "phase": "핵심 기능 구현", "estimatedHours": 24, "difficulty": "HARD",   "startDate": "%s", "endDate": "%s"},
                        {"title": "외부 서비스 연동",            "phase": "연동 및 통합",   "estimatedHours": 16, "difficulty": "HARD",   "startDate": "%s", "endDate": "%s"},
                        {"title": "예외 처리 및 유효성 검증",     "phase": "핵심 기능 구현", "estimatedHours": 8,  "difficulty": "MEDIUM", "startDate": "%s", "endDate": "%s"},
                        {"title": "단위·통합 테스트 작성",        "phase": "테스트",        "estimatedHours": 8,  "difficulty": "MEDIUM", "startDate": "%s", "endDate": "%s"},
                        {"title": "배포 및 API 문서화",          "phase": "배포",          "estimatedHours": 8,  "difficulty": "EASY",   "startDate": "%s", "endDate": "%s"}
                      ]
                    }
                    """.formatted(goal, goal,
                            d0, d1, d3, d5, d6, d9, d11, d13, d13, d15, d15, d17);
            } else {
                String d8  = d.plusDays(8).toString();
                content = """
                    {
                      "projectName": "%s",
                      "projectGoal": "%s 구현",
                      "tasks": [
                        {"title": "요구사항 분석 및 기획",  "phase": "기획",   "estimatedHours": 8,  "difficulty": "EASY",   "startDate": "%s", "endDate": "%s"},
                        {"title": "UI/UX 설계",            "phase": "디자인",  "estimatedHours": 16, "difficulty": "MEDIUM", "startDate": "%s", "endDate": "%s"},
                        {"title": "DB 스키마 설계",         "phase": "설계",   "estimatedHours": 8,  "difficulty": "MEDIUM", "startDate": "%s", "endDate": "%s"},
                        {"title": "백엔드 API 개발",        "phase": "개발",   "estimatedHours": 24, "difficulty": "HARD",   "startDate": "%s", "endDate": "%s"},
                        {"title": "프론트엔드 화면 개발",    "phase": "개발",   "estimatedHours": 24, "difficulty": "HARD",   "startDate": "%s", "endDate": "%s"},
                        {"title": "테스트 및 버그 수정",     "phase": "테스트", "estimatedHours": 16, "difficulty": "MEDIUM", "startDate": "%s", "endDate": "%s"},
                        {"title": "배포 및 문서화",          "phase": "배포",   "estimatedHours": 8,  "difficulty": "EASY",   "startDate": "%s", "endDate": "%s"}
                      ]
                    }
                    """.formatted(goal, goal,
                            d0, d1, d3, d5, d6, d8, d8, d11, d11, d13, d13, d15, d15, d17);
            }
        } else if (systemPrompt.contains("태스크 배분") || systemPrompt.contains("assignments")) {
            content = "{\"assignments\": []}";
        } else if (systemPrompt.contains("요구사항 분석가") || systemPrompt.contains("질문")) {
            content = """
                {
                  "questions": [
                    {"question": "주요 사용자는 누구인가요?", "type": "text"},
                    {"question": "핵심 기능 3가지를 간략히 설명해 주세요.", "type": "text"},
                    {"question": "외부 API 연동이 필요한가요?", "type": "boolean"}
                  ]
                }
                """;
        } else if (systemPrompt.contains("회의록 분석") || systemPrompt.contains("회의 내용")) {
            content = """
                {
                  "summary": ["주요 안건을 논의하였습니다.", "다음 액션 아이템을 확인하였습니다."],
                  "todos": [{"title": "회의 결과 문서화", "assignee": ""}]
                }
                """;
        } else if (systemPrompt.contains("위험 분석") || systemPrompt.contains("병목")) {
            content = "{\"risks\": [], \"recommendations\": []}";
        } else {
            content = "{}";
        }
        return new ChatResult(content.trim(), 0);
    }
}
