package com.example.teamflow.domain.ai.agent;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.infra.openai.OpenAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AssignmentAgent {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            당신은 팀 프로젝트 태스크 배분 전문가입니다.
            팀원 정보와 태스크 목록을 보고 각 태스크에 적합한 담당자를 추천하세요.

            규칙:
            1. memberName은 반드시 아래 '팀원 목록'에 있는 이름 중 정확히 하나를 사용하세요. 다른 이름은 절대 사용하지 마세요.
            2. 팀원의 역할(role)과 스킬(skills)을 고려해 적합한 태스크를 배정하세요.
            3. 각 팀원에게 배정된 태스크의 estimatedHours 합계가 해당 팀원의 weeklyCapacityHours를 최대한 초과하지 않도록 분배하세요.
            4. 가용시간이 적은 팀원에게 과도하게 배정하지 마세요. 가용시간이 많은 팀원에게 더 많은 작업을 배정하세요.
            5. 모든 태스크에 반드시 담당자를 배정하세요.

            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "assignments": [
                {"taskTitle": "태스크 제목", "memberName": "담당자 이름"}
              ]
            }
            """;

    public record TaskInput(String title, int estimatedHours) {}
    public record Assignment(String taskTitle, String memberName) {}

    public AiAgentResult<List<Assignment>> assign(List<TaskInput> tasks, List<MemberResponse> members) {
        String userMessage = buildUserMessage(tasks, members);
        OpenAiClient.ChatResult chatResult = openAiClient.chat(SYSTEM_PROMPT, userMessage);

        try {
            JsonNode root = objectMapper.readTree(chatResult.content());
            List<Assignment> assignments = new ArrayList<>();
            root.path("assignments").forEach(node -> assignments.add(new Assignment(
                    node.path("taskTitle").asText(),
                    node.path("memberName").asText()
            )));
            return new AiAgentResult<>(assignments, SYSTEM_PROMPT, chatResult.content(), chatResult.totalTokens());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }
    }

    private String buildUserMessage(List<TaskInput> tasks, List<MemberResponse> members) {
        StringBuilder sb = new StringBuilder("팀원 목록:\n");
        members.forEach(m -> sb.append("- ").append(m.name())
                .append(" (역할: ").append(m.role())
                .append(", 스킬: ").append(m.skills())
                .append(", 주간 가용시간: ").append(m.weeklyCapacityHours()).append("h)\n"));

        sb.append("\n태스크 목록 (제목 | 예상 소요시간):\n");
        tasks.forEach(t -> sb.append("- ").append(t.title())
                .append(" | ").append(t.estimatedHours()).append("h\n"));

        return sb.toString();
    }
}
