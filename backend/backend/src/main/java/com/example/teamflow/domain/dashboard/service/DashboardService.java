package com.example.teamflow.domain.dashboard.service;

import com.example.teamflow.common.enums.ProjectStatus;
import com.example.teamflow.domain.dashboard.dto.MemberDashboardResponse;
import com.example.teamflow.domain.dashboard.dto.PmDashboardResponse;
import com.example.teamflow.domain.dashboard.dto.ProjectSummary;
import com.example.teamflow.domain.member.dto.TeamWorkloadResponse;
import com.example.teamflow.domain.member.dto.WorkloadResponse;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.dto.ProjectResponse;
import com.example.teamflow.domain.project.service.ProjectService;
import com.example.teamflow.domain.task.dto.MyTasksResponse;
import com.example.teamflow.domain.task.dto.TaskResponse;
import com.example.teamflow.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProjectService projectService;
    private final MemberService memberService;
    private final TaskService taskService;

    public PmDashboardResponse getPmDashboard() {
        List<ProjectResponse> projects = projectService.getProjects(null, ProjectStatus.ACTIVE);

        int totalTaskCount = projects.stream().mapToInt(ProjectResponse::taskCount).sum();
        int doneTaskCount = projects.stream().mapToInt(ProjectResponse::doneTaskCount).sum();
        int lateTaskCount = projects.stream().mapToInt(ProjectResponse::lateTaskCount).sum();
        double averageProgress = projects.isEmpty() ? 0.0
                : projects.stream().mapToDouble(ProjectResponse::progress).average().orElse(0.0);

        int memberCount = memberService.getMembers(null).size();

        LocalDate from = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate to = LocalDate.now().with(DayOfWeek.SUNDAY);
        List<TeamWorkloadResponse> workloads = memberService.getTeamWorkloads(null, from, to);
        double averageLoadRate = workloads.isEmpty() ? 0.0
                : workloads.stream().mapToDouble(TeamWorkloadResponse::loadRate).average().orElse(0.0);

        return new PmDashboardResponse(
                projects.size(), averageProgress, totalTaskCount, doneTaskCount, lateTaskCount,
                memberCount, averageLoadRate, projects);
    }

    public MemberDashboardResponse getMemberDashboard(Long memberId) {
        LocalDate from = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate to = LocalDate.now().with(DayOfWeek.SUNDAY);

        WorkloadResponse workload = memberService.getWorkload(memberId, from, to);
        MyTasksResponse myTasks = taskService.getMyTasks(memberId, null);
        List<ProjectResponse> projectResponses = projectService.getProjects(memberId, ProjectStatus.ACTIVE);

        List<TaskResponse> allTasks = Stream.concat(
                Stream.concat(myTasks.today().stream(), myTasks.thisWeek().stream()),
                myTasks.later().stream()
        ).toList();

        int taskCount = allTasks.size();
        int doneTaskCount = (int) allTasks.stream()
                .filter(t -> "DONE".equals(t.status())).count();

        LocalDate nextDueDate = allTasks.stream()
                .filter(t -> !"DONE".equals(t.status()))
                .map(TaskResponse::endDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        List<ProjectSummary> summaries = projectResponses.stream()
                .map(p -> new ProjectSummary(p.id(), p.name(), p.progress(), p.health()))
                .toList();

        return new MemberDashboardResponse(
                workload.loadRate(), workload.assignedHours(), workload.capacityHours(),
                projectResponses.size(), taskCount, doneTaskCount, nextDueDate,
                myTasks.today(), myTasks.thisWeek(), myTasks.later(),
                summaries);
    }
}
