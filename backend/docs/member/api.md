# Member 도메인 — API 명세

공통 규칙(인증·응답 형식·에러 코드): [conventions.md](../common/conventions.md)

---

## 엔드포인트 목록

| Method | Path | 설명 | Phase |
|--------|------|------|-------|
| GET | `/me` | 현재 사용자 조회 | 1 |
| GET | `/members` | 팀 멤버 목록 | 1 |
| GET | `/members/{memberId}` | 멤버 상세 | 1 |
| GET | `/members/{memberId}/workload` | 멤버 업무량 조회 | 2 |
| GET | `/team/workloads` | 팀 전체 업무량 | 2 |

---

## 상세 명세

### GET `/me`

JWT 토큰 `sub`(memberId) 기준 인증된 사용자 조회.

```json
// Response
{
  "data": {
    "id": "1",
    "name": "김민서",
    "role": "PM",
    "initial": "민",
    "weeklyCapacityHours": 30,
    "skills": ["기획", "문서"]
  }
}
```

---

### GET `/members`

쿼리 파라미터: `?role=PM` (선택)

팀 전체 멤버 목록. 페이지네이션 없음 (팀 규모 소규모 가정).

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

### GET `/members/{memberId}`

멤버 단건 조회. 응답 형식은 `/me`와 동일.

오류: `MEMBER_NOT_FOUND` (404)

---

### GET `/members/{memberId}/workload`

쿼리 파라미터: `?from=YYYY-MM-DD&to=YYYY-MM-DD`

Task 집계로 실시간 계산. 기간 내 `assigneeId = memberId`인 Task 기준.

```json
// Response
{
  "data": {
    "memberId": "2",
    "capacityHours": 35,
    "assignedHours": 32,
    "loadRate": 0.92,
    "projectCount": 3,
    "taskCount": 14
  }
}
```

`loadRate = assignedHours / weeklyCapacityHours` (1.0 초과 가능 — 과부하 상태 표현)

오류: `MEMBER_NOT_FOUND` (404)

---

### GET `/team/workloads`

쿼리 파라미터: `?from=YYYY-MM-DD&to=YYYY-MM-DD` `?projectId=1`

팀 전체 멤버 업무량 요약. Team 화면 및 Dashboard AI callout에서 사용.

```json
// Response
{
  "data": {
    "items": [
      {
        "memberId": "2",
        "memberName": "박지훈",
        "role": "FRONTEND",
        "capacityHours": 35,
        "assignedHours": 32,
        "loadRate": 0.92,
        "taskCount": 14,
        "projectCount": 3,
        "skills": ["React", "UI"]
      }
    ]
  }
}
```

`projectId` 지정 시 해당 프로젝트 참여 멤버만 필터.

---

## DTO 클래스

| 클래스 | 용도 |
|--------|------|
| `MemberResponse` | `/me`, `/members`, `/members/{id}` 응답 |
| `WorkloadResponse` | `/members/{id}/workload` 응답 |
| `TeamWorkloadResponse` | `/team/workloads` 응답 아이템 |
