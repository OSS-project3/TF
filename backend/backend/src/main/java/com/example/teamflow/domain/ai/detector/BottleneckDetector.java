package com.example.teamflow.domain.ai.detector;

import com.example.teamflow.domain.ai.dto.BottleneckReport;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.dto.ProjectResponse;
import com.example.teamflow.domain.task.entity.Task;
import com.example.teamflow.domain.task.service.TaskAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BottleneckDetector {

    // IN_PROGRESS 상태가 5일 이상 지속되면 정체 작업으로 판단
    private static final int STUCK_DAYS = 5;

    // 담당자의 진행 중 작업 수가 3개를 초과하면 과부하로 판단
    private static final int OVERLOAD_THRESHOLD = 3;

    private final TaskAggregationService taskAggregationService;
    private final MemberService memberService;

    /**
     * 프로젝트 내 병목 요소를 탐지한다.
     *
     * 감지 대상
     * 1. 지연 작업(Late Task)
     * 2. BLOCKED 상태 작업
     * 3. 장기간 정체 작업(Stuck Task)
     * 4. 업무 과부하 담당자
     */
    public BottleneckReport detect(ProjectResponse project) {
        Long projectId = project.id();

        // 지연 작업 조회
        List<Task> lateTasks = taskAggregationService.findLateByProjectId(projectId);

        // BLOCKED 상태 작업 조회
        List<Task> blockedTasks = taskAggregationService.findBlockedByProjectId(projectId);

        // 일정 기간 이상 정체된 작업 조회
        List<Task> stuckTasks = taskAggregationService.findStuckInProgress(projectId, STUCK_DAYS);

        // 병목 작업 담당자 ID 수집
        List<Long> allAssigneeIds = collectAssigneeIds(lateTasks, blockedTasks, stuckTasks);

        // 담당자 ID → 이름 매핑 생성
        Map<Long, String> memberNameMap = buildMemberNameMap(allAssigneeIds);

        // 진행 중 작업 수 기반 과부하 담당자 탐지
        List<BottleneckReport.MemberWorkloadInfo> overloaded =
                detectOverloadedMembers(projectId);

        // AI 분석에 사용할 병목 리포트 생성
        return new BottleneckReport(
                projectId,
                project.name(),
                toTaskInfoList(lateTasks, memberNameMap),
                toTaskInfoList(blockedTasks, memberNameMap),
                toTaskInfoList(stuckTasks, memberNameMap),
                overloaded
        );
    }

    /**
     * 병목 작업들의 담당자 ID를 수집한다.
     */
    private List<Long> collectAssigneeIds(List<Task> a, List<Task> b, List<Task> c) {
        List<Long> ids = new ArrayList<>();

        for (List<Task> tasks : List.of(a, b, c)) {
            tasks.stream()
                    .filter(t -> !t.getAssigneeIds().isEmpty())
                    .flatMap(t -> t.getAssigneeIds().stream())
                    .forEach(ids::add);
        }

        return ids.stream().distinct().toList();
    }

    /**
     * 담당자 ID를 이름으로 변환하기 위한 매핑 생성.
     *
     * 예)
     * 1L → 김철수
     * 2L → 이영희
     */
    private Map<Long, String> buildMemberNameMap(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return memberService.getMembersByIds(memberIds).stream()
                .collect(Collectors.toMap(
                        MemberResponse::id,
                        MemberResponse::name
                ));
    }

    /**
     * Task 엔티티를 리포트용 DTO로 변환한다.
     */
    private List<BottleneckReport.TaskInfo> toTaskInfoList(
            List<Task> tasks,
            Map<Long, String> nameMap
    ) {
        return tasks.stream()
                .map(t -> new BottleneckReport.TaskInfo(
                        t.getId(),
                        t.getTitle(),
                        t.getAssigneeIds().isEmpty()
                                ? "미배정"
                                : t.getAssigneeIds().stream()
                                        .map(id -> nameMap.getOrDefault(id, "미배정"))
                                        .collect(Collectors.joining(", "))
                ))
                .toList();
    }

    /**
     * 담당자별 진행 중(IN_PROGRESS) 작업 수를 집계하여
     * 과부하 상태의 담당자를 탐지한다.
     */
    private List<BottleneckReport.MemberWorkloadInfo> detectOverloadedMembers(Long projectId) {

        // 담당자별 진행 중 작업 수 조회
        List<Object[]> counts =
                taskAggregationService.countInProgressByAssignee(projectId);

        // 기준치를 초과한 담당자 추출
        List<Long> overloadedIds = counts.stream()
                .filter(row -> ((Number) row[1]).intValue() > OVERLOAD_THRESHOLD)
                .map(row -> (Long) row[0])
                .toList();

        if (overloadedIds.isEmpty()) {
            return List.of();
        }

        // 담당자별 작업 수 저장
        Map<Long, Integer> countMap = counts.stream()
                .filter(row -> ((Number) row[1]).intValue() > OVERLOAD_THRESHOLD)
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // 리포트 DTO 변환
        return memberService.getMembersByIds(overloadedIds).stream()
                .map(m -> new BottleneckReport.MemberWorkloadInfo(
                        m.id(),
                        m.name(),
                        countMap.getOrDefault(m.id(), 0)
                ))
                .toList();
    }
}