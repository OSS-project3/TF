# Meeting 도메인 — API 명세

공통 규칙(인증·응답 형식·에러 코드): [conventions.md](../common/conventions.md)

---

## 엔드포인트 목록

| Method | Path | 설명 | Phase |
|--------|------|------|-------|
| GET | `/meetings` | 회의록 목록 | 2 |
| GET | `/meetings/{meetingId}` | 회의록 상세 | 2 |
| POST | `/meetings` | 회의록 저장 | 2 |
| POST | `/meeting-summaries` | AI 회의록 요약 (저장 없음) | 3 |
| POST | `/meetings/{meetingId}/tasks` | TODO → 프로젝트 작업 등록 | 3 |

---

## 상세 명세

### GET `/meetings`

쿼리 파라미터: `?projectId=1` `?from=YYYY-MM-DD&to=YYYY-MM-DD`

```json
// Response
{
  "data": {
    "items": [
      {
        "id": "1",
        "title": "스프린트 체크인",
        "date": "2026-05-22",
        "attendeeMemberIds": ["1", "2", "3", "4"],
        "notes": "회의 원문...",
        "summary": ["결제 API 스펙은 거의 완성되었습니다."],
        "todos": [
          {
            "id": "1",
            "assigneeId": "2",
            "projectId": "1",
            "title": "결제 화면 컴포넌트 1차 완료",
            "dueDate": "2026-05-27",
            "appliedTaskId": null
          }
        ],
        "manual": false
      }
    ]
  }
}
```

---

### GET `/meetings/{meetingId}`

단건 조회. 응답 형식은 목록 아이템과 동일.

오류: `MEETING_NOT_FOUND` (404)

---

### POST `/meetings`

AI 요약 결과 저장(`manual: false`) 또는 수기 등록(`manual: true`) 모두 이 엔드포인트 사용.

```json
// Request
{
  "title": "스프린트 체크인",
  "date": "2026-05-22",
  "attendeeMemberIds": ["1", "2", "3", "4"],
  "notes": "회의 원문...",
  "summary": ["결제 API 스펙은 거의 완성되었습니다."],
  "todos": [
    {
      "assigneeId": "2",
      "projectId": "1",
      "title": "결제 화면 컴포넌트 1차 완료",
      "dueDate": "2026-05-27"
    }
  ],
  "manual": false
}

// Response
{ "data": { "id": "5" } }
```

오류: `MEMBER_NOT_FOUND` (404), `PROJECT_NOT_FOUND` (404)

---

### POST `/meeting-summaries`

AI 요약 생성만 수행 — DB 저장 없음. 결과를 확인 후 `/meetings` POST로 저장.

```json
// Request
{ "notes": "회의 원문...", "projectId": "1" }

// Response
{
  "data": {
    "summary": ["결제 화면 컴포넌트의 검증 로직 복잡도가 이슈입니다."],
    "todos": [
      {
        "assigneeName": "박지훈",
        "assigneeId": "2",
        "projectName": "모바일 결제 리뉴얼",
        "projectId": "1",
        "title": "결제 화면 컴포넌트 1차 완료",
        "dueDate": "2026-05-27"
      }
    ]
  }
}
```

초기 구현: 규칙 기반 파싱으로 대체 가능 (LLM 연동은 Phase 3).

---

### POST `/meetings/{meetingId}/tasks`

`applied_task_id = NULL`인 todo들을 일괄로 Task 생성 후 `applied_task_id` 갱신.

```json
// Response
{ "data": { "createdTaskIds": ["100", "101"] } }
```

이 엔드포인트는 MeetingService + TaskService + MeetingTodoService를 조합 → **MeetingTodoApplyFacade** 사용.  
자세한 흐름: [service-flow.md](./service-flow.md)

오류: `MEETING_NOT_FOUND` (404)

---

## DTO 클래스

| 클래스 | 용도 |
|--------|------|
| `MeetingResponse` | GET /meetings, GET /meetings/{id} 응답 |
| `MeetingTodoResponse` | MeetingResponse 내 todo 항목 |
| `MeetingCreateRequest` | POST /meetings 요청 |
| `MeetingTodoRequest` | MeetingCreateRequest 내 todo 항목 |
| `MeetingSummaryRequest` | POST /meeting-summaries 요청 |
| `MeetingSummaryResponse` | POST /meeting-summaries 응답 |
| `MeetingTaskApplyResponse` | POST /meetings/{id}/tasks 응답 |
