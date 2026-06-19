# Dashboard 도메인 — Service 흐름

## 담당 Service

`com.teamflow.dashboard.service.DashboardService`

> 별도 엔티티·Repository 없음. 타 도메인 Service를 조합하여 집계 반환.

---

## 의존 도메인

| 도메인 | 메서드 | 이유 |
|--------|--------|------|
| `ProjectService` | `getProjects(...)` | 프로젝트 목록 및 집계 |
| `TaskService` | `getAggregation(projectId)`, `getMyTasks(...)` | Task 집계, 내 작업 그룹핑 |
| `MemberService` | `getMembers()`, `getWorkload(...)` | 멤버 수, 부하율 계산 |

> 3개 이상 도메인 조합이지만 단순 집계 읽기 전용이므로 Facade 대신 DashboardService 단독 사용.

---

## 주요 메서드

### `getPmDashboard(): PmDashboardResponse`

```
1. projectService.getProjects(null, null, ACTIVE) → activeProjects
2. 각 project의 taskCount, doneTaskCount, lateTaskCount 합산
3. averageProgress = sum(progress) / activeProjects.size()
4. memberService.getMembers(null) → members → memberCount
5. memberService.getTeamWorkloads(null, null, null) → averageLoadRate 계산
   (projectId 필터 없음 — 전체 멤버 대상이므로 memberIds = null 전달)
6. PmDashboardResponse 조립 반환
```

@Transactional(readOnly = true)

---

### `getMemberDashboard(Long memberId): MemberDashboardResponse`

```
1. memberService.getWorkload(memberId, thisWeekFrom, thisWeekTo)
   → loadRate, assignedHours, capacityHours
2. taskService.getMyTasks(memberId, null, null, null)
   → today/thisWeek/later 그룹핑
3. projectService.getProjects(memberId, null, ACTIVE) → 참여 프로젝트
4. nextDueDate = today + thisWeek + later Task 중 미완료 최소 endDate
5. MemberDashboardResponse 조립 반환
```

@Transactional(readOnly = true)

---

## 비즈니스 규칙

- PM 권한 체크: `/dashboard/pm` Controller에서 `@PreAuthorize("hasRole('PM')")`
- `averageLoadRate` 계산: 전체 멤버 loadRate의 산술 평균 (멤버 0명이면 0.0)
- `projects` 필드: MemberDashboard에서는 진행률·health만 포함하는 요약 형태
- `nextDueDate`: NULL 가능 (미완료 Task가 없는 경우)
