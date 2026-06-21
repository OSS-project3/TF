package com.example.teamflow.domain.ai.agent;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.ai.dto.QuestionItem;
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
public class RequirementAgent {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            당신은 대학생 팀 프로젝트 요구사항 분석가입니다.
            사용자가 입력한 기능 설명을 보고 프로젝트 생성에 필요한 추가 질문 3~5개를 생성하세요.
            type은 boolean(예/아니오), single(단답), text(서술형) 중 하나입니다.

            반드시 아래 3가지 항목을 질문에 포함하세요:
            1. 프론트엔드(화면/UI)가 이미 구현되어 있나요? (type: boolean)
            2. ERD 또는 데이터베이스 스키마가 이미 설계·구현되어 있나요? (type: boolean)
            3. 백엔드 API만 개발하면 되나요, 아니면 프론트엔드도 함께 개발해야 하나요? (type: single)
            그 외 기능에 특화된 질문을 1~2개 추가하세요.

            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "questions": [
                {"question": "질문 내용", "type": "boolean"}
              ]
            }
            """;

    public AiAgentResult<List<QuestionItem>> generateQuestions(String feature) {
        OpenAiClient.ChatResult chatResult = openAiClient.chat(SYSTEM_PROMPT, "구현 기능: " + feature);

        try {
            JsonNode root = objectMapper.readTree(chatResult.content());
            List<QuestionItem> questions = new ArrayList<>();
            root.path("questions").forEach(node ->
                    questions.add(new QuestionItem(
                            node.path("question").asText(),
                            node.path("type").asText("single")
                    ))
            );
            return new AiAgentResult<>(questions, SYSTEM_PROMPT, chatResult.content(), chatResult.totalTokens());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }
    }
}
