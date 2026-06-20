package com.example.teamflow.domain.task.service;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.task.dto.GithubPrPayload;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final TaskService taskService;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    public void verifySignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("GitHub Webhook secret이 설정되지 않았습니다. 서명 검증을 건너뜁니다.");
            return;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + toHex(hash);
            if (!expected.equals(signature)) {
                throw new BusinessException(ErrorCode.WEBHOOK_SIGNATURE_INVALID);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
    }

    public int handlePrMerge(GithubPrPayload payload) {
        if (!payload.isMerged()) return 0;

        String branch = payload.mergedBranch();
        if (branch == null || branch.isBlank()) return 0;

        int count = taskService.completeByGitBranch(branch);
        if (count > 0) {
            log.info("GitHub PR 머지 감지 — 브랜치: {}, 자동 완료 태스크: {}건", branch, count);
        }
        return count;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
