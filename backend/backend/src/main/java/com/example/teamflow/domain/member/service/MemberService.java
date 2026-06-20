package com.example.teamflow.domain.member.service;

import com.example.teamflow.common.enums.MemberRole;
import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.common.security.WorkspaceContext;
import com.example.teamflow.domain.member.dto.MemberResponse;
import com.example.teamflow.domain.member.dto.MemberUpdateRequest;
import com.example.teamflow.domain.member.dto.PasswordChangeRequest;
import com.example.teamflow.domain.member.dto.TeamWorkloadResponse;
import com.example.teamflow.domain.member.dto.WorkloadResponse;
import com.example.teamflow.domain.member.entity.Member;
import com.example.teamflow.domain.member.repository.MemberRepository;
import com.example.teamflow.domain.task.dto.TaskSummary;
import com.example.teamflow.domain.task.service.TaskAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final TaskAggregationService taskAggregationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MemberResponse getMe(Long memberId) {
        Member member = findById(memberId);
        return MemberResponse.from(member);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(MemberRole role) {
        Long workspaceId = WorkspaceContext.get();
        List<Member> members = (role != null)
                ? memberRepository.findAllByWorkspaceIdAndRoleWithSkills(workspaceId, role)
                : memberRepository.findAllByWorkspaceIdWithSkills(workspaceId);
        return members.stream().map(MemberResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(findById(memberId));
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembersByIds(List<Long> memberIds) {
        return memberRepository.findAllByIdInWithSkills(memberIds).stream()
                .map(MemberResponse::from)
                .toList();
    }

    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public void validateMembersExist(List<Long> memberIds) {
        memberIds.forEach(this::findById);
    }

    @Transactional
    public MemberResponse updateMe(Long memberId, MemberUpdateRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateProfile(req.name(), req.initial(), req.weeklyCapacityHours());
        if (req.skills() != null) {
            member.clearSkills();
            req.skills().forEach(member::addSkill);
        }
        return MemberResponse.from(member);
    }

    @Transactional
    public void changePassword(Long memberId, PasswordChangeRequest req) {
        Member member = findById(memberId);
        if (!passwordEncoder.matches(req.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }
        member.updatePassword(passwordEncoder.encode(req.newPassword()));
    }

    @Transactional
    public void deleteMe(Long memberId) {
        Member member = findById(memberId);
        memberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public WorkloadResponse getWorkload(Long memberId, LocalDate from, LocalDate to) {
        Member member = findById(memberId);
        return computeWorkload(member, from, to);
    }

    @Transactional(readOnly = true)
    public List<TeamWorkloadResponse> getTeamWorkloads(List<Long> memberIds, LocalDate from, LocalDate to) {
        Long workspaceId = WorkspaceContext.get();
        List<Member> members = (memberIds != null)
                ? memberRepository.findAllByIdInWithSkills(memberIds)
                : memberRepository.findAllByWorkspaceIdWithSkills(workspaceId);
        return members.stream()
                .map(m -> {
                    WorkloadResponse w = computeWorkload(m, from, to);
                    List<String> skills = m.getSkills().stream()
                            .map(s -> s.getSkill())
                            .toList();
                    return new TeamWorkloadResponse(
                            m.getId(), m.getName(), m.getRole().name(),
                            m.getWeeklyCapacityHours(), w.assignedHours(), w.loadRate(),
                            w.taskCount(), w.projectCount(), skills);
                })
                .toList();
    }

    private WorkloadResponse computeWorkload(Member member, LocalDate from, LocalDate to) {
        List<TaskSummary> tasks = taskAggregationService.findByAssigneeAndDateRange(member.getId(), from, to);
        int assignedHours = tasks.stream().mapToInt(TaskSummary::estimatedHours).sum();
        double loadRate = member.getWeeklyCapacityHours() == 0 ? 0.0
                : (double) assignedHours / member.getWeeklyCapacityHours();
        int projectCount = (int) tasks.stream().map(TaskSummary::projectId).distinct().count();
        return new WorkloadResponse(
                member.getId(), member.getWeeklyCapacityHours(),
                assignedHours, loadRate, projectCount, tasks.size());
    }
}
