package com.example.teamflow.domain.ai.repository;

import com.example.teamflow.domain.ai.entity.AgentDecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentDecisionLogRepository extends JpaRepository<AgentDecisionLog, Long> {

    List<AgentDecisionLog> findTop5ByProjectIdOrderByCreatedAtDesc(Long projectId);
}
