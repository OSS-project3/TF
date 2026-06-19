package com.example.teamflow.domain.meeting.service;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.meeting.dto.MeetingCreateRequest;
import com.example.teamflow.domain.meeting.dto.MeetingCreateResponse;
import com.example.teamflow.domain.meeting.dto.MeetingResponse;
import com.example.teamflow.domain.meeting.dto.MeetingTodoRequest;
import com.example.teamflow.domain.meeting.entity.Meeting;
import com.example.teamflow.domain.meeting.repository.MeetingRepository;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MemberService memberService;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetings(Long projectId, LocalDate from, LocalDate to) {
        List<Meeting> meetings = (projectId != null)
                ? meetingRepository.findByProjectIdAndDateRange(projectId, from, to)
                : meetingRepository.findByDateRange(from, to);
        return meetings.stream().map(MeetingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(Long meetingId) {
        return MeetingResponse.from(findById(meetingId));
    }

    @Transactional
    public MeetingCreateResponse createMeeting(MeetingCreateRequest req) {
        req.attendeeMemberIds().forEach(memberService::findById);

        List<MeetingTodoRequest> todos = req.todos() != null ? req.todos() : List.of();
        todos.forEach(t -> {
            memberService.findById(t.assigneeId());
            projectService.findById(t.projectId());
        });

        Meeting meeting = Meeting.create(req.title(), req.date(), req.notes(), req.manual());

        req.attendeeMemberIds().forEach(meeting::addAttendee);

        List<String> summary = req.summary() != null ? req.summary() : List.of();
        for (int i = 0; i < summary.size(); i++) {
            meeting.addSummaryItem(i, summary.get(i));
        }

        todos.forEach(t -> meeting.addTodo(t.assigneeId(), t.projectId(), t.title(), t.dueDate()));

        Meeting saved = meetingRepository.save(meeting);
        return new MeetingCreateResponse(saved.getId());
    }

    public Meeting findById(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
    }
}
