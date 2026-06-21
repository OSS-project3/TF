package com.example.teamflow.domain.project.repository;

import com.example.teamflow.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByProjectId(Long projectId);

    Optional<ProjectMember> findByProjectIdAndMemberId(Long projectId, Long memberId);

    boolean existsByProjectIdAndMemberId(Long projectId, Long memberId);

    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
