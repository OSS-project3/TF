package com.example.teamflow.infra.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenInfo(
        String sub,
        String email,
        @JsonProperty("email_verified") String emailVerified,
        String name,
        @JsonProperty("given_name") String givenName,
        String aud
) {}
