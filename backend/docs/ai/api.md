# AI 도메인 — API 명세

공통 규칙(인증·응답 형식·에러 코드): [conventions.md](../common/conventions.md)

> AI 도메인은 별도 DB 테이블 없음.  
> Phase 3 전체. 초기 구현은 규칙 기반으로 대체 가능.

---

## 엔드포인트 목록

| Method | Path | 설명 | Phase |
|--------|------|------|-------|
| GET | `/ai/messages` | AI Thread 메시지 조회 | 3 |
| POST | `/ai/projects/{projectId}/decompositions` | AI 작업 분해 | 3 |
| POST | `/ai/projects/{projectId}/assignments` | AI 자동 배정 | 3 |
| POST | `/ai/goal-suggestions` | 목표 개선 제안 | 3 |

---

## 상세 명세

### GET `/ai/messages`

AIThread 컴포넌트가 화면 전환 시마다 호출.

쿼리 파라미터: `?scope=dashboard` `?role=pm` `?projectId=1`

`scope` 허용값: `dashboard` | `projects` | `detail` | `team` | `meetings` | `schedule`

```json
// Response
{
  "data": {
    "items": [
      {
        "id": "ai-1",
        "tag": "INSIGHT",
        "time": "14:02",
        "body": "관리자 대시보드 V2의 진행률이 지난주 대비 8% 낮습니다."
      }
    ]
  }
}
```

`tag` 허용값: `INSIGHT` | `NUDGE` | `WARN` | `TIP` | `AUTO` | `SUMMARY` | `PLAN` | `BALANCE` | `READY` | `FOCUS` | `INFO`

초기 구현: scope/role/projectId 기반 정적 메시지 반환으로 대체 가능.

---

### POST `/ai/projects/{projectId}/decompositions`

CreateProjectModal 생성 직후 → ProjectDetail `decomposing` 단계에서 호출.

```json
// Request
{
  "goal": "결제 성공률 12% 향상",
  "deadline": "2026-07-18",
  "memberIds": ["1", "2", "3", "4"]
}

// Response
{
  "data": {
    "reasoningMessages": [
      "목표를 분석했습니다.",
      "5개 단계로 작업을 나눴습니다."
    ],
    "tasks": [
      {
        "title": "결제 플로우 리서치",
        "phase": "리서치",
        "estimatedHours": 8,
        "difficulty": "EASY",
        "dependencyTaskIds": []
      }
    ]
  }
}
```

반환된 tasks는 DB에 저장되지 않음 — 프론트에서 확인 후 `POST /projects/{id}/tasks`로 개별 저장.  
초기 구현: 역할/마감일 기반 고정 템플릿 반환으로 대체 가능.

오류: `PROJECT_NOT_FOUND` (404)

---

### POST `/ai/projects/{projectId}/assignments`

ProjectDetail AI 재배정 버튼. 멤버 역할과 부하 기준으로 최적 배정 제안.

```json
// Request
{ "strategy": "ROLE_AND_LOAD_BALANCE" }

// Response
{
  "data": {
    "assignments": [
      {
        "taskId": "10",
        "assigneeId": "2",
        "reason": "Frontend 역할과 현재 부하를 고려했습니다."
      }
    ],
    "workloads": [
      {
        "memberId": "2",
        "assignedHours": 32,
        "loadRate": 0.92
      }
    ]
  }
}
```

반환된 assignments는 즉시 적용하지 않음 — 프론트에서 확인 후 `PATCH /tasks/{id}/assignee`로 적용.

오류: `PROJECT_NOT_FOUND` (404)

---

### POST `/ai/goal-suggestions`

CreateProjectModal에서 목표 입력 후 개선 제안 버튼 클릭 시 호출.

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

## DTO 클래스

| 클래스 | 용도 |
|--------|------|
| `AiMessageResponse` | GET /ai/messages 응답 아이템 |
| `DecomposeRequest` | POST /ai/projects/{id}/decompositions 요청 |
| `DecomposeResponse` | POST /ai/projects/{id}/decompositions 응답 |
| `AssignRequest` | POST /ai/projects/{id}/assignments 요청 |
| `AssignResponse` | POST /ai/projects/{id}/assignments 응답 |
| `GoalSuggestionRequest` | POST /ai/goal-suggestions 요청 |
| `GoalSuggestionResponse` | POST /ai/goal-suggestions 응답 |
