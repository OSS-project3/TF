# TeamFlow API 명세서

> Base URL: `/api/v1`  
> 현재 프론트엔드 구현 기준으로 작성. 모든 데이터는 현재 `src/data` seed data + local state로 동작하며, 백엔드 연결 시 아래 API로 대체한다.

---

## 공통 규칙

**인증**

초기 개발: 요청 헤더에 `X-Member-Id`로 현재 사용자 식별  
운영: Spring Security + JWT

**권한**

| 역할 | 가능한 작업 |
|------|------------|
| PM (`m1`) | 프로젝트 생성/수정/삭제, 멤버 초대/배정, 팀 전체 조회, AI 자동 배정 |
| Member | 본인 참여 프로젝트/작업/일정 조회, 본인 작업 완료 처리 |

**응답 형식**

```json
// 단건
{ "data": { ... }, "message": "ok" }

// 목록
{ "data": { "items": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 } }

// 오류
{ "error": { "code": "PROJECT_NOT_FOUND", "message": "Project not found" } }
```

---

## 엔드포인트 목록

| # | Method | Path | 설명 | 사용 화면 |
|---|--------|------|------|-----------|
| 1 | GET | `/me` | 현재 사용자 조회 | Sidebar 역할 분기 |
| 2 | GET | `/members` | 팀 멤버 목록 | Team, CreateProjectModal, Schedule, Meetings |
| 3 | GET | `/members/{memberId}` | 멤버 상세 | - |
| 4 | GET | `/members/{memberId}/workload` | 멤버 업무량 | Team |
| 5 | GET | `/projects` | 프로젝트 목록 | Dashboard, MemberDashboard, Projects, Schedule |
| 6 | POST | `/projects` | 프로젝트 생성 | CreateProjectModal |
| 7 | GET | `/projects/{projectId}` | 프로젝트 상세 | ProjectDetail |
| 8 | PATCH | `/projects/{projectId}` | 프로젝트 수정 | - |
| 9 | DELETE | `/projects/{projectId}` | 프로젝트 삭제(archive) | - |
| 10 | GET | `/projects/{projectId}/members` | 프로젝트 멤버 조회 | ProjectDetail |
| 11 | PUT | `/projects/{projectId}/members` | 멤버 전체 교체 | CreateProjectModal |
| 12 | POST | `/projects/{projectId}/members/{memberId}` | 멤버 추가 | - |
| 13 | DELETE | `/projects/{projectId}/members/{memberId}` | 멤버 제거 | - |
| 14 | GET | `/projects/{projectId}/tasks` | 프로젝트 작업 목록 | ProjectDetail, Schedule |
| 15 | POST | `/projects/{projectId}/tasks` | 작업 수동 생성 | Schedule 일정 추가, Meetings TODO 적용 |
| 16 | PATCH | `/tasks/{taskId}` | 작업 수정 | - |
| 17 | PATCH | `/tasks/{taskId}/status` | 작업 완료 처리 | ProjectDetail 체크박스, MemberDashboard |
| 18 | PATCH | `/tasks/{taskId}/assignee` | 담당자 변경 | - |
| 19 | GET | `/me/tasks` | 내 작업 목록 (today/thisWeek/later) | MemberDashboard |
| 20 | GET | `/dashboard/pm` | PM 대시보드 통계 | Dashboard |
| 21 | GET | `/dashboard/member` | 개인 대시보드 통계 | MemberDashboard |
| 22 | GET | `/team/workloads` | 팀 멤버별 업무량 | Team, Dashboard AI callout |
| 23 | GET | `/schedules` | 일정 목록 | Schedule 간트/캘린더/리스트 |
| 24 | POST | `/schedules` | 수동 일정 추가 | Schedule + 일정 추가 모달 |
| 25 | PATCH | `/schedules/{scheduleId}` | 일정 수정 | - |
| 26 | POST | `/schedule-proposals` | 일정 재최적화 제안 | Schedule ✸ 재최적화 버튼 |
| 27 | PATCH | `/schedule-proposals/{proposalId}` | 제안 적용/거절 | - |
| 28 | GET | `/meetings` | 회의록 목록 | Meetings 기록 보기 |
| 29 | GET | `/meetings/{meetingId}` | 회의록 상세 | - |
| 30 | POST | `/meetings` | 회의록 저장 | Meetings 수기 등록 / AI 요약 결과 저장 |
| 31 | POST | `/meeting-summaries` | AI 회의록 요약 | Meetings ✸ AI로 요약하기 |
| 32 | POST | `/meetings/{meetingId}/tasks` | TODO → 프로젝트 작업 등록 | Meetings ✸ 프로젝트에 작업으로 추가 |
| 33 | GET | `/ai/messages` | AI Thread 메시지 조회 | AIThread (모든 화면) |
| 34 | POST | `/ai/projects/{projectId}/decompositions` | AI 작업 분해 | ProjectDetail decomposing 단계 |
| 35 | POST | `/ai/projects/{projectId}/assignments` | AI 자동 배정 | ProjectDetail ✸ AI 재배정 버튼 |
| 36 | POST | `/ai/goal-suggestions` | 목표 개선 제안 | CreateProjectModal goalSuggestion |

