# Meeting 도메인 — ERD

의존 도메인: [member](../member/erd.md), [project](../project/erd.md), [task](../task/erd.md)

---

## 엔티티

### Meeting

테이블명: `meeting`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `title` | `VARCHAR(200)` | NOT NULL | 회의 제목 (예: "스프린트 체크인") |
| `date` | `DATE` | NOT NULL | 회의 날짜 (`YYYY-MM-DD`) |
| `notes` | `TEXT` | NULL | 회의 원문 (AI 요약 입력값) |
| `is_manual` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | 수기 등록 여부 |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |

---

### MeetingAttendee

테이블명: `meeting_attendee`

API 요청 `attendeeMemberIds: string[]` → 행으로 분해 저장.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `meeting_id` | `BIGINT` | NOT NULL, FK → `meeting.id` | |
| `member_id` | `BIGINT` | NOT NULL, FK → `member.id` | 참석 멤버 |

UNIQUE(`meeting_id`, `member_id`)

---

### MeetingSummaryItem

테이블명: `meeting_summary_item`

AI 요약 결과의 순서 있는 항목. API 응답에서 `summary: string[]`로 노출.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `meeting_id` | `BIGINT` | NOT NULL, FK → `meeting.id` | |
| `order_index` | `INT` | NOT NULL | 항목 순서 (0부터 시작) |
| `content` | `TEXT` | NOT NULL | 요약 내용 |

---

### MeetingTodo

테이블명: `meeting_todo`

회의에서 나온 액션 아이템. `appliedTaskId`로 Task 등록 여부 추적.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `meeting_id` | `BIGINT` | NOT NULL, FK → `meeting.id` | |
| `assignee_id` | `BIGINT` | NOT NULL, FK → `member.id` | 담당 멤버 |
| `project_id` | `BIGINT` | NOT NULL, FK → `project.id` | 대상 프로젝트 |
| `title` | `VARCHAR(200)` | NOT NULL | 할 일 내용 |
| `due_date` | `DATE` | NULL | 기한 (`YYYY-MM-DD`) |
| `applied_task_id` | `BIGINT` | NULL, FK → `task.id` | 등록된 Task ID. NULL = 미적용 |

> `applied_task_id = NULL`인 todo만 "프로젝트에 작업으로 추가" 버튼 활성화.

---

## 관계

```
meeting (1) ──< meeting_attendee (N) >── member
meeting (1) ──< meeting_summary_item (N)
meeting (1) ──< meeting_todo (N)
                  meeting_todo.assignee_id → member
                  meeting_todo.project_id  → project
                  meeting_todo.applied_task_id → task (nullable)
```

---

## Spring 엔티티 매핑

```
com.teamflow.meeting.domain
  ├── Meeting.java
  ├── MeetingAttendee.java
  ├── MeetingSummaryItem.java
  └── MeetingTodo.java

com.teamflow.meeting.repository
  ├── MeetingRepository.java
  ├── MeetingAttendeeRepository.java
  ├── MeetingSummaryItemRepository.java
  └── MeetingTodoRepository.java
```

`Meeting` ← `BaseTimeEntity` 상속  
나머지 3개는 BaseTimeEntity 상속 불필요 (Meeting의 createdAt으로 충분)
