# Meeting 도메인 — Service 흐름

## 담당 Service / Facade

- `com.teamflow.meeting.service.MeetingService` — 회의록 CRUD
- `com.teamflow.meeting.facade.MeetingTodoApplyFacade` — TODO → Task 등록 (3개 도메인 조합)

---

## 의존 도메인

| 도메인 | 메서드 | 이유 |
|--------|--------|------|
| `MemberService` | `getMember(id)` | 참석자·담당자 존재 확인 |
| `ProjectService` | `getProject(id)` | todo 대상 프로젝트 존재 확인 |
| `TaskService` | `createTask(projectId, req)` | TODO to Task 생성 시 사용 (Facade 경유) |

---

## MeetingService 주요 메서드

### `getMeetings(Long projectId, LocalDate from, LocalDate to): List<MeetingResponse>`

```
1. 필터 조건으로 Meeting 조회 (projectId → MeetingAttendee 통해 필터, 날짜 범위)
2. 각 Meeting의 attendees, summaryItems, todos 조회 (fetchJoin 권장)
3. List<MeetingResponse> 변환 반환
```

@Transactional(readOnly = true)

---

### `getMeeting(Long meetingId): MeetingResponse`

```
1. meetingRepository.findById(meetingId) → MEETING_NOT_FOUND
2. 연관 데이터 조회 (attendees, summaryItems, todos)
3. MeetingResponse 반환
```

@Transactional(readOnly = true)

---

### `createMeeting(MeetingCreateRequest req): MeetingCreateResponse`

```
1. req.attendeeMemberIds 각각 memberService.getMember(id) 존재 확인
2. req.todos의 assigneeId, projectId 유효성 확인
3. Meeting 엔티티 생성 및 저장
4. MeetingAttendee 행 일괄 생성 (saveAll)
5. MeetingSummaryItem 행 일괄 생성 (orderIndex = 리스트 인덱스)
6. MeetingTodo 행 일괄 생성 (applied_task_id = NULL)
7. MeetingCreateResponse { id } 반환
```

@Transactional

---

### `summarize(String notes, Long projectId): MeetingSummaryResponse`

```
1. projectService.getProject(projectId) → PROJECT_NOT_FOUND
2. 규칙 기반 파싱 or LLM 호출로 summary, todos 생성
3. DB 저장 없이 MeetingSummaryResponse 반환
```

@Transactional(readOnly = true)

Phase 3: `AiSummarizeService` 인터페이스 도입 (구현체 교체 가능성 있으므로 인터페이스 필요).

---

## MeetingTodoApplyFacade

3개 도메인(Meeting, Task, MeetingTodo) 조합 → Facade 패턴 사용.

```java
@Component
@RequiredArgsConstructor
public class MeetingTodoApplyFacade {
    private final MeetingService meetingService;
    private final TaskService taskService;
    private final MeetingTodoRepository meetingTodoRepository;
}
```

### `applyTodosToTasks(Long meetingId): MeetingTaskApplyResponse`

```
1. meetingService.getMeeting(meetingId) → MEETING_NOT_FOUND
2. applied_task_id = NULL인 MeetingTodo 조회
3. 각 todo에 대해:
   a. TaskCreateRequest 생성 (title, assigneeId, projectId, dueDate → endDate)
   b. taskService.createTask(projectId, request) → createdTaskId
   c. meetingTodo.applyTask(createdTaskId) → applied_task_id 갱신
4. MeetingTaskApplyResponse { createdTaskIds } 반환
```

@Transactional

---

## 비즈니스 규칙

- `applied_task_id != NULL`인 todo는 이미 적용됨 → 재호출 시 해당 todo는 건너뜀
- `summary` 저장 시 `order_index` = 리스트 인덱스 그대로 사용 (0부터 시작)
- `manual = true`이면 notes는 NULL 가능, summary는 빈 배열 가능
- 회의록 수정/삭제 API는 현재 명세에 없음
