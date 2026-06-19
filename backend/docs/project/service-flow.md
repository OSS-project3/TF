# Project 도메인 — Service 흐름

## 담당 Service

`com.teamflow.project.service.ProjectService`

---

## 의존 도메인

| 도메인 | 메서드 | 이유 |
|--------|--------|------|
| `MemberService` | `getMember(id)` | 멤버 존재 확인, 프로젝트 멤버 정보 조회 |
| `TaskService` | `getAggregation(projectId)` | progress·health·taskCount 집계 계산 |

---

## 주요 메서드

### `getProjects(Long memberId, ProjectHealth health, ProjectStatus status): List<ProjectResponse>`

```
1. 필터 조건으로 Project 조회
   - memberId가 있으면 project_member JOIN으로 해당 멤버 참여 프로젝트만
   - status 필터 (기본 ACTIVE)
2. 각 Project에 대해 taskService.getAggregation(projectId) → TaskAggregation
3. health 필터가 있으면 집계 결과로 2차 필터
4. ProjectResponse.from(project, aggregation) 변환 반환
```

@Transactional(readOnly = true)

---

### `createProject(ProjectCreateRequest req): ProjectCreateResponse`

```
1. req.memberIds 각각 memberService.getMember(id) 호출 → MEMBER_NOT_FOUND 검증
2. Project 엔티티 생성 및 저장
3. memberIds → ProjectMember 행 일괄 생성 (saveAll)
4. ProjectCreateResponse { id } 반환
```

@Transactional

---

### `getProject(Long projectId): ProjectResponse`

```
1. projectRepository.findById(id) → PROJECT_NOT_FOUND
2. taskService.getAggregation(projectId) → 집계 계산
3. ProjectResponse 반환
```

@Transactional(readOnly = true)

---

### `updateProject(Long projectId, ProjectUpdateRequest req): ProjectResponse`

```
1. 프로젝트 존재 확인
2. 각 필드 null 체크 후 변경 (project.updateName(), project.updateGoal(), project.updateDeadline())
3. 저장 (Dirty Checking) 후 getProject() 결과 반환
```

@Transactional

---

### `archiveProject(Long projectId)`

```
1. 프로젝트 존재 확인
2. project.archive() → status = ARCHIVED
```

@Transactional

---

### `getProjectMembers(Long projectId): List<MemberResponse>`

```
1. 프로젝트 존재 확인
2. projectMemberRepository.findMemberIdsByProjectId(projectId)
3. memberService.getMembersByIds(memberIds) 호출
4. List<MemberResponse> 반환
```

@Transactional(readOnly = true)

---

### `replaceProjectMembers(Long projectId, List<Long> memberIds)`

```
1. 프로젝트 존재 확인
2. memberIds 전체 유효성 확인 (memberService)
3. projectMemberRepository.deleteByProjectId(projectId)
4. ProjectMember 일괄 생성 (saveAll)
```

@Transactional

---

### `addProjectMember(Long projectId, Long memberId)`

```
1. 프로젝트, 멤버 존재 확인
2. 이미 존재하면 DUPLICATE_PROJECT_MEMBER
3. ProjectMember 생성 및 저장
```

@Transactional

---

### `removeProjectMember(Long projectId, Long memberId)`

```
1. projectMemberRepository.findByProjectIdAndMemberId() → 없으면 MEMBER_NOT_FOUND
2. 삭제
```

@Transactional

---

## 비즈니스 규칙

- PM 권한 체크: Controller에서 `@PreAuthorize("hasRole('PM')")` 적용
- 아카이브 후 ARCHIVED 프로젝트는 기본 목록 조회에서 제외 (`status=ACTIVE` 기본 필터)
- `TaskAggregation`은 별도 DTO record로 정의:
  ```java
  record TaskAggregation(int total, int done, int late) {}
  ```
- 멤버 전체 교체(`PUT /members`)는 기존 행 삭제 후 재삽입 — 순서 보장 불필요
