# Dashboard 도메인 — API 명세

공통 규칙(인증·응답 형식·에러 코드): [conventions.md](../common/conventions.md)

> Dashboard 도메인은 별도 엔티티·테이블이 없다. Project, Task, Member 집계로 실시간 계산.

---

## 엔드포인트 목록

| Method | Path | 설명 | 권한 | Phase |
|--------|------|------|------|-------|
| GET | `/dashboard/pm` | PM 대시보드 통계 | PM | 2 |
| GET | `/dashboard/member` | 개인 대시보드 통계 | Member | 2 |

---

## 상세 명세

### GET `/dashboard/pm`

PM 전용. 전체 프로젝트 현황 요약.

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
    "projects": [
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
    ]
  }
}
```

필드 설명:
- `averageProgress`: ACTIVE 프로젝트 progress 평균
- `averageLoadRate`: 전체 멤버 loadRate 평균
- `projects`: ACTIVE 프로젝트 목록 (ProjectResponse 형식)

---

### GET `/dashboard/member`

JWT 토큰 기준 개인 대시보드. MemberDashboard 화면에서 사용.

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
    "projects": [
      {
        "id": "1",
        "name": "모바일 결제 리뉴얼",
        "progress": 0.62,
        "health": "OK"
      }
    ]
  }
}
```

필드 설명:
- `nextDueDate`: 미완료 Task 중 가장 가까운 endDate
- `todayTasks` / `thisWeekTasks` / `laterTasks`: `GET /me/tasks`와 동일한 그룹핑
- `projects`: 본인 참여 프로젝트 요약

---

## DTO 클래스

| 클래스 | 용도 |
|--------|------|
| `PmDashboardResponse` | GET /dashboard/pm 응답 |
| `MemberDashboardResponse` | GET /dashboard/member 응답 |
| `ProjectSummary` | MemberDashboardResponse 내 프로젝트 요약 |
