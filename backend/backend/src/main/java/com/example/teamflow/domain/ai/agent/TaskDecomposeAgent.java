package com.example.teamflow.domain.ai.agent;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.infra.openai.OpenAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TaskDecomposeAgent {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            당신은 대학생 팀 프로젝트 태스크 분해 전문가입니다.
            기능 설명과 요구사항 답변을 바탕으로 프로젝트 이름과 태스크 목록을 생성하세요.
            difficulty는 EASY, MEDIUM, HARD 중 하나입니다.

            각 태스크에는 시작일(startDate)과 마감일(endDate)을 "yyyy-MM-dd" 형식으로 반드시 포함하세요.
            일정 규칙:
            - 사용자가 제공한 '오늘 날짜'를 기준으로 첫 태스크를 시작하고, 모든 날짜는 '오늘 날짜' 이후여야 합니다.
            - 태스크를 기획 → 설계 → 개발 → 테스트 → 배포 단계 순서로 시간순으로 배치하고, 의존 관계가 있으면 선행 태스크가 먼저 끝나도록 하세요.
            - '프로젝트 마감일'이 주어지면 마지막 태스크의 endDate가 그 마감일을 넘지 않도록, estimatedHours에 비례해 각 태스크의 기간을 분배하세요.

            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "projectName": "프로젝트 이름",
              "projectGoal": "프로젝트 목표 한 줄 요약",
              "tasks": [
                {
                  "title": "태스크 제목",
                  "phase": "단계명 (예: 설계, 개발, 테스트)",
                  "estimatedHours": 4,
                  "difficulty": "MEDIUM",
                  "startDate": "2026-06-21",
                  "endDate": "2026-06-24"
                }
              ]
            }
            태스크는 5~10개로 작성하세요.
            """;

    public record DecomposeResult(
            String projectName,
            String projectGoal,
            List<TaskProposal> tasks
    ) {}

    public record TaskProposal(
            String title,
            String phase,
            int estimatedHours,
            String difficulty,
            String startDate,
            String endDate
    ) {}

    /** 날짜 컨텍스트 없이 호출 (Q&A 세션 등). 오늘 날짜만 기준으로 일정 생성. */
    public AiAgentResult<DecomposeResult> decompose(String feature, Map<String, Object> answers) {
        return decompose(feature, answers, LocalDate.now(), null);
    }

    /**
     * 오늘 날짜와 프로젝트 마감일을 기준으로 태스크를 분해하고, 각 태스크의 시작일/마감일까지 생성한다.
     * @param today    일정 기준이 되는 현재 날짜 (null이면 날짜 컨텍스트 미제공)
     * @param deadline 프로젝트 마감일 (null이면 마감 제약 없이 순차 배치)
     */
    public AiAgentResult<DecomposeResult> decompose(String feature, Map<String, Object> answers,
                                                    LocalDate today, LocalDate deadline) {
        String userMessage = buildUserMessage(feature, answers, today, deadline);
        OpenAiClient.ChatResult chatResult = openAiClient.chat(SYSTEM_PROMPT, userMessage);

        try {
            JsonNode root = objectMapper.readTree(chatResult.content());
            String projectName = root.path("projectName").asText("AI 생성 프로젝트");
            String projectGoal = root.path("projectGoal").asText(feature);

            List<TaskProposal> tasks = new ArrayList<>();
            root.path("tasks").forEach(node -> tasks.add(new TaskProposal(
                    node.path("title").asText(),
                    node.path("phase").asText("개발"),
                    node.path("estimatedHours").asInt(4),
                    node.path("difficulty").asText("MEDIUM"),
                    emptyToNull(node.path("startDate").asText(null)),
                    emptyToNull(node.path("endDate").asText(null))
            )));

            return new AiAgentResult<>(
                    new DecomposeResult(projectName, projectGoal, tasks),
                    SYSTEM_PROMPT, chatResult.content(), chatResult.totalTokens()
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }
    }

    private String buildUserMessage(String feature, Map<String, Object> answers,
                                    LocalDate today, LocalDate deadline) {
        StringBuilder sb = new StringBuilder("구현 기능: ").append(feature).append("\n");
        if (today != null) sb.append("오늘 날짜: ").append(today).append("\n");
        if (deadline != null) sb.append("프로젝트 마감일: ").append(deadline).append("\n");
        sb.append("\n요구사항 답변:\n");
        answers.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
