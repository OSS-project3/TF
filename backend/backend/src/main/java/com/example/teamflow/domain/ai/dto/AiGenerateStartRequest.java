package com.example.teamflow.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiGenerateStartRequest(@NotBlank String feature) {}
