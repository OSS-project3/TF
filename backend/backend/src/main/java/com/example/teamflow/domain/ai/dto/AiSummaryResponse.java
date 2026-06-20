package com.example.teamflow.domain.ai.dto;

import java.util.List;

public record AiSummaryResponse(List<String> summary, List<TodoItem> todos) {}
