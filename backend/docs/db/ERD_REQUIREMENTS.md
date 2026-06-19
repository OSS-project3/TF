# TeamFlow ERD 명세서

> API 명세서(`docs/api/API_REQUIREMENTS.md`)의 필드명·타입과 일치하도록 작성.  
> DB 컬럼은 snake_case, Java 엔티티 필드는 camelCase로 매핑한다.

---

## 엔티티 목록

| 엔티티 | 테이블명 | 설명 |
|--------|----------|------|
| Member | `member` | 팀 멤버 (PM 포함) |
| MemberSkill | `member_skill` | 멤버가 보유한 스킬 |
| Project | `project` | 프로젝트 |
| ProjectMember | `project_member` | 프로젝트-멤버 N:N 중간 테이블 |
| Task | `task` | 작업 (할 일 단위) |
| TaskDependency | `task_dependency` | 작업 선행 관계 N:N 중간 테이블 |
| Meeting | `meeting` | 회의록 |
| MeetingAttendee | `meeting_attendee` | 회의 참석자 |
| MeetingSummaryItem | `meeting_summary_item` | AI 요약 항목 (순서 있음) |
| MeetingTodo | `meeting_todo` | 회의에서 나온 액션 아이템 |

---

## 관계 정의

| 관계 | 형태 | 연결 방식 |
|------|------|-----------|
| Member → MemberSkill | 1 : N | `member_skill.member_id` |
| Member ↔ Project | N : N | `project_member` 중간 테이블 |
| Project → Task | 1 : N | `task.project_id` |
| Member → Task | 1 : N | `task.assignee_id` (담당자 단일 지정) |
| Task ↔ Task | N : N | `task_dependency` 중간 테이블 (선행 관계) |
| Meeting → MeetingAttendee | 1 : N | `meeting_attendee.meeting_id` |
| Meeting → MeetingSummaryItem | 1 : N | `meeting_summary_item.meeting_id` |
| Meeting → MeetingTodo | 1 : N | `meeting_todo.meeting_id` |
| MeetingTodo → Task | 0..1 : 1 | `meeting_todo.applied_task_id` (NULL = 미적용) |

---

## 엔티티 상세

### Member

API 응답 키: `id`, `name`, `role`, `initial`, `weeklyCapacityHours`, `skills`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `name` | `VARCHAR(50)` | NOT NULL | 이름 (예: "김민서") |
| `role` | `VARCHAR(20)` | NOT NULL | `PM` \| `Frontend` \| `Backend` \| `Designer` \| `QA` |
| `initial` | `VARCHAR(5)` | NOT NULL | 아바타 이니셜 (예: "민") |
| `weekly_capacity_hours` | `INT` | NOT NULL | 주간 가용 시간 (예: 35) |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 생성일시 |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 수정일시 |

---

### MemberSkill

API 응답에서 `skills: string[]`로 노출. 멤버별 독립 소유, 스킬 공유 없음.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `member_id` | `BIGINT` | NOT NULL, FK → `member.id` | 소유 멤버 |
| `skill` | `VARCHAR(50)` | NOT NULL | 스킬명 (예: "React", "기획") |

---

### Project

API 응답 키: `id`, `name`, `goal`, `deadline`, `progress`, `memberIds`, `taskCount`, `doneTaskCount`, `lateTaskCount`, `health`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `name` | `VARCHAR(100)` | NOT NULL | 프로젝트명 |
| `goal` | `VARCHAR(255)` | NOT NULL | 목표 (예: "결제 성공률 12% 향상") |
| `deadline` | `DATE` | NOT NULL | 마감일 (`YYYY-MM-DD`) |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'ACTIVE' | `ACTIVE` \| `ARCHIVED` |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 생성일시 |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 수정일시 |

> `progress`, `taskCount`, `doneTaskCount`, `lateTaskCount`, `health` 는 Task 집계로 실시간 계산 — DB에 저장하지 않는다.

---

### ProjectMember

멤버-프로젝트 N:N 중간 테이블. API 응답에서 `memberIds: string[]`로 노출.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `project_id` | `BIGINT` | NOT NULL, FK → `project.id` | 프로젝트 |
| `member_id` | `BIGINT` | NOT NULL, FK → `member.id` | 멤버 |

> UNIQUE(`project_id`, `member_id`) — 동일 멤버 중복 등록 방지

---

### Task

API 응답 키: `id`, `projectId`, `title`, `phase`, `estimatedHours`, `difficulty`, `status`, `assigneeId`, `dependencyTaskIds`, `startDate`, `endDate`, `isCriticalPath`, `isLateRisk`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `project_id` | `BIGINT` | NOT NULL, FK → `project.id` | 소속 프로젝트 |
| `assignee_id` | `BIGINT` | NULL, FK → `member.id` | 담당자 (미배정 시 NULL) |
| `title` | `VARCHAR(200)` | NOT NULL | 작업 제목 |
| `phase` | `VARCHAR(20)` | NOT NULL | `리서치` \| `설계` \| `디자인` \| `개발` \| `QA` |
| `estimated_hours` | `INT` | NOT NULL | 예상 소요 시간 |
| `difficulty` | `VARCHAR(10)` | NOT NULL | `EASY` \| `MEDIUM` \| `HARD` |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'TODO' | `TODO` \| `IN_PROGRESS` \| `DONE` \| `BLOCKED` |
| `start_date` | `DATE` | NULL | 시작일 |
| `end_date` | `DATE` | NULL | 종료일 |
| `is_critical_path` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | 크리티컬 패스 여부 |
| `is_late_risk` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | 지연 위험 여부 |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 생성일시 |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 수정일시 |

