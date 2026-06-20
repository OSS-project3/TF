package com.example.teamflow.domain.ai.dto;

import java.util.List;

public record BottleneckReport(
        Long projectId,
        String projectName,
        List<TaskInfo> lateTasks,
        List<TaskInfo> blockedTasks,
        List<TaskInfo> stuckTasks,
        List<MemberWorkloadInfo> overloadedMembers
) {
    public boolean hasIssue() {
        return !lateTasks.isEmpty() || !blockedTasks.isEmpty() || !stuckTasks.isEmpty();
    }

    public record TaskInfo(Long taskId, String title, String assigneeName) {}
    public record MemberWorkloadInfo(Long memberId, String memberName, int taskCount) {}
}
