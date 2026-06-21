package com.example.teamflow.domain.member.dto;

public record LoginResponse(String accessToken, boolean needsRoleSetup) {
    public LoginResponse(String accessToken) {
        this(accessToken, false);
    }
}
