package com.example.teamflow.domain.ai.repository;

import com.example.teamflow.domain.ai.entity.AiRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestHistoryRepository extends JpaRepository<AiRequestHistory, Long> {
}
