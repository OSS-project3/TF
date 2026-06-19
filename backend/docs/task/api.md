# Task 도메인 — API 명세

공통 규칙(인증·응답 형식·에러 코드): [conventions.md](../common/conventions.md)

---

## 엔드포인트 목록

| Method | Path | 설명 | Phase |
|--------|------|------|-------|
| GET | `/projects/{projectId}/tasks` | 프로젝트 작업 목록 | 1 |
| POST | `/projects/{projectId}/tasks` | 작업 수동 생성 | 1 |
| PATCH | `/tasks/{taskId}` | 작업 수정 | 2 |
| PATCH | `/tasks/{taskId}/status` | 상태 변경 | 1 |
| PATCH | `/tasks/{taskId}/assignee` | 담당자 변경 | 2 |
| GET | `/me/tasks` | 내 작업 목록 | 2 |

---

## 상세 명세

### GET `/projects/{projectId}/tasks`

쿼리 파라미터: `?assigneeId=2` `?status=TODO` `?phase=개발`

ProjectDetail, Schedule 화면에서 사용.

```json
// Response item
{
  "id": "10",
  "projectId": "1",
  "title": "결제 화면 컴포넌트",
  "phase": "개발",
  "estimatedHours": 18,
  "difficulty": "HARD",
  "status": "TODO",
  "assigneeId": "2",
  "dependencyTaskIds": ["7", "8"],
  "startDate": "2026-07-02",
  "endDate": "2026-07-06",
  "isCriticalPath": true,
  "isLateRisk": false
}
```

오류: `PROJECT_NOT_FOUND` (404)

---

### POST `/projects/{projectId}/tasks`

Schedule 일정 추가 모달 및 Meetings TODO 적용 시 호출.

```json
// Request
{
  "title": "오류 화면 디자인",
  "phase": "디자인",
  "estimatedHours": 8,
  "difficulty": "MEDIUM",
  "assigneeId": "4",
  "startDate": "2026-05-22",
  "endDate": "2026-05-29"
}

// Response
{ "data": { "id": "100" } }
```

오류: `PROJECT_NOT_FOUND` (404), `MEMBER_NOT_FOUND` (404)

---

### PATCH `/tasks/{taskId}`

작업 속성 부분 수정. null 필드는 변경하지 않음.

```json
// Request (모든 필드 선택적)
{
  "title": "결제 화면 컴포넌트 v2",
  "phase": "개발",
  "estimatedHours": 20,
  "difficulty": "HARD",
  "startDate": "2026-07-02",
  "endDate": "2026-07-08",
  "isCriticalPath": true,
  "isLateRisk": false
}
```

오류: `TASK_NOT_FOUND` (404)

---

### PATCH `/tasks/{taskId}/status`

ProjectDetail 체크박스 및 MemberDashboard TaskRow에서 호출.

```json
// Request
{ "status": "DONE" }

// Response
{ "data": { "message": "ok" } }
```

허용 값: `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED`  
오류: `TASK_NOT_FOUND` (404), `INVALID_TASK_STATUS_TRANSITION` (422)

---

### PATCH `/tasks/{taskId}/assignee`

```json
// Request
{ "assigneeId": "5" }

// Response
{ "data": { "message": "ok" } }
```

오류: `TASK_NOT_FOUND` (404), `MEMBER_NOT_FOUND` (404)

---

### GET `/me/tasks`

쿼리 파라미터: `?from=YYYY-MM-DD&to=YYYY-MM-DD` `?status=TODO`

MemberDashboard에서 사용. 오늘/이번 주/이후로 그룹핑.

```json
// Response
{
  "data": {
    "today": [
      {
        "id": "10",
        "projectId": "1",
        "title": "결제 화면 컴포넌트",
        "phase": "개발",
        "estimatedHours": 18,
        "difficulty": "HARD",
        "status": "TODO",
        "assigneeId": "2",
        "dependencyTaskIds": [],
        "startDate": "2026-07-02",
        "endDate": "2026-07-02",
        "isCriticalPath": true,
        "isLateRisk": false
      }
    ],
    "thisWeek": [],
    "later": []
  }
}
```

그룹핑 기준:
- `today`: `endDate = 오늘`
- `thisWeek`: `endDate = 이번 주 내 (오늘 제외)`
- `later`: 그 이후

---

## DTO 클래스

| 클래스 | 용도 |
|--------|------|
| `TaskResponse` | 작업 단건/목록 응답 |
| `TaskCreateRequest` | POST /projects/{id}/tasks 요청 |
| `TaskUpdateRequest` | PATCH /tasks/{id} 요청 |
| `TaskStatusUpdateRequest` | PATCH /tasks/{id}/status 요청 |
| `TaskAssigneeUpdateRequest` | PATCH /tasks/{id}/assignee 요청 |
| `MyTasksResponse` | GET /me/tasks 응답 (today/thisWeek/later 포함) |
| `ScheduleResponse` | GET /schedules 응답 아이템 |
| `ScheduleCreateRequest` | POST /schedules 요청 |
| `ScheduleUpdateRequest` | PATCH /schedules/{id} 요청 |

---

## Schedule 엔드포인트

> Schedule은 별도 엔티티 없음. `startDate/endDate`가 있는 Task를 ScheduleResponse로 변환.  
> Controller/Service는 `task` 패키지 내에 위치. `scheduleId = taskId`.

| Method | Path | 설명 | Phase |
|--------|------|------|-------|
| GET | `/schedules` | 일정 목록 | 2 |
| POST | `/schedules` | 수동 일정 추가 | 2 |
| PATCH | `/schedules/{scheduleId}` | 일정 수정 | 2 |

### GET `/schedules`

쿼리 파라미터: `?projectId=1` `?memberId=2` `?from=YYYY-MM-DD&to=YYYY-MM-DD`

`startDate/endDate` 없는 Task는 제외. 여러 프로젝트에 걸친 조회 가능.

```json
// Response item
{
  "id": "schedule-10",
  "taskId": "10",
  "projectId": "1",
  "projectName": "모바일 결제 리뉴얼",
  "title": "결제 화면 컴포넌트",
  "ownerId": "2",
  "phase": "개발",
  "startDate": "2026-07-02",
  "endDate": "2026-07-06",
  "status": "TODO",
  "kind": "CRITICAL_PATH"
}
```

`kind` 판정 (우선순위 순):
- `DONE`: `status = DONE`
- `CRITICAL_PATH`: `isCriticalPath = true AND status != DONE`
- `LATE_RISK`: `isLateRisk = true AND status != DONE`
- `NORMAL`: 그 외

### POST `/schedules`

내부적으로 Task 생성 후 ScheduleResponse 반환.

```json
// Request
{
  "projectId": "1",
  "title": "오류 화면 디자인",
  "ownerId": "4",
  "phase": "디자인",
  "startDate": "2026-05-22",
  "endDate": "2026-05-29"
}
// Response
{ "data": { "id": "schedule-100", "taskId": "100" } }
```

오류: `PROJECT_NOT_FOUND` (404), `MEMBER_NOT_FOUND` (404)

### PATCH `/schedules/{scheduleId}`

```json
// Request (모든 필드 선택적)
{
  "title": "오류 화면 디자인 v2",
  "ownerId": "4",
  "phase": "디자인",
  "startDate": "2026-05-23",
  "endDate": "2026-05-30"
}
```

오류: `TASK_NOT_FOUND` (404)

> `schedule-proposals` (AI 재최적화)는 Phase 3 — [ai/api.md](../ai/api.md) 참고.
