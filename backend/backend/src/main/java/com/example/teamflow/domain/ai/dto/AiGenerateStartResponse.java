package com.example.teamflow.domain.ai.dto;

import java.util.List;

public record AiGenerateStartResponse(Long sessionId, List<QuestionItem> questions) {}
