package com.example.teamflow.domain.ai.scheduler;

import com.example.teamflow.domain.ai.service.AiProjectService;
import com.example.teamflow.domain.ai.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final MonitoringService monitoringService;
    private final AiProjectService aiProjectService;

    @Scheduled(cron = "0 0 9 * * *")
    public void runDailyMonitoring() {
        log.info("일일 프로젝트 모니터링 스케줄러 시작");
        monitoringService.runDailyMonitoring();
        log.info("일일 프로젝트 모니터링 스케줄러 완료");
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredSessions() {
        log.info("만료 AiSession 정리 시작");
        aiProjectService.cleanupExpiredSessions();
    }
}
