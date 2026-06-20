package com.example.teamflow.domain.member.repository;

import com.example.teamflow.common.enums.MemberRole;
import com.example.teamflow.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.skills WHERE m.role = :role")
    List<Member> findAllByRoleWithSkills(MemberRole role);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.skills")
    List<Member> findAllWithSkills();

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.skills WHERE m.id IN :ids")
    List<Member> findAllByIdInWithSkills(List<Long> ids);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.skills WHERE m.workspaceId = :workspaceId")
    List<Member> findAllByWorkspaceIdWithSkills(@Param("workspaceId") Long workspaceId);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.skills WHERE m.workspaceId = :workspaceId AND m.role = :role")
    List<Member> findAllByWorkspaceIdAndRoleWithSkills(@Param("workspaceId") Long workspaceId, @Param("role") MemberRole role);
}
