package com.example.teamflow.domain.ai.agent;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.ai.dto.AiSummaryResponse;
import com.example.teamflow.domain.ai.dto.TodoItem;
import com.example.teamflow.domain.meeting.dto.MeetingResponse;
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
public class MeetingAgent {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            당신은 회의록 분석 전문가입니다.
            회의 내용을 분석하여 핵심 결정사항과 후속 조치(TODO)를 추출하세요.
            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "summary": ["결정사항1", "결정사항2"],
              "todos": [
                {"title": "할 일 제목", "assignee": "담당자 이름 (없으면 null)"}
              ]
            }
            """;

    public AiAgentResult<AiSummaryResponse> analyze(MeetingResponse meeting) {
        String userMessage = buildUserMessage(meeting);
        OpenAiClient.ChatResult chatResult = openAiClient.chat(SYSTEM_PROMPT, userMessage);

        try {
            JsonNode root = objectMapper.readTree(chatResult.content());

            List<String> summary = new ArrayList<>();
            root.path("summary").forEach(n -> summary.add(n.asText()));

            List<TodoItem> todos = new ArrayList<>();
            root.path("todos").forEach(n -> todos.add(new TodoItem(
                    n.path("title").asText(),
                    n.path("assignee").isNull() ? null : n.path("assignee").asText(null)
            )));

            return new AiAgentResult<>(
                    new AiSummaryResponse(summary, todos),
                    SYSTEM_PROMPT, chatResult.content(), chatResult.totalTokens()
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }
    }

    private String buildUserMessage(MeetingResponse meeting) {
        StringBuilder sb = new StringBuilder();
        sb.append("회의 제목: ").append(meeting.title()).append("\n");
        sb.append("회의 날짜: ").append(meeting.date()).append("\n");
        if (meeting.notes() != null && !meeting.notes().isBlank()) {
            sb.append("회의 내용:\n").append(meeting.notes()).append("\n");
        }
        if (!meeting.summary().isEmpty()) {
            sb.append("기존 요약 항목:\n");
            meeting.summary().forEach(s -> sb.append("- ").append(s).append("\n"));
        }
        if (!meeting.todos().isEmpty()) {
            sb.append("기존 TODO:\n");
            meeting.todos().forEach(t -> sb.append("- ").append(t.title()).append("\n"));
        }
        return sb.toString();
    }
}
