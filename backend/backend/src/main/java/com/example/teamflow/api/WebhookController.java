package com.example.teamflow.api;

import com.example.teamflow.domain.task.dto.GithubPrPayload;
import com.example.teamflow.domain.task.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Webhook", description = "GitHub Webhook 수신 API")
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "GitHub Webhook 수신",
            description = "PR 머지 이벤트 수신 시, 연동된 브랜치명과 일치하는 태스크를 자동으로 DONE 처리합니다."
    )
    @PostMapping("/api/v1/webhooks/github")
    public ResponseEntity<Void> handleGithubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestBody String payload) {

        webhookService.verifySignature(payload, signature);

        if ("pull_request".equals(event)) {
            try {
                GithubPrPayload prPayload = objectMapper.readValue(payload, GithubPrPayload.class);
                webhookService.handlePrMerge(prPayload);
            } catch (Exception e) {
                log.warn("GitHub PR 웹훅 파싱 실패: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok().build();
    }
}
