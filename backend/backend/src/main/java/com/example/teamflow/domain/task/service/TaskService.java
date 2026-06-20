package com.example.teamflow.domain.task.service;

import com.example.teamflow.common.enums.TaskStatus;
import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.member.service.MemberService;
import com.example.teamflow.domain.project.service.ProjectService;
import com.example.teamflow.domain.task.dto.*;
import com.example.teamflow.domain.task.entity.Task;
import com.example.teamflow.domain.task.entity.TaskDependency;
import com.example.teamflow.domain.task.entity.TaskExecutionLog;
import com.example.teamflow.domain.task.repository.TaskDependencyRepository;
import com.example.teamflow.domain.task.repository.TaskExecutionLogRepository;
import com.example.teamflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskExecutionLogRepository taskExecutionLogRepository;
    private final ProjectService projectService;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId, Long assigneeId,
                                                TaskStatus status, String phase) {
        projectService.findById(projectId);
        List<Task> tasks = taskRepository.findByProjectIdWithFilters(projectId, assigneeId, status, phase);
        return attachDependencies(tasks);
    }

    @Transactional
    public TaskCreateResponse createTask(Long projectId, TaskCreateRequest request) {
        projectService.findById(projectId);
        if (request.assigneeId() != null) {
            memberService.findById(request.assigneeId());
        }

        Task task = Task.create(
                projectId,
                request.title(),
                request.phase(),
                request.estimatedHours() != null ? request.estimatedHours() : 0,
                request.difficulty(),
                request.assigneeId(),
                request.startDate(),
                request.endDate()
        );
        if (request.gitBranch() != null) task.linkGitBranch(request.gitBranch().isBlank() ? null : request.gitBranch());
        taskRepository.save(task);

        if (request.dependencyTaskIds() != null && !request.dependencyTaskIds().isEmpty()) {
            validateNoCycle(task.getId(), request.dependencyTaskIds());
            List<TaskDependency> deps = request.dependencyTaskIds().stream()
                    .map(depId -> TaskDependency.create(task.getId(), depId))
                    .toList();
            taskDependencyRepository.saveAll(deps);
        }

        return new TaskCreateResponse(task.getId());
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        Task task = findById(taskId);
        task.updateTitle(request.title());
        task.updatePhase(request.phase());
        task.updateEstimatedHours(request.estimatedHours());
        task.updateDifficulty(request.difficulty());
        task.updateDates(request.startDate(), request.endDate());
        task.updateFlags(request.isCriticalPath(), request.isLateRisk());
        if (request.gitBranch() != null) task.linkGitBranch(request.gitBranch().isBlank() ? null : request.gitBranch());
        List<Long> deps = getDependencyIds(taskId);
        return TaskResponse.of(task, deps);
    }

    @Transactional
    public void changeStatus(Long taskId, TaskStatus newStatus, Long memberId) {
        Task task = findById(taskId);
        TaskStatus fromStatus = task.getStatus();
        task.changeStatus(newStatus);
        taskExecutionLogRepository.save(
                TaskExecutionLog.create(taskId, memberId, fromStatus, newStatus));
    }

    @Transactional
    public void changeAssignee(Long taskId, Long memberId) {
        Task task = findById(taskId);
        memberService.findById(memberId);
        task.assignTo(memberId);
    }

    @Transactional(readOnly = true)
    public MyTasksResponse getMyTasks(Long memberId, TaskStatus status) {
        List<Task> tasks = taskRepository.findByAssigneeId(memberId, status);
        Map<Long, List<Long>> depsMap = buildDepsMap(tasks.stream().map(Task::getId).toList());

        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        List<TaskResponse> todayList = new ArrayList<>();
        List<TaskResponse> thisWeekList = new ArrayList<>();
        List<TaskResponse> laterList = new ArrayList<>();

        for (Task task : tasks) {
            TaskResponse response = TaskResponse.of(task, depsMap.getOrDefault(task.getId(), List.of()));
            LocalDate endDate = task.getEndDate();
            if (endDate == null || endDate.isAfter(endOfWeek)) {
                laterList.add(response);
            } else if (endDate.isEqual(today)) {
                todayList.add(response);
            } else {
                thisWeekList.add(response);
            }
        }

        return new MyTasksResponse(todayList, thisWeekList, laterList);
    }

    @Transactional
    public int completeByGitBranch(String gitBranch) {
        List<Task> tasks = taskRepository.findByGitBranchAndNotDone(gitBranch);
        tasks.forEach(task -> {
            TaskStatus from = task.getStatus();
            task.changeStatus(TaskStatus.DONE);
            taskExecutionLogRepository.save(
                    TaskExecutionLog.create(task.getId(), null, from, TaskStatus.DONE));
        });
        return tasks.size();
    }

    @Transactional
    public void deleteTask(Long taskId) {
        findById(taskId);
        taskDependencyRepository.deleteAllByTaskId(taskId);
        taskDependencyRepository.deleteAllByPrerequisiteTaskId(taskId);
        taskExecutionLogRepository.deleteAllByTaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    public Task findById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private List<TaskResponse> attachDependencies(List<Task> tasks) {
        if (tasks.isEmpty()) return List.of();
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        Map<Long, List<Long>> depsMap = buildDepsMap(taskIds);
        return tasks.stream()
                .map(t -> TaskResponse.of(t, depsMap.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    private Map<Long, List<Long>> buildDepsMap(List<Long> taskIds) {
        if (taskIds.isEmpty()) return Map.of();
        return taskDependencyRepository.findAllByTaskIdIn(taskIds).stream()
                .collect(Collectors.groupingBy(
                        TaskDependency::getTaskId,
                        Collectors.mapping(TaskDependency::getPrerequisiteTaskId, Collectors.toList())
                ));
    }

    private List<Long> getDependencyIds(Long taskId) {
        return taskDependencyRepository.findAllByTaskId(taskId).stream()
                .map(TaskDependency::getPrerequisiteTaskId)
                .toList();
    }

    private void validateNoCycle(Long taskId, List<Long> depIds) {
        Set<Long> visited = new HashSet<>();
        for (Long depId : depIds) {
            if (hasCycle(taskId, depId, visited)) {
                throw new BusinessException(ErrorCode.CIRCULAR_TASK_DEPENDENCY);
            }
        }
    }

    private boolean hasCycle(Long target, Long current, Set<Long> visited) {
        if (current.equals(target)) return true;
        if (visited.contains(current)) return false;
        visited.add(current);
        List<Long> nextDeps = taskDependencyRepository.findAllByTaskId(current).stream()
                .map(TaskDependency::getPrerequisiteTaskId)
                .toList();
        for (Long next : nextDeps) {
            if (hasCycle(target, next, visited)) return true;
        }
        return false;
    }
}