---

## 도메인 타입

### Member

```ts
// 프론트 타입 (src/types/index.ts)
interface Member {
  id: string;       // "m1"
  name: string;     // "김민서"
  role: string;     // "PM" | "Frontend" | "Backend" | "Designer" | "QA"
  init: string;     // "민" — 아바타 이니셜
  hours: number;    // 주간 가용 시간
  skills: string[]; // ["기획", "문서"]
}
```

```
Spring 엔티티: Member, Skill, MemberSkill
추가 필드: createdAt, updatedAt
```

### Project

```ts
interface Project {
  id: string;
  name: string;
  goal: string;
  deadline: string;              // "YYYY-MM-DD"
  progress: number;              // 0.0 ~ 1.0
  members: string[];             // memberId[]
  tasks: number;                 // 전체 작업 수
  done: number;                  // 완료 작업 수
  late: number;                  // 지연 작업 수
  health: 'ok' | 'warn' | 'bad' | 'idle';
}
```

```
Spring 엔티티: Project, ProjectMember
tasks/done/late/progress/health → Task 집계로 계산 권장
```

### Task

```ts
interface Task {
  id: string;
  phase: string;                         // "리서치" | "설계" | "디자인" | "개발" | "QA"
  title: string;
  hours: number;                         // 예상 소요 시간
  difficulty: 'easy' | 'medium' | 'hard';
  deps: string[];                        // 선행 taskId[]
}
```

```
Spring 추가 필드:
  projectId, assigneeId, status (TODO/IN_PROGRESS/DONE/BLOCKED)
  startDate, endDate, isCriticalPath, isLateRisk
  createdAt, updatedAt
```

### MeetingRecord

```ts
interface MeetingRecord {
  id: string;
  title: string;
  date: string;
  attendees: string[];          // 멤버 이름 (현재 구현) → memberId로 변경 권장
  notes: string;
  summary: string[];
  todos: { who: string; what: string; due: string; proj: string }[];
  manual?: boolean;
}
```

```
Spring 엔티티: Meeting, MeetingAttendee, MeetingSummaryItem, MeetingTodo
attendees: 이름 대신 memberId 사용 권장
```

### AIMessage

```ts
interface AIMessage {
  tag: string;  // "INSIGHT" | "NUDGE" | "WARN" | "TIP" | "AUTO" | "SUMMARY" | "PLAN" | "BALANCE" | "READY" | "FOCUS" | "INFO"
  t: string;    // 시간 "14:02"
  body: string;
}
```

---

## 상세 명세

### 1. GET `/me`

현재 사용자 조회

```json
// Response
{
  "data": {
    "id": "m1",
    "name": "김민서",
    "role": "PM",
    "initial": "민",
    "weeklyCapacityHours": 30,
    "skills": ["기획", "문서"]
  }
}
```

---

### 2. GET `/members`

쿼리: `?role=PM` (선택)

```json
// Response item
{
  "id": "m2",
  "name": "박지훈",
  "role": "Frontend",
  "initial": "지",
  "weeklyCapacityHours": 35,
  "skills": ["React", "UI"]
}
```

---

### 4. GET `/members/{memberId}/workload`

쿼리: `?from=YYYY-MM-DD&to=YYYY-MM-DD`

