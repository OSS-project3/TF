package com.example.teamflow.domain.ai.agent;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.ai.dto.AiRiskResponse;
import com.example.teamflow.domain.ai.dto.BottleneckReport;
import com.example.teamflow.domain.ai.dto.RiskItem;
import com.example.teamflow.infra.openai.OpenAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskAgent {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            당신은 팀 프로젝트 위험 분석 전문가입니다.
            아래 프로젝트 병목 현황을 보고 위험 요소와 권고사항을 JSON으로 반환하세요.
            level은 반드시 WARN 또는 CRITICAL 중 하나입니다.
            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "risks": [
                {
                  "level": "WARN",
                  "message": "위험 요소 설명",
                  "recommendation": "권고사항"
                }
              ]
            }
            위험 요소가 없으면 risks를 빈 배열로 반환하세요.
            """;

    public AiAgentResult<AiRiskResponse> analyze(BottleneckReport report) {
        String userMessage = buildUserMessage(report);
        OpenAiClient.ChatResult chatResult = openAiClient.chat(SYSTEM_PROMPT, userMessage);

        try {
            JsonNode root = objectMapper.readTree(chatResult.content());

            List<RiskItem> risks = new ArrayList<>();
            root.path("risks").forEach(n -> risks.add(new RiskItem(
                    n.path("level").asText("WARN"),
                    n.path("message").asText(),
                    n.path("recommendation").asText()
            )));

            return new AiAgentResult<>(
                    new AiRiskResponse(risks, LocalDateTime.now()),
                    SYSTEM_PROMPT, chatResult.content(), chatResult.totalTokens()
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }
    }

    private String buildUserMessage(BottleneckReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("프로젝트: ").append(report.projectName()).append("\n\n");

        sb.append("지연 태스크 (").append(report.lateTasks().size()).append("개):\n");
        report.lateTasks().forEach(t ->
                sb.append("- ").append(t.title()).append(" (담당: ").append(t.assigneeName()).append(")\n"));

        sb.append("\nBLOCKED 태스크 (").append(report.blockedTasks().size()).append("개):\n");
        report.blockedTasks().forEach(t ->
                sb.append("- ").append(t.title()).append(" (담당: ").append(t.assigneeName()).append(")\n"));

        sb.append("\n정체 태스크 — 5일 이상 IN_PROGRESS 변화 없음 (").append(report.stuckTasks().size()).append("개):\n");
        report.stuckTasks().forEach(t ->
                sb.append("- ").append(t.title()).append(" (담당: ").append(t.assigneeName()).append(")\n"));

        if (!report.overloadedMembers().isEmpty()) {
            sb.append("\n워크로드 초과 멤버:\n");
            report.overloadedMembers().forEach(m ->
                    sb.append("- ").append(m.memberName()).append(": IN_PROGRESS 태스크 ").append(m.taskCount()).append("개\n"));
        }

        return sb.toString();
    }
}
