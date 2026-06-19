package com.example.teamflow.domain.meeting.repository;

import com.example.teamflow.domain.meeting.entity.MeetingAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, Long> {
}
