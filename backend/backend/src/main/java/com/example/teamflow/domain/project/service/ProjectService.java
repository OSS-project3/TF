package com.example.teamflow.domain.project.service;

import com.example.teamflow.common.enums.ProjectStatus;
import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.common.security.WorkspaceContext;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.dto.*;
import com.example.teamflow.domain.project.entity.Project;
import com.example.teamflow.domain.project.entity.ProjectMember;
import com.example.teamflow.domain.project.repository.ProjectMemberRepository;
import com.example.teamflow.domain.project.repository.ProjectRepository;
import com.example.teamflow.domain.task.dto.TaskAggregation;
import com.example.teamflow.domain.task.service.TaskAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberService memberService;
    private final TaskAggregationService taskAggregationService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(Long memberId, ProjectStatus status) {
        ProjectStatus effectiveStatus = (status != null) ? status : ProjectStatus.ACTIVE;
        Long workspaceId = WorkspaceContext.get();

        List<Project> projects = (memberId != null)
                ? projectRepository.findAllByMemberIdAndWorkspaceIdAndStatus(memberId, workspaceId, effectiveStatus)
                : projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, effectiveStatus);

        return projects.stream()
                .map(p -> toResponse(p))
                .toList();
    }

    @Transactional
    public ProjectCreateResponse createProject(ProjectCreateRequest request) {
        validateDeadlineRange(request.deadline());
        if (request.memberIds() != null) {
            memberService.validateMembersExist(request.memberIds());
        }
        Long workspaceId = WorkspaceContext.get();

        Project project = Project.create(request.name(), request.goal(), request.deadline(), workspaceId);
        projectRepository.save(project);

        if (request.memberIds() != null) {
            List<ProjectMember> members = request.memberIds().stream()
                    .map(id -> ProjectMember.create(project.getId(), id))
                    .toList();
            projectMemberRepository.saveAll(members);
        }

        return new ProjectCreateResponse(project.getId());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        Project project = findById(projectId);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request) {
        validateDeadlineRange(request.deadline());
        Project project = findById(projectId);
        project.updateName(request.name());
        project.updateGoal(request.goal());
        project.updateDeadline(request.deadline());
        return toResponse(project);
    }

    @Transactional
    public void archiveProject(Long projectId) {
        Project project = findById(projectId);
        project.archive();
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getProjectMembers(Long projectId) {
        findById(projectId);
        List<Long> memberIds = projectMemberRepository.findAllByProjectId(projectId).stream()
                .map(ProjectMember::getMemberId)
                .toList();
        return memberService.getMembersByIds(memberIds);
    }

    @Transactional
    public void replaceProjectMembers(Long projectId, ProjectMemberReplaceRequest request) {
        findById(projectId);
        memberService.validateMembersExist(request.memberIds());
        projectMemberRepository.deleteAllByProjectId(projectId);
        List<ProjectMember> members = request.memberIds().stream()
                .map(id -> ProjectMember.create(projectId, id))
                .toList();
        projectMemberRepository.saveAll(members);
    }

    @Transactional
    public void addProjectMember(Long projectId, Long memberId) {
        findById(projectId);
        memberService.findById(memberId);
        if (projectMemberRepository.existsByProjectIdAndMemberId(projectId, memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PROJECT_MEMBER);
        }
        projectMemberRepository.save(ProjectMember.create(projectId, memberId));
    }

    @Transactional
    public void removeProjectMember(Long projectId, Long memberId) {
        ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberId(projectId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        projectMemberRepository.delete(pm);
    }

    public Project findById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    public List<Long> getMemberIds(Long projectId) {
        return projectMemberRepository.findAllByProjectId(projectId).stream()
                .map(ProjectMember::getMemberId)
                .toList();
    }

    @Transactional
    public void removeMemberFromAllProjects(Long memberId) {
        projectMemberRepository.deleteAllByMemberId(memberId);
    }

    private void validateDeadlineRange(LocalDate deadline) {
        if (deadline != null && deadline.isAfter(LocalDate.now().plusYears(5))) {
            throw new BusinessException(ErrorCode.INVALID_DEADLINE);
        }
    }

    private ProjectResponse toResponse(Project project) {
        List<Long> memberIds = projectMemberRepository.findAllByProjectId(project.getId()).stream()
                .map(ProjectMember::getMemberId)
                .toList();
        TaskAggregation agg = taskAggregationService.getAggregation(project.getId());
        return ProjectResponse.of(project, memberIds, agg);
    }
}
