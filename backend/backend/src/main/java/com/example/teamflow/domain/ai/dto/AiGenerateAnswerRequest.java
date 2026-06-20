package com.example.teamflow.domain.ai.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AiGenerateAnswerRequest(@NotNull Map<String, Object> answers) {}
