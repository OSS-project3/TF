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

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    public OpenAiClient(@Qualifier("openAiRestClient") RestClient restClient,
                        ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public record ChatResult(String content, int totalTokens) {}

    public ChatResult chat(String systemPrompt, String userMessage) {
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
}
