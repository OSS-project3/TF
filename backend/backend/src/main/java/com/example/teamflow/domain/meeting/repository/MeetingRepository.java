package com.example.teamflow.domain.meeting.repository;

import com.example.teamflow.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @Query("SELECT m FROM Meeting m WHERE " +
           "(:from IS NULL OR m.date >= :from) AND (:to IS NULL OR m.date <= :to) " +
           "ORDER BY m.date DESC")
    List<Meeting> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT DISTINCT m FROM Meeting m JOIN m.todos t WHERE t.projectId = :projectId " +
           "AND (:from IS NULL OR m.date >= :from) AND (:to IS NULL OR m.date <= :to) " +
           "ORDER BY m.date DESC")
    List<Meeting> findByProjectIdAndDateRange(@Param("projectId") Long projectId,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
