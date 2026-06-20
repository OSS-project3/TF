package com.example.teamflow.domain.meeting.facade;

import com.example.teamflow.common.enums.TaskDifficulty;
import com.example.teamflow.domain.meeting.dto.MeetingTaskApplyResponse;
import com.example.teamflow.domain.meeting.entity.MeetingTodo;
import com.example.teamflow.domain.meeting.repository.MeetingTodoRepository;
import com.example.teamflow.domain.meeting.service.MeetingService;
import com.example.teamflow.domain.task.dto.TaskCreateRequest;
import com.example.teamflow.domain.task.dto.TaskCreateResponse;
import com.example.teamflow.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MeetingTaskFacade {

    private final MeetingService meetingService;
    private final TaskService taskService;
    private final MeetingTodoRepository meetingTodoRepository;

    @Transactional
    public MeetingTaskApplyResponse applyTodosToTasks(Long meetingId) {
        meetingService.findById(meetingId);

        List<MeetingTodo> todos =
                meetingTodoRepository.findAllByMeetingIdAndAppliedTaskIdIsNull(meetingId);

        List<Long> createdTaskIds = todos.stream()
                .map(todo -> {
                    TaskCreateResponse created = taskService.createTask(
                            todo.getProjectId(),
                            new TaskCreateRequest(
                                    todo.getTitle(),
                                    "개발",
                                    1,
                                    TaskDifficulty.EASY,
                                    todo.getAssigneeId(),
                                    null,
                                    todo.getDueDate(),
                                    null,
                                    null
                            )
                    );
                    todo.applyTask(created.id());
                    return created.id();
                })
                .toList();

        return new MeetingTaskApplyResponse(createdTaskIds, createdTaskIds.size());
    }
}
