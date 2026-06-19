# TeamFlow 공통 규칙

모든 도메인 문서에서 공유하는 규칙. 개별 도메인 문서에 중복 기재하지 않는다.

---

## Base URL

`/api/v1`

---

## 인증

| 경로 패턴 | 인증 필요 |
|-----------|----------|
| `/api/v1/auth/**` | 불필요 |
| 그 외 전체 | `Authorization: Bearer {token}` 필수 |

JWT payload: `{ "sub": memberId, "role": "PM" }`

`JwtAuthFilter` → 토큰 파싱 → `SecurityContext` 저장

---

## 응답 형식

### 단건 성공

```json
{ "data": { ... }, "message": "ok" }
```

### 목록 성공 (페이지네이션)

```json
{
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

### 오류

```json
{ "error": { "code": "PROJECT_NOT_FOUND", "message": "..." } }
```

---

## 권한

| 역할 | 허용 작업 |
|------|----------|
| `PM` | 프로젝트 생성/수정/삭제, 멤버 초대/배정, 팀 전체 조회, AI 자동 배정 |
| `Member` | 본인 참여 프로젝트/작업/일정 조회, 본인 작업 완료 처리 |

---

## 공통 Enum

| Enum | 값 |
|------|----|
| `MemberRole` | `PM`, `FRONTEND`, `BACKEND`, `DESIGNER`, `QA` |
| `TaskStatus` | `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED` |
| `TaskDifficulty` | `EASY`, `MEDIUM`, `HARD` |
| `TaskPhase` | `리서치`, `설계`, `디자인`, `개발`, `QA` |
| `ProjectStatus` | `ACTIVE`, `ARCHIVED` |
| `ProjectHealth` | `OK`, `WARN`, `BAD`, `IDLE` |
| `ScheduleKind` | `NORMAL`, `CRITICAL_PATH`, `LATE_RISK`, `DONE` |
| `AiMessageTag` | `INSIGHT`, `NUDGE`, `WARN`, `TIP`, `AUTO`, `SUMMARY`, `PLAN`, `BALANCE`, `READY`, `FOCUS`, `INFO` |

---

## 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| `MEMBER_NOT_FOUND` | 404 | 멤버가 존재하지 않음 |
| `PROJECT_NOT_FOUND` | 404 | 프로젝트가 존재하지 않음 |
| `TASK_NOT_FOUND` | 404 | 작업이 존재하지 않음 |
| `MEETING_NOT_FOUND` | 404 | 회의록이 존재하지 않음 |
| `PROPOSAL_NOT_FOUND` | 404 | 일정 재최적화 제안이 존재하지 않음 |
| `UNAUTHORIZED` | 401 | 인증 토큰 없음 또는 만료 |
| `FORBIDDEN` | 403 | PM 전용 기능을 일반 멤버가 호출 |
| `DUPLICATE_PROJECT_MEMBER` | 409 | 이미 프로젝트에 소속된 멤버 |
| `CIRCULAR_TASK_DEPENDENCY` | 422 | 순환 선행 관계 감지 |
| `INVALID_TASK_STATUS_TRANSITION` | 422 | 허용되지 않는 상태 전이 |

---

## BaseTimeEntity

모든 Entity가 상속. `createdAt`, `updatedAt` 자동 관리.

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

## 구현 우선순위

| Phase | 범위 |
|-------|------|
| Phase 1 | `GET /me`, `GET /members`, `GET /projects`, `POST /projects`, `GET /projects/{id}`, `GET /projects/{id}/tasks`, `PATCH /tasks/{id}/status` |
| Phase 2 | dashboard, `/me/tasks`, `/team/workloads`, schedules, meetings CRUD |
| Phase 3 | AI 기능 전체 (`/ai/**`, `/meeting-summaries`, `/schedule-proposals`) |
