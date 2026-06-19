package com.example.teamflow.domain.project.repository;

import com.example.teamflow.common.enums.ProjectStatus;
import com.example.teamflow.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByStatus(ProjectStatus status);

    @Query("SELECT DISTINCT p FROM Project p JOIN ProjectMember pm ON pm.projectId = p.id " +
           "WHERE pm.memberId = :memberId AND p.status = :status")
    List<Project> findAllByMemberIdAndStatus(@Param("memberId") Long memberId,
                                             @Param("status") ProjectStatus status);
}
