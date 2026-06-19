package com.example.teamflow.domain.task.dto;

import java.util.List;

public record MyTasksResponse(
        List<TaskResponse> today,
        List<TaskResponse> thisWeek,
        List<TaskResponse> later
) {}
