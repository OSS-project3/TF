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
            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "projectName": "프로젝트 이름",
              "projectGoal": "프로젝트 목표 한 줄 요약",
              "tasks": [
                {
                  "title": "태스크 제목",
                  "phase": "단계명 (예: 설계, 개발, 테스트)",
                  "estimatedHours": 4,
                  "difficulty": "MEDIUM"
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
            String difficulty
    ) {}

    public AiAgentResult<DecomposeResult> decompose(String feature, Map<String, Object> answers) {
        String userMessage = buildUserMessage(feature, answers);
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
                    node.path("difficulty").asText("MEDIUM")
            )));

            return new AiAgentResult<>(
                    new DecomposeResult(projectName, projectGoal, tasks),
                    SYSTEM_PROMPT, chatResult.content(), chatResult.totalTokens()
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }
    }

    private String buildUserMessage(String feature, Map<String, Object> answers) {
        StringBuilder sb = new StringBuilder("구현 기능: ").append(feature).append("\n\n요구사항 답변:\n");
        answers.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }
}
