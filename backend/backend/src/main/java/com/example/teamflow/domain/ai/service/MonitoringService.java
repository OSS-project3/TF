package com.example.teamflow.domain.ai.service;

import com.example.teamflow.common.enums.AgentType;
import com.example.teamflow.common.enums.ProjectStatus;
import com.example.teamflow.domain.ai.agent.RiskAgent;
import com.example.teamflow.domain.ai.detector.BottleneckDetector;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.ai.dto.AiRiskResponse;
import com.example.teamflow.domain.ai.dto.BottleneckReport;
import com.example.teamflow.domain.ai.dto.RiskItem;
import com.example.teamflow.domain.ai.entity.AgentDecisionLog;
import com.example.teamflow.domain.ai.entity.AiRequestHistory;
import com.example.teamflow.domain.ai.repository.AgentDecisionLogRepository;
import com.example.teamflow.domain.ai.repository.AiRequestHistoryRepository;
import com.example.teamflow.domain.project.dto.ProjectResponse;
import com.example.teamflow.domain.project.service.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final ProjectService projectService;
    private final BottleneckDetector bottleneckDetector;
    private final RiskAgent riskAgent;
    private final AgentDecisionLogRepository agentDecisionLogRepository;
    private final AiRequestHistoryRepository aiRequestHistoryRepository;
    private final ObjectMapper objectMapper;

    // 스케줄러에서 호출 — 전체 ACTIVE 프로젝트 순회
    @Transactional
    public void runDailyMonitoring() {
        List<ProjectResponse> projects = projectService.getProjects(null, ProjectStatus.ACTIVE);
        log.info("일일 모니터링 시작 — 대상 프로젝트: {}개", projects.size());

        for (ProjectResponse project : projects) {
            try {
                analyzeProject(project);
            } catch (Exception e) {
                log.error("프로젝트 {} 모니터링 실패: {}", project.id(), e.getMessage());
            }
        }
    }

    private void analyzeProject(ProjectResponse project) {
        BottleneckReport report = bottleneckDetector.detect(project);

        if (!report.hasIssue()) {
            log.debug("프로젝트 {} — 이상 없음, AI 호출 생략", project.id());
            return;
        }

        AiAgentResult<AiRiskResponse> result = riskAgent.analyze(report);

        String risksJson = serializeRisks(result.data().risks());
        agentDecisionLogRepository.save(
                AgentDecisionLog.create(project.id(), AgentType.MONITORING, risksJson));

        aiRequestHistoryRepository.save(AiRequestHistory.create(
                null, project.id(), AgentType.MONITORING,
                result.promptUsed(), result.rawResponse(), result.tokenUsage()));

        log.info("프로젝트 {} 위험 분석 완료 — 위험 {}개", project.id(), result.data().risks().size());
    }

    // GET /projects/{id}/risks 에서 반환
    @Transactional(readOnly = true)
    public AiRiskResponse getRisks(Long projectId) {
        List<AgentDecisionLog> logs = agentDecisionLogRepository
                .findTop5ByProjectIdOrderByCreatedAtDesc(projectId);

        if (logs.isEmpty()) {
            return new AiRiskResponse(List.of(), null);
        }

        AgentDecisionLog latest = logs.get(0);
        List<RiskItem> risks = parseRisks(latest.getRisksJson());
        return new AiRiskResponse(risks, latest.getCreatedAt());
    }

    private String serializeRisks(List<RiskItem> risks) {
        try {
            return objectMapper.writeValueAsString(risks);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<RiskItem> parseRisks(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RiskItem.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