```json
// Response
{
  "data": {
    "memberId": "m2",
    "capacityHours": 35,
    "assignedHours": 32,
    "loadRate": 0.92,
    "projectCount": 3,
    "taskCount": 14
  }
}
```

---

### 5. GET `/projects`

쿼리: `?memberId=m2` `?health=WARN` `?status=ACTIVE`

```json
// Response item
{
  "id": "p1",
  "name": "모바일 결제 리뉴얼",
  "goal": "결제 성공률 12% 향상",
  "deadline": "2026-07-18",
  "progress": 0.62,
  "memberIds": ["m1", "m2", "m3", "m4"],
  "taskCount": 24,
  "doneTaskCount": 15,
  "lateTaskCount": 1,
  "health": "OK"
}
```

---

### 6. POST `/projects`

CreateProjectModal에서 호출. 생성 후 ProjectDetail로 이동하며 AI 작업 분해(#34)와 연결된다.

```json
// Request
{
  "name": "모바일 결제 리뉴얼",
  "goal": "결제 성공률 12% 향상",
  "deadline": "2026-07-18",
  "memberIds": ["m1", "m2", "m3"]
}

// Response
{ "data": { "id": "p5" } }
```

---

### 14. GET `/projects/{projectId}/tasks`

쿼리: `?assigneeId=m2` `?status=TODO` `?phase=개발`

```json
// Response item
{
  "id": "t10",
  "projectId": "p1",
  "title": "결제 화면 컴포넌트",
  "phase": "개발",
  "estimatedHours": 18,
  "difficulty": "HARD",
  "status": "TODO",
  "assigneeId": "m2",
  "dependencyTaskIds": ["t7", "t8"],
  "startDate": "2026-07-02",
  "endDate": "2026-07-06",
  "isCriticalPath": true,
  "isLateRisk": false
}
```

---

### 15. POST `/projects/{projectId}/tasks`

Schedule 일정 추가 모달 / Meetings TODO 적용 시 호출

```json
// Request
{
  "title": "오류 화면 디자인",
  "phase": "디자인",
  "estimatedHours": 8,
  "difficulty": "MEDIUM",
  "assigneeId": "m4",
  "startDate": "2026-05-22",
  "endDate": "2026-05-29"
}
```

---

### 17. PATCH `/tasks/{taskId}/status`

ProjectDetail 체크박스, MemberDashboard TaskRow에서 호출

```json
// Request
{ "status": "DONE" }
```

---

### 18. PATCH `/tasks/{taskId}/assignee`

```json
// Request
{ "assigneeId": "m5" }
```

---

### 19. GET `/me/tasks`

쿼리: `?from=YYYY-MM-DD&to=YYYY-MM-DD` `?status=TODO`

```json
// Response
{
  "data": {
    "today": [ /* Task[] */ ],
    "thisWeek": [ /* Task[] */ ],
    "later": [ /* Task[] */ ]
  }
}
```

---

### 20. GET `/dashboard/pm`

```json
// Response
{
  "data": {
    "activeProjectCount": 4,
    "averageProgress": 0.49,
    "totalTaskCount": 62,
    "doneTaskCount": 31,
    "lateTaskCount": 3,
    "memberCount": 5,
    "averageLoadRate": 0.64,
    "projects": [ /* Project[] */ ]
  }
}
```

---

### 21. GET `/dashboard/member`

`X-Member-Id` 헤더 기준으로 응답

```json
// Response
{
  "data": {
    "loadRate": 0.92,
    "assignedHours": 32,
    "capacityHours": 35,
    "projectCount": 3,
    "taskCount": 11,
    "doneTaskCount": 5,
    "nextDueDate": "2026-05-27",
    "todayTasks": [],
    "thisWeekTasks": [],
    "laterTasks": [],
    "projects": []
  }
}
```

---

### 22. GET `/team/workloads`

쿼리: `?from=YYYY-MM-DD&to=YYYY-MM-DD` `?projectId=p1`

```json
// Response item
{
  "memberId": "m2",
  "memberName": "박지훈",
  "role": "Frontend",
  "capacityHours": 35,
  "assignedHours": 32,
  "loadRate": 0.92,
  "taskCount": 14,
  "projectCount": 3,
  "skills": ["React", "UI"]
}
```

---

### 23. GET `/schedules`

쿼리: `?projectId=p1` (필수에 가까움) `?memberId=m2` `?from=YYYY-MM-DD&to=YYYY-MM-DD`

```json
// Response item
{
  "id": "schedule-1",
  "taskId": "t10",
  "projectId": "p1",
  "projectName": "모바일 결제 리뉴얼",
  "title": "결제 화면 컴포넌트",
  "ownerId": "m2",
  "phase": "개발",
  "startDate": "2026-07-02",
  "endDate": "2026-07-06",
  "status": "TODO",
  "kind": "CRITICAL_PATH"
}
```

`kind`: `NORMAL` | `CRITICAL_PATH` | `LATE_RISK` | `DONE`

---

### 24. POST `/schedules`

Schedule 일정 추가 모달. 내부에서 Task 생성 또는 기존 Task에 연결.

```json
// Request
{
  "projectId": "p1",
  "title": "오류 화면 디자인",
  "ownerId": "m4",
  "phase": "디자인",
  "startDate": "2026-05-22",
  "endDate": "2026-05-29"
}
```

---

### 26. POST `/schedule-proposals`

Schedule ✸ 재최적화 버튼

```json
// Request
{ "projectId": "p1", "strategy": "BALANCE_WORKLOAD" }

// Response
{
  "data": {
    "id": "proposal-1",
    "summary": "과부하 멤버의 작업 2건을 QA 멤버에게 이동하는 안입니다.",
    "changes": [
      {
        "taskId": "t11",
        "fromAssigneeId": "m2",
        "toAssigneeId": "m5",
        "reason": "m2의 주간 부하가 90%를 초과했습니다."
      }
    ]
  }
}
```

---

### 27. PATCH `/schedule-proposals/{proposalId}`

재최적화 제안 수락/거절 시 호출

```json
// Request
{ "status": "APPLIED" }  // or "REJECTED"

// Response
{ "data": { "message": "ok" } }
```

---

### 28. GET `/meetings`

쿼리: `?projectId=p1` `?from=YYYY-MM-DD&to=YYYY-MM-DD`

---

### 30. POST `/meetings`

AI 요약 결과 저장 (`manual: false`) 또는 수기 등록 (`manual: true`)

```json
// Request
{
  "title": "스프린트 체크인",
  "date": "2026-05-22",
  "attendeeMemberIds": ["m1", "m2", "m3", "m4"],
  "notes": "회의 원문...",
  "summary": ["결제 API 스펙은 거의 완성되었습니다."],
  "todos": [
    {
      "assigneeId": "m2",
      "projectId": "p1",
      "title": "결제 화면 컴포넌트 1차 완료",
      "dueDate": "2026-05-27"
    }
  ],
  "manual": false
}
```

---

### 31. POST `/meeting-summaries`

Meetings 화면에서 ✸ AI로 요약하기 버튼 클릭 시 호출

```json
// Request
{ "notes": "회의 원문...", "projectId": "p1" }

// Response
{
  "data": {
    "summary": ["결제 화면 컴포넌트의 검증 로직 복잡도가 이슈입니다."],
    "todos": [
      {
        "assigneeName": "박지훈",
        "assigneeId": "m2",
        "projectName": "모바일 결제 리뉴얼",
        "projectId": "p1",
        "title": "결제 화면 컴포넌트 1차 완료",
        "dueDate": "2026-05-27"
      }
    ]
  }
}
```

---

### 32. POST `/meetings/{meetingId}/tasks`

Meetings 화면에서 ✸ 프로젝트에 작업으로 추가 버튼 클릭 시 호출

```json
// Response
{ "data": { "createdTaskIds": ["t100", "t101"] } }
```

---

### 33. GET `/ai/messages`

AIThread 컴포넌트가 화면 전환 시마다 호출

쿼리: `?scope=dashboard` `?role=pm` `?projectId=p1`

`scope` 값: `dashboard` | `projects` | `detail` | `team` | `meetings` | `schedule`

```json
// Response item
{
  "id": "ai-1",
  "tag": "INSIGHT",
  "time": "14:02",
  "body": "관리자 대시보드 V2의 진행률이 지난주 대비 8% 낮습니다."
}
```

---

### 34. POST `/ai/projects/{projectId}/decompositions`

CreateProjectModal 생성 직후 → ProjectDetail `decomposing` 단계에서 호출

```json
// Request
{
  "goal": "결제 성공률 12% 향상",
  "deadline": "2026-07-18",
  "memberIds": ["m1", "m2", "m3", "m4"]
}

// Response
{
  "data": {
    "reasoningMessages": [
      "목표를 분석했습니다.",
      "5개 단계로 작업을 나눴습니다."
    ],
    "tasks": [ /* Task[] */ ]
  }
}
```

초기 구현은 규칙 기반 템플릿으로 응답해도 된다.

---

### 35. POST `/ai/projects/{projectId}/assignments`

ProjectDetail ✸ AI 재배정 버튼 클릭 시 호출

```json
// Request
{ "strategy": "ROLE_AND_LOAD_BALANCE" }

// Response
{
  "data": {
    "assignments": [
      {
        "taskId": "t10",
        "assigneeId": "m2",
        "reason": "Frontend 역할과 현재 부하를 고려했습니다."
      }
    ],
    "workloads": [ /* workload 요약 */ ]
  }
}
```

---

### 36. POST `/ai/goal-suggestions`

CreateProjectModal에서 목표 입력 후 제안 버튼 클릭 시 호출

```json
// Request
{ "goal": "사용자 경험을 개선한다" }

// Response
{
  "data": {
    "suggestion": "신규 사용자 7일 리텐션을 5%p 향상시키는 목표로 바꾸면 측정 가능합니다."
  }
}
```

---

## 구현 우선순위

### Phase 1 — seed data 제거 최소 API

1. `GET /me`
2. `GET /members`
3. `GET /projects`
4. `POST /projects`
5. `GET /projects/{projectId}`
6. `GET /projects/{projectId}/tasks`
7. `PATCH /tasks/{taskId}/status`

### Phase 2 — 화면별 기능 완성

1. `GET /dashboard/pm`
2. `GET /dashboard/member`
3. `GET /me/tasks`
4. `GET /team/workloads`
5. `GET /schedules`
6. `POST /schedules`
7. `GET /meetings`
8. `POST /meetings`

### Phase 3 — AI / 자동화

1. `GET /ai/messages`
2. `POST /ai/projects/{projectId}/decompositions`
3. `POST /ai/projects/{projectId}/assignments`
4. `POST /meeting-summaries`
5. `POST /meetings/{meetingId}/tasks`
6. `POST /schedule-proposals`
7. `PATCH /schedule-proposals/{proposalId}`
8. `POST /ai/goal-suggestions`

---

## 프론트 연결 시 변경 포인트

| 파일 | 변경 내용 |
|------|----------|
| `src/data/index.ts` | seed data → API fetch 또는 React Query 훅으로 대체 |
| `App.tsx` | `projects`, `currentUserId` state → 서버 데이터 기반으로 변경 |
| `Projects/CreateProjectModal.tsx` | `onCreate` → `POST /projects` 호출 |
| `ProjectDetail/ProjectDetail.tsx` | seed task/assignment → task API + AI API로 분리 |
| `MemberDashboard/MemberDashboard.tsx` | 역할 기반 가짜 task 필터 → `GET /me/tasks` 로 대체 |
| `Schedule/Schedule.tsx` | `customTasks` local state → `POST /schedules` 로 저장 |
| `Meetings/Meetings.tsx` | local meeting records → `GET/POST /meetings` 로 변경 |
| `AIThread/AIThread.tsx` | 하드코딩 메시지 → `GET /ai/messages?scope=...` 로 조회 |

---

## 설계 결정사항

- 도메인 용어 `Member`로 통일 (`User` 사용 금지)
- `progress` / `health` — DB 컬럼 없음, 조회 시 Task 집계 실시간 계산
- Schedule 별도 엔티티 없음 — Task의 `startDate/endDate`로 표현
- 인증 JWT로 설정

## 설계 미결정사항

- AI 결과를 매번 생성할지, 생성 후 저장해서 재사용할지 (Phase 3 시점에 결정)
