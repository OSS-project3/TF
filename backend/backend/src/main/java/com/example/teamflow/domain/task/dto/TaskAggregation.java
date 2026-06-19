package com.example.teamflow.domain.task.dto;

public record TaskAggregation(int total, int done, int late) {

    public static TaskAggregation empty() {
        return new TaskAggregation(0, 0, 0);
    }
}
