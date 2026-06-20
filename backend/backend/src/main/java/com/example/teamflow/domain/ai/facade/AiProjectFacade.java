package com.example.teamflow.domain.ai.facade;

import com.example.teamflow.common.enums.AgentType;
import com.example.teamflow.common.enums.TaskDifficulty;
import com.example.teamflow.domain.ai.agent.AssignmentAgent;
import com.example.teamflow.domain.ai.agent.TaskDecomposeAgent;
import com.example.teamflow.domain.ai.dto.AiAgentResult;
import com.example.teamflow.domain.ai.dto.AiGenerateCompleteResponse;
import com.example.teamflow.domain.ai.dto.AiGenerateStartResponse;
import com.example.teamflow.domain.ai.entity.AiRequestHistory;
import com.example.teamflow.domain.ai.entity.AiSession;
import com.example.teamflow.domain.ai.repository.AiRequestHistoryRepository;
import com.example.teamflow.domain.ai.service.AiProjectService;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.dto.ProjectCreateRequest;
import com.example.teamflow.domain.project.dto.ProjectCreateResponse;
import com.example.teamflow.domain.project.service.ProjectService;
import com.example.teamflow.domain.task.dto.TaskCreateRequest;
import com.example.teamflow.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiProjectFacade {

    private static final Logger log = LoggerFactory.getLogger(AiProjectFacade.class);

    private final AiProjectService aiProjectService;
    private final TaskDecomposeAgent taskDecomposeAgent;
    private final AssignmentAgent assignmentAgent;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final MemberService memberService;
    private final AiRequestHistoryRepository aiRequestHistoryRepository;

    public AiGenerateStartResponse startGeneration(String feature, Long memberId) {
        return aiProjectService.startGeneration(feature, memberId);
    }

    // AI 호출 + DB 저장을 하나의 트랜잭션으로 처리 (MVP 수준에서 충분)
    // 실패 시 전체 롤백 → 세션을 FAILED로 별도 업데이트
    @Transactional
    public AiGenerateCompleteResponse completeGeneration(Long sessionId,
                                                          Map<String, Object> answers,
                                                          Long memberId) {
        AiSession session = aiProjectService.getValidSession(sessionId);
        session.startProcessing(answers.toString());

        try {
            // AI 호출
            AiAgentResult<TaskDecomposeAgent.DecomposeResult> decomposeResult =
                    taskDecomposeAgent.decompose(session.getFeature(), answers);
            TaskDecomposeAgent.DecomposeResult decomposed = decomposeResult.data();

            List<String> taskTitles = decomposed.tasks().stream()
                    .map(TaskDecomposeAgent.TaskProposal::title).toList();

            List<MemberResponse> members = memberService.getMembers(null);
            AiAgentResult<List<AssignmentAgent.Assignment>> assignResult =
                    assignmentAgent.assign(taskTitles, members);

            // 이름 → ID 매핑 (대소문자 무시)
            Map<String, Long> nameToId = members.stream()
                    .collect(Collectors.toMap(
                            m -> m.name().toLowerCase(),
                            MemberResponse::id,
                            (a, b) -> a));

            // 프로젝트 생성
            ProjectCreateResponse project = projectService.createProject(new ProjectCreateRequest(
                    decomposed.projectName(),
                    decomposed.projectGoal(),
                    LocalDate.now().plusDays(30),
                    List.of(memberId)
            ));
            Long projectId = project.id();

            // AI 담당자 매핑 (대소문자 무시 매칭)
            Map<String, Long> assigneeMap = assignResult.data().stream()
                    .filter(a -> nameToId.containsKey(a.memberName().toLowerCase()))
                    .collect(Collectors.toMap(
                            AssignmentAgent.Assignment::taskTitle,
                            a -> nameToId.get(a.memberName().toLowerCase()),
                            (x, y) -> x
                    ));

            // 매핑 실패 태스크에 라운드로빈 폴백
            List<Long> memberIds = new ArrayList<>(members.stream().map(MemberResponse::id).toList());
            AtomicInteger robin = new AtomicInteger(0);

            int matched = (int) decomposed.tasks().stream()
                    .filter(p -> assigneeMap.containsKey(p.title())).count();
            int unmatched = decomposed.tasks().size() - matched;
            if (unmatched > 0) {
                log.warn("AI 담당자 매핑 실패 {} 건 — 라운드로빈으로 배분", unmatched);
            }

            // 태스크 생성 + 배정 멤버 수집
            Set<Long> autoMemberIds = new HashSet<>();
            for (TaskDecomposeAgent.TaskProposal proposal : decomposed.tasks()) {
                Long assigneeId = assigneeMap.get(proposal.title());
                if (assigneeId == null && !memberIds.isEmpty()) {
                    assigneeId = memberIds.get(robin.getAndIncrement() % memberIds.size());
                }
                if (assigneeId != null) autoMemberIds.add(assigneeId);
                taskService.createTask(projectId, new TaskCreateRequest(
                        proposal.title(),
                        proposal.phase(),
                        proposal.estimatedHours(),
                        parseDifficulty(proposal.difficulty()),
                        assigneeId,
                        null, null, null, null
                ));
            }

            // 배정된 멤버 자동 ProjectMember 등록 (요청자는 createProject에서 이미 추가됨)
            autoMemberIds.stream()
                    .filter(id -> !id.equals(memberId))
                    .forEach(id -> projectService.addProjectMember(projectId, id));

            // 히스토리 저장
            aiRequestHistoryRepository.save(AiRequestHistory.create(
                    memberId, projectId, AgentType.TASK,
                    decomposeResult.promptUsed(), decomposeResult.rawResponse(), decomposeResult.tokenUsage()
            ));
            aiRequestHistoryRepository.save(AiRequestHistory.create(
                    memberId, projectId, AgentType.ASSIGNMENT,
                    assignResult.promptUsed(), assignResult.rawResponse(), assignResult.tokenUsage()
            ));

            // 세션 완료
            session.complete(projectId);

            log.info("AI 프로젝트 생성 완료 — projectId: {}, tasks: {}", projectId, decomposed.tasks().size());
            return new AiGenerateCompleteResponse(projectId, decomposed.tasks().size());

        } catch (Exception e) {
            // 트랜잭션 롤백 후 REQUIRES_NEW로 세션 FAILED 마킹
            log.error("AI 프로젝트 생성 실패 — sessionId: {}", sessionId, e);
            aiProjectService.markFailed(sessionId);
            throw e;
        }
    }

    private TaskDifficulty parseDifficulty(String value) {
        try {
            return TaskDifficulty.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return TaskDifficulty.MEDIUM;
        }
    }
}
