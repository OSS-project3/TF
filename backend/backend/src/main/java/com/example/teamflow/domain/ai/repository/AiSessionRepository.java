package com.example.teamflow.domain.ai.repository;

import com.example.teamflow.domain.ai.entity.AiSession;
import com.example.teamflow.common.enums.AiSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface AiSessionRepository extends JpaRepository<AiSession, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE AiSession s SET s.status = :status WHERE s.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") AiSessionStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM AiSession s WHERE s.status = 'QUESTIONING' AND s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
}
