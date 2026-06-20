package com.example.teamflow.domain.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubPrPayload(
        String action,
        @JsonProperty("pull_request") PullRequest pullRequest
) {
    public record PullRequest(
            boolean merged,
            Head head
    ) {}

    public record Head(String ref) {}

    public boolean isMerged() {
        return "closed".equals(action)
                && pullRequest != null
                && pullRequest.merged();
    }

    public String mergedBranch() {
        return pullRequest != null && pullRequest.head() != null
                ? pullRequest.head().ref()
                : null;
    }
}
