package com.example.teamflow.domain.member.dto;

import com.example.teamflow.domain.member.entity.Member;

import java.util.List;

public record MemberResponse(
        Long id,
        String name,
        String role,
        String initial,
        int weeklyCapacityHours,
        List<String> skills
) {
    public static MemberResponse from(Member member) {
        List<String> skillNames = member.getSkills().stream()
                .map(s -> s.getSkill())
                .toList();
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getRole().name(),
                member.getInitial(),
                member.getWeeklyCapacityHours(),
                skillNames
        );
    }
}
