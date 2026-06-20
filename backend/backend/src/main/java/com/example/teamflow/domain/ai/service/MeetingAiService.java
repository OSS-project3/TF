package com.example.teamflow.domain.ai.service;

import com.example.teamflow.common.enums.AgentType;
import com.example.teamflow.domain.ai.agent.MeetingAgent;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.ai.dto.AiSummaryResponse;
import com.example.teamflow.domain.ai.entity.AiRequestHistory;
import com.example.teamflow.domain.ai.repository.AiRequestHistoryRepository;
import com.example.teamflow.domain.meeting.dto.MeetingResponse;
import com.example.teamflow.domain.meeting.service.MeetingService;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingAiService {

    private final MeetingService meetingService;
    private final MeetingAgent meetingAgent;
    private final AiRequestHistoryRepository aiRequestHistoryRepository;
    private final ProjectService projectService;
    private final MemberService memberService;

    @Transactional
    public AiSummaryResponse summarize(Long meetingId, Long memberId) {
        MeetingResponse meeting = meetingService.getMeeting(meetingId);

        AiAgentResult<AiSummaryResponse> result = meetingAgent.analyze(meeting);

        aiRequestHistoryRepository.save(AiRequestHistory.create(
                memberId,
                null,
                AgentType.MEETING,
                result.promptUsed(),
                result.rawResponse(),
                result.tokenUsage()
        ));

        return result.data();
    }

    @Transactional
    public AiSummaryResponse summarizeFromNotes(String notes, Long projectId, Long memberId) {
        List<Long> attendeeIds = List.of();
        String enrichedNotes = notes;

        if (projectId != null) {
            attendeeIds = projectService.getMemberIds(projectId);
            if (!attendeeIds.isEmpty()) {
                String names = attendeeIds.stream()
                        .map(id -> memberService.findById(id).getName())
                        .collect(Collectors.joining(", "));
                enrichedNotes = "참석자: " + names + "\n\n" + notes;
            }
        }

        MeetingResponse meeting = new MeetingResponse(
                null, "직접 요약", LocalDate.now(), attendeeIds, enrichedNotes, List.of(), List.of(), false);

        AiAgentResult<AiSummaryResponse> result = meetingAgent.analyze(meeting);

        aiRequestHistoryRepository.save(AiRequestHistory.create(
                memberId, null, AgentType.MEETING,
                result.promptUsed(), result.rawResponse(), result.tokenUsage()));

        return result.data();
    }
}
