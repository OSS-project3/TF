# Member 도메인 — Service 흐름

## 담당 Service

`com.teamflow.member.service.MemberService`

---

## 의존 도메인

| 도메인 | 목적 |
|--------|------|
| Task (Phase 2) | 업무량 계산 시 Task 집계 조회 |

> Phase 1에서는 workload API가 없으므로 MemberService는 완전 독립 구현 가능.

> `ProjectService`는 의존하지 않는다. `getTeamWorkloads`의 `projectId` 필터는 Controller에서 처리한다. (MemberService → ProjectService → MemberService 순환 방지)

---

## 주요 메서드

### `getMe(Long memberId): MemberResponse`

```
1. memberRepository.findById(memberId) → 없으면 MEMBER_NOT_FOUND
2. skills 조회 (Member fetchJoin 또는 별도 쿼리)
3. MemberResponse.from(member, skills) 반환
```

@Transactional(readOnly = true)

---

### `getMembers(@Nullable MemberRole role): List<MemberResponse>`

```
1. role이 null이면 전체 조회 / 아니면 role 필터 조회
2. MemberSkill N+1 방지: findAllWithSkills() — LEFT JOIN FETCH 쿼리
3. List<MemberResponse> 변환 반환
```

@Transactional(readOnly = true)

---

### `getMember(Long memberId): MemberResponse`

```
1. memberRepository.findById(memberId) → 없으면 MEMBER_NOT_FOUND
2. MemberResponse 반환
```

@Transactional(readOnly = true)

---

### `getWorkload(Long memberId, LocalDate from, LocalDate to): WorkloadResponse`

```
1. getMember(memberId) — 존재 확인 겸 용량 조회
2. taskService.findByAssigneeAndDateRange(memberId, from, to) → List<TaskSummary>
3. assignedHours = sum(estimatedHours)
4. loadRate = assignedHours / member.weeklyCapacityHours
5. projectCount = distinct projectId 수
6. WorkloadResponse 반환
```

@Transactional(readOnly = true)  
의존: `TaskService`

---

### `getTeamWorkloads(@Nullable List<Long> memberIds, @Nullable LocalDate from, @Nullable LocalDate to): List<TeamWorkloadResponse>`

`memberIds`가 null이면 전체 멤버, 아니면 지정 멤버만 집계.  
projectId 필터는 Controller에서 먼저 처리한 뒤 memberIds로 전달한다.

```
1. memberIds가 null이면 memberRepository.findAll()
   아니면 memberRepository.findAllById(memberIds)
2. 각 멤버별 getWorkload(id, from, to) 호출하여 집계
3. List<TeamWorkloadResponse> 변환 반환
```

@Transactional(readOnly = true)  
의존: `TaskService`

Controller 처리 예시:

```java
// GET /team/workloads?projectId=1
List<Long> memberIds = projectId != null
    ? projectService.getMemberIds(projectId)  // ProjectService 호출은 Controller에서
    : null;
return memberService.getTeamWorkloads(memberIds, from, to);
```

---

## 비즈니스 규칙

- `memberId`는 Controller에서 `@AuthenticationPrincipal`로 추출 후 Service에 전달
- skills는 Member와 항상 함께 조회 — 별도 skill 조회 API 없음
- `loadRate` 상한 없음 — 1.0 초과 시 과부하 상태를 그대로 표현
- MemberSkill 추가/삭제 API는 현재 명세에 없음 (필요 시 추가)
