package com.example.teamflow.domain.task.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import com.example.teamflow.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_execution_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskExecutionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private TaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private TaskStatus toStatus;

    public static TaskExecutionLog create(Long taskId, Long memberId,
                                          TaskStatus fromStatus, TaskStatus toStatus) {
        TaskExecutionLog log = new TaskExecutionLog();
        log.taskId = taskId;
        log.memberId = memberId;
        log.fromStatus = fromStatus;
        log.toStatus = toStatus;
        return log;
    }
}
