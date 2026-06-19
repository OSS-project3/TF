package com.example.teamflow.domain.task.repository;

import com.example.teamflow.domain.task.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findAllByTaskId(Long taskId);

    List<TaskDependency> findAllByTaskIdIn(List<Long> taskIds);

    void deleteAllByTaskId(Long taskId);
}
