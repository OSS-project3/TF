package com.example.teamflow.domain.meeting.repository;

import com.example.teamflow.domain.meeting.entity.MeetingSummaryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSummaryItemRepository extends JpaRepository<MeetingSummaryItem, Long> {
}
