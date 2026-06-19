# Project 도메인 — API 명세

공통 규칙(인증·응답 형식·에러 코드): [conventions.md](../common/conventions.md)

---

## 엔드포인트 목록

| Method | Path | 설명 | 권한 | Phase |
|--------|------|------|------|-------|
| GET | `/projects` | 프로젝트 목록 | 전체 | 1 |
| POST | `/projects` | 프로젝트 생성 | PM | 1 |
| GET | `/projects/{projectId}` | 프로젝트 상세 | 전체 | 1 |
| PATCH | `/projects/{projectId}` | 프로젝트 수정 | PM | 2 |
| DELETE | `/projects/{projectId}` | 프로젝트 아카이브 | PM | 2 |
| GET | `/projects/{projectId}/members` | 프로젝트 멤버 목록 | 전체 | 1 |
| PUT | `/projects/{projectId}/members` | 멤버 전체 교체 | PM | 1 |
| POST | `/projects/{projectId}/members/{memberId}` | 멤버 단건 추가 | PM | 2 |
| DELETE | `/projects/{projectId}/members/{memberId}` | 멤버 단건 제거 | PM | 2 |

---

## 상세 명세

### GET `/projects`

쿼리 파라미터: `?memberId=2` `?health=WARN` `?status=ACTIVE`

- `memberId`: 해당 멤버가 참여한 프로젝트만 반환
- `health`: WARN·BAD·OK·IDLE 필터 (집계 후 필터링)
- `status`: 기본값 `ACTIVE` (ARCHIVED 포함 시 명시)

`progress`, `health`는 Task 집계 기반 실시간 계산.

```json
// Response item
{
  "id": "1",
  "name": "모바일 결제 리뉴얼",
  "goal": "결제 성공률 12% 향상",
  "deadline": "2026-07-18",
  "progress": 0.62,
  "memberIds": ["1", "2", "3", "4"],
  "taskCount": 24,
  "doneTaskCount": 15,
  "lateTaskCount": 1,
  "health": "OK"
}
```

---

### POST `/projects`

생성 후 → AI 작업 분해 `POST /ai/projects/{id}/decompositions` 로 이어짐.

```json
// Request
{
  "name": "모바일 결제 리뉴얼",
  "goal": "결제 성공률 12% 향상",
  "deadline": "2026-07-18",
  "memberIds": ["1", "2", "3"]
}

// Response
{ "data": { "id": "5" } }
```

오류: `MEMBER_NOT_FOUND` — memberIds에 존재하지 않는 멤버 포함 시

---

### GET `/projects/{projectId}`

프로젝트 상세 (멤버 목록 + 집계 포함).

```json
// Response
{
  "data": {
    "id": "1",
    "name": "모바일 결제 리뉴얼",
    "goal": "결제 성공률 12% 향상",
    "deadline": "2026-07-18",
    "status": "ACTIVE",
    "progress": 0.62,
    "memberIds": ["1", "2", "3", "4"],
    "taskCount": 24,
    "doneTaskCount": 15,
    "lateTaskCount": 1,
    "health": "OK"
  }
}
```

오류: `PROJECT_NOT_FOUND` (404)

---

### PATCH `/projects/{projectId}`

부분 수정. null 필드는 변경하지 않음.

```json
// Request
{
  "name": "모바일 결제 리뉴얼 v2",
  "goal": "결제 성공률 15% 향상",
  "deadline": "2026-08-01"
}
```

---

### DELETE `/projects/{projectId}`

실제 삭제가 아닌 아카이브 처리. `status = ARCHIVED`로 변경.

---

### GET `/projects/{projectId}/members`

ProjectDetail 화면에서 사용. 멤버 정보 포함.

```json
// Response
{
  "data": {
    "items": [
      {
        "id": "2",
        "name": "박지훈",
        "role": "FRONTEND",
        "initial": "지",
        "weeklyCapacityHours": 35,
        "skills": ["React", "UI"]
      }
    ]
  }
}
```

---

### PUT `/projects/{projectId}/members`

기존 멤버 전체를 새 목록으로 교체. CreateProjectModal에서 사용.

```json
// Request
{ "memberIds": ["1", "2", "3", "4"] }
```

---

### POST `/projects/{projectId}/members/{memberId}`

단일 멤버 추가. 이미 소속된 경우 `DUPLICATE_PROJECT_MEMBER` (409).

---

### DELETE `/projects/{projectId}/members/{memberId}`

단일 멤버 제거.

---

## DTO 클래스

| 클래스 | 용도 |
|--------|------|
| `ProjectCreateRequest` | POST /projects 요청 |
| `ProjectUpdateRequest` | PATCH /projects/{id} 요청 |
| `ProjectCreateResponse` | POST /projects 응답 (`{ id }`) |
| `ProjectResponse` | 목록/상세 응답 (집계 필드 포함) |
| `ProjectMemberReplaceRequest` | PUT /projects/{id}/members 요청 |
