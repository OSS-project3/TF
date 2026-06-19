package com.example.teamflow.domain.meeting.repository;

import com.example.teamflow.domain.meeting.entity.MeetingTodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingTodoRepository extends JpaRepository<MeetingTodo, Long> {

    List<MeetingTodo> findAllByMeetingIdAndAppliedTaskIdIsNull(Long meetingId);
}
