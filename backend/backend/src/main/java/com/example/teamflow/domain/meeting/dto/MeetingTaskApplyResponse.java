package com.example.teamflow.domain.meeting.dto;

import java.util.List;

public record MeetingTaskApplyResponse(List<Long> createdTaskIds, int count) {}
