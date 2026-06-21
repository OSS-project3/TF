package com.example.teamflow.domain.ai.repository;

import com.example.teamflow.common.enums.AgentType;
import com.example.teamflow.domain.ai.entity.AiRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRequestHistoryRepository extends JpaRepository<AiRequestHistory, Long> {

    List<AiRequestHistory> findTop20ByProjectIdInAndAgentTypeInOrderByCreatedAtDesc(
            List<Long> projectIds, List<AgentType> types);
}
