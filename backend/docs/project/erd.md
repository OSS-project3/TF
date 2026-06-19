# Project 도메인 — ERD

의존 도메인: [member](../member/erd.md)

---

## 엔티티

### Project

테이블명: `project`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `name` | `VARCHAR(100)` | NOT NULL | 프로젝트명 |
| `goal` | `VARCHAR(255)` | NOT NULL | 목표 (예: "결제 성공률 12% 향상") |
| `deadline` | `DATE` | NOT NULL | 마감일 (`YYYY-MM-DD`) |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'ACTIVE'` | `ACTIVE` \| `ARCHIVED` |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |

> `progress`, `taskCount`, `doneTaskCount`, `lateTaskCount`, `health` — **DB 저장 안 함**.  
> 조회 시 Task 집계로 실시간 계산 → DTO에만 포함.

---

### ProjectMember

테이블명: `project_member`

N:N 중간 테이블. API 응답에서 `memberIds: string[]`로 노출.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `project_id` | `BIGINT` | NOT NULL, FK → `project.id` | |
| `member_id` | `BIGINT` | NOT NULL, FK → `member.id` | |

UNIQUE(`project_id`, `member_id`) — 중복 등록 방지

---

## 관계

```
project (1) ──< project_member (N) >── member (1)
                project_member.member_id → member.id
```

---

## 집계 필드 계산 규칙

`ProjectResponse`에 포함되는 집계 필드 (Task 도메인 데이터 기반):

| 필드 | 계산 방식 |
|------|-----------|
| `taskCount` | 프로젝트 전체 Task 수 |
| `doneTaskCount` | `status = DONE` Task 수 |
| `lateTaskCount` | `endDate < today AND status != DONE` Task 수 |
| `progress` | `doneTaskCount / taskCount` (0.0 ~ 1.0, taskCount=0이면 0.0) |
| `health` | 아래 규칙 참조 |

**health 판정 기준** (우선순위 순)

| 조건 | health |
|------|--------|
| `taskCount = 0` | `IDLE` |
| `lateTaskCount > 0 AND progress < 0.3` | `BAD` |
| `lateTaskCount > 0 OR progress < 0.5` | `WARN` |
| 그 외 | `OK` |

---

## Spring 엔티티 매핑

```
com.teamflow.project.domain
  ├── Project.java        ← BaseTimeEntity 상속
  └── ProjectMember.java  ← BaseTimeEntity 상속 불필요

com.teamflow.project.repository
  ├── ProjectRepository.java
  └── ProjectMemberRepository.java
```
