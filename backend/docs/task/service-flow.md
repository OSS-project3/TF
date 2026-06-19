# Task 도메인 — Service 흐름

## 담당 Service

- `com.teamflow.task.service.TaskService` — CRUD 및 상태 변경
- `com.teamflow.task.service.TaskAggregationService` — Project용 집계 계산

---

## 의존 도메인

| 도메인 | 메서드 | 이유 |
|--------|--------|------|
| `ProjectService` | `getProject(id)` | 프로젝트 존재 확인 |
| `MemberService` | `getMember(id)` | 담당자 존재 확인 |

---

## TaskService 주요 메서드

### `getTasksByProject(Long projectId, TaskFilter filter): List<TaskResponse>`

```
1. projectService.getProject(projectId) → PROJECT_NOT_FOUND
2. TaskRepository.findByProjectIdWithFilter(projectId, filter)
   (동적 조건: assigneeId, status, phase — QueryDSL 권장)
3. 각 Task의 dependencyTaskIds 조회 (TaskDependencyRepository)
4. List<TaskResponse> 변환 반환
```

@Transactional(readOnly = true)

---

### `createTask(Long projectId, TaskCreateRequest req): TaskCreateResponse`

```
1. projectService.getProject(projectId) → PROJECT_NOT_FOUND
2. req.assigneeId가 있으면 memberService.getMember(id) → MEMBER_NOT_FOUND
3. Task 엔티티 생성 및 저장
4. TaskCreateResponse { id } 반환
```

@Transactional

---

### `updateTask(Long taskId, TaskUpdateRequest req): TaskResponse`

```
1. taskRepository.findById(taskId) → TASK_NOT_FOUND
2. null 체크 후 각 필드 업데이트 (task.updateTitle(), task.updateDates(), ...)
3. TaskResponse 반환
```

@Transactional

---

### `changeStatus(Long taskId, TaskStatus newStatus)`

```
1. taskRepository.findById(taskId) → TASK_NOT_FOUND
2. 상태 전이 유효성 검사 (선택적 — 현재 명세에 전이 제약 없으나 추후 추가 가능)
3. task.changeStatus(newStatus)
```

@Transactional

---

### `changeAssignee(Long taskId, Long memberId)`

```
1. taskRepository.findById(taskId) → TASK_NOT_FOUND
2. memberService.getMember(memberId) → MEMBER_NOT_FOUND
3. task.assignTo(memberId)
```

@Transactional

---

### `getMyTasks(Long memberId, LocalDate from, LocalDate to, TaskStatus status): MyTasksResponse`

```
1. assigneeId = memberId인 Task 조회 (status 필터 선택적)
2. 각 Task를 endDate 기준으로 그룹핑:
   - today: endDate = LocalDate.now()
   - thisWeek: endDate between now+1 and endOfWeek
   - later: 그 이후
3. MyTasksResponse { today, thisWeek, later } 반환
```

@Transactional(readOnly = true)

---

## TaskAggregationService 주요 메서드

### `getAggregation(Long projectId): TaskAggregation`

ProjectService, DashboardService에서 집계 계산 시 호출.

```
1. taskRepository.countByProjectId(projectId) → total
2. taskRepository.countByProjectIdAndStatus(projectId, DONE) → done
3. taskRepository.countLateByProjectId(projectId, LocalDate.now()) → late
   (endDate < today AND status != DONE)
4. TaskAggregation { total, done, late } 반환
```

@Transactional(readOnly = true)

---

### `findByAssigneeAndDateRange(Long memberId, LocalDate from, LocalDate to): List<TaskSummary>`

MemberService의 workload 계산에서 호출.

```
1. assigneeId = memberId, startDate/endDate 기간 겹치는 Task 조회
2. List<TaskSummary { estimatedHours, projectId }> 반환
```

@Transactional(readOnly = true)

---

## 비즈니스 규칙

- Task는 `projectId`, `assigneeId`를 ID로만 보유 — 연관 엔티티 직접 로딩 없음
- `dependencyTaskIds`: Task 조회 시 TaskDependencyRepository에서 batch 조회 (N+1 방지)
- 순환 의존 방지: `POST /projects/{id}/tasks`에서 `dependencyTaskIds` 지정 시 순환 검사
  → DFS로 탐지, 감지되면 `CIRCULAR_TASK_DEPENDENCY` (422)
- `isLateRisk`, `isCriticalPath`는 AI/Schedule 제안 적용 시 갱신 — 수동 변경도 허용

---

## ScheduleService

`com.example.teamflow.task.service.ScheduleService`

Schedule은 Task 기반 뷰. 별도 Repository 없음. `task` 패키지 내 위치.

### 의존

| 도메인 | 메서드 | 이유 |
|--------|--------|------|
| `TaskService` | `createTask(...)`, `updateTask(...)` | Task 생성/수정 위임 |
| `ProjectService` | `getProject(id)` | 프로젝트명 조회 |
| `MemberService` | `getMember(id)` | 담당자 존재 확인 |

### `getSchedules(Long projectId, Long memberId, LocalDate from, LocalDate to): List<ScheduleResponse>`

```
1. projectId가 있으면 해당 프로젝트 Task 조회 (startDate NOT NULL)
   없으면 memberId 기준 전체 프로젝트 Task 조회
2. 기간 필터: from~to 범위와 startDate~endDate 겹치는 Task만
3. memberId 필터: assigneeId = memberId
4. 각 Task의 projectName 조회 (ProjectService 호출 — N+1 방지: projectId 일괄 수집 후 in-query)
5. kind 판정 후 ScheduleResponse 변환 반환
```

@Transactional(readOnly = true)

### `createSchedule(ScheduleCreateRequest req): ScheduleCreateResponse`

```
1. projectService.getProject(req.projectId) — 존재 확인 및 이름 조회
2. memberService.getMember(req.ownerId) — 존재 확인
3. TaskCreateRequest 변환 후 taskService.createTask(projectId, taskReq)
4. ScheduleCreateResponse { id: "schedule-{taskId}", taskId } 반환
```

@Transactional

### `updateSchedule(Long taskId, ScheduleUpdateRequest req)`

```
1. TaskUpdateRequest 변환 (ownerId → assigneeId 포함)
2. taskService.updateTask(taskId, taskReq)
```

@Transactional