---

### TaskDependency

Task 간 선행 관계 N:N 중간 테이블. API 응답에서 `dependencyTaskIds: string[]`로 노출.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `task_id` | `BIGINT` | NOT NULL, FK → `task.id` | 현재 작업 |
| `prerequisite_task_id` | `BIGINT` | NOT NULL, FK → `task.id` | 선행 작업 (이 작업이 완료돼야 `task_id`가 시작 가능) |

> UNIQUE(`task_id`, `prerequisite_task_id`) — 순환 의존 방지는 서비스 레이어에서 처리

---

### Meeting

API 응답 키: `id`, `title`, `date`, `attendeeMemberIds`, `notes`, `summary`, `todos`, `manual`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `title` | `VARCHAR(200)` | NOT NULL | 회의 제목 (예: "스프린트 체크인") |
| `date` | `DATE` | NOT NULL | 회의 날짜 (`YYYY-MM-DD`) |
| `notes` | `TEXT` | NULL | 회의 원문 |
| `is_manual` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | 수기 등록 여부 (`manual` 필드 매핑) |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 생성일시 |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | 수정일시 |

---

### MeetingAttendee

API 요청 키: `attendeeMemberIds: string[]`로 수신 → 행으로 분해 저장

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `meeting_id` | `BIGINT` | NOT NULL, FK → `meeting.id` | 회의 |
| `member_id` | `BIGINT` | NOT NULL, FK → `member.id` | 참석 멤버 |

> UNIQUE(`meeting_id`, `member_id`) — 동일 멤버 중복 등록 방지

---

### MeetingSummaryItem

API 응답에서 `summary: string[]`로 노출. AI 요약 결과의 순서 있는 항목.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `meeting_id` | `BIGINT` | NOT NULL, FK → `meeting.id` | 회의 |
| `order_index` | `INT` | NOT NULL | 항목 순서 (0부터 시작) |
| `content` | `TEXT` | NOT NULL | 요약 내용 |

---

### MeetingTodo

API 요청/응답 키: `assigneeId`, `projectId`, `title`, `dueDate`. 적용 여부는 `appliedTaskId`로 추적.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `meeting_id` | `BIGINT` | NOT NULL, FK → `meeting.id` | 회의 |
| `assignee_id` | `BIGINT` | NOT NULL, FK → `member.id` | 담당 멤버 |
| `project_id` | `BIGINT` | NOT NULL, FK → `project.id` | 대상 프로젝트 |
| `title` | `VARCHAR(200)` | NOT NULL | 할 일 내용 |
| `due_date` | `DATE` | NULL | 기한 (`YYYY-MM-DD`) |
| `applied_task_id` | `BIGINT` | NULL, FK → `task.id` | 프로젝트 작업으로 등록된 Task ID. NULL = 미적용 |

> `applied_task_id`가 NULL인 todo만 "프로젝트에 작업으로 추가" 버튼 활성화.  
> `POST /meetings/{meetingId}/tasks` 호출 시 Task 생성 후 `applied_task_id` 갱신.

---

## ERD 다이어그램

```
member
  ├─< member_skill          (1:N)
  ├─< project_member >─ project
  │                          └─< task
  │                               ├─< task_dependency >─ task (선행)
  │                               └─< meeting_todo.applied_task_id (0..1)
  └─< task (assignee_id)

meeting
  ├─< meeting_attendee >─ member
  ├─< meeting_summary_item
  └─< meeting_todo
        ├── assignee_id → member
        ├── project_id  → project
        └── applied_task_id → task (nullable)
```

---

## 타입 열거값 정리

| 필드 | 허용값 |
|------|--------|
| `member.role` | `PM`, `Frontend`, `Backend`, `Designer`, `QA` |
| `task.phase` | `리서치`, `설계`, `디자인`, `개발`, `QA` |
| `task.difficulty` | `EASY`, `MEDIUM`, `HARD` |
| `task.status` | `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED` |
| `project.status` | `ACTIVE`, `ARCHIVED` |

---

## Spring 엔티티 매핑 요약

| 테이블 | Spring 엔티티 클래스 |
|--------|----------------------|
| `member` | `Member` |
| `member_skill` | `MemberSkill` |
| `project` | `Project` |
| `project_member` | `ProjectMember` |
| `task` | `Task` |
| `task_dependency` | `TaskDependency` |
| `meeting` | `Meeting` |
| `meeting_attendee` | `MeetingAttendee` |
| `meeting_summary_item` | `MeetingSummaryItem` |
| `meeting_todo` | `MeetingTodo` |

```
com.teamflow
  member/domain  → Member, MemberSkill
  project/domain → Project, ProjectMember
  task/domain    → Task, TaskDependency
  meeting/domain → Meeting, MeetingAttendee, MeetingSummaryItem, MeetingTodo
```
