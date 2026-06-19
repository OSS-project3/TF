package com.example.teamflow.domain.member.repository;

import com.example.teamflow.domain.member.entity.MemberSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberSkillRepository extends JpaRepository<MemberSkill, Long> {

    List<MemberSkill> findAllByMemberId(Long memberId);
}
