package com.example.teamflow.domain.task.dto;

import java.time.LocalDate;

public record TaskSummary(int estimatedHours, Long projectId, LocalDate startDate, LocalDate endDate) {}
