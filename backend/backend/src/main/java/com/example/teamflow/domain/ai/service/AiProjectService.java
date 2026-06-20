package com.example.teamflow.domain.ai.service;

import com.example.teamflow.common.enums.AgentType;
import com.example.teamflow.common.enums.AiSessionStatus;
import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.ai.agent.RequirementAgent;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.ai.dto.AiGenerateStartResponse;
import com.example.teamflow.domain.ai.dto.QuestionItem;
import com.example.teamflow.domain.ai.entity.AiRequestHistory;
import com.example.teamflow.domain.ai.entity.AiSession;
import com.example.teamflow.domain.ai.repository.AiRequestHistoryRepository;
import com.example.teamflow.domain.ai.repository.AiSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiProjectService {

    private static final Logger log = LoggerFactory.getLogger(AiProjectService.class);

    private final AiSessionRepository aiSessionRepository;
    private final AiRequestHistoryRepository aiRequestHistoryRepository;
    private final RequirementAgent requirementAgent;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiGenerateStartResponse startGeneration(String feature, Long memberId) {
        AiAgentResult<List<QuestionItem>> result = requirementAgent.generateQuestions(feature);

        String questionsJson = serialize(result.data());
        AiSession session = AiSession.create(memberId, feature, questionsJson);
        aiSessionRepository.save(session);

        aiRequestHistoryRepository.save(AiRequestHistory.create(
                memberId, null, AgentType.REQUIREMENT,
                result.promptUsed(), result.rawResponse(), result.tokenUsage()
        ));

        return new AiGenerateStartResponse(session.getId(), result.data());
    }

    public AiSession getValidSession(Long sessionId) {
        AiSession session = aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND));
        if (session.getStatus() != AiSessionStatus.QUESTIONING) {
            throw new BusinessException(ErrorCode.AI_SESSION_INVALID);
        }
        if (session.isExpired()) {
            throw new BusinessException(ErrorCode.AI_SESSION_EXPIRED);
        }
        return session;
    }

    // 외부 트랜잭션이 롤백되더라도 FAILED 상태는 반드시 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long sessionId) {
        aiSessionRepository.updateStatus(sessionId, AiSessionStatus.FAILED);
    }

    @Transactional
    public void cleanupExpiredSessions() {
        int deleted = aiSessionRepository.deleteExpiredSessions(LocalDateTime.now());
        log.info("만료된 AiSession 정리 완료 — {}건 삭제", deleted);
    }

    private String serialize(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
