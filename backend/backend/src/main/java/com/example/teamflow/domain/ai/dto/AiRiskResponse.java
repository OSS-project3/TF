package com.example.teamflow.domain.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiRiskResponse(List<RiskItem> risks, LocalDateTime analyzedAt) {}
