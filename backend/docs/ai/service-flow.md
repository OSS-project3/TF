# AI 도메인 — Service 흐름

## 담당 Service

- `com.teamflow.ai.service.AiMessageService` — AI Thread 메시지 생성
- `com.teamflow.ai.service.TaskDecomposeService` — 작업 분해
- `com.teamflow.ai.service.AssignmentService` — 자동 배정
- `com.teamflow.ai.service.GoalSuggestionService` — 목표 개선 제안

> 모든 AI 서비스는 구현체 교체 가능성이 있으므로 **인터페이스 도입 필수**.

```java
public interface TaskDecomposeService {
    DecomposeResponse decompose(Long projectId, DecomposeRequest req);
}
// 구현체: RuleBasedTaskDecomposeService (Phase 3 초기)
//          LlmTaskDecomposeService (Phase 3 LLM 연동 후)
```

---

## 의존 도메인

| 도메인 | 메서드 | 이유 |
|--------|--------|------|
| `ProjectService` | `getProject(id)` | 프로젝트 존재 확인 및 정보 조회 |
| `TaskService` | `getTasksByProject(...)` | 배정 대상 Task 목록 |
| `MemberService` | `getProjectMembers(...)`, `getWorkload(...)` | 멤버 역할·부하 조회 |

---

## AiMessageService

### `getMessages(String scope, String role, Long projectId): List<AiMessageResponse>`

```
1. scope, role, projectId 기반으로 컨텍스트 데이터 수집
   (scope=detail → projectService.getProject(projectId), taskService.getAggregation(projectId))
2. 규칙 기반 메시지 생성 or LLM 호출
3. List<AiMessageResponse> 반환
```

초기 구현: scope별 고정 메시지 Map으로 반환.

@Transactional(readOnly = true)

---

## TaskDecomposeService

### `decompose(Long projectId, DecomposeRequest req): DecomposeResponse`

```
1. projectService.getProject(projectId) → PROJECT_NOT_FOUND
2. memberService.getMembersByIds(req.memberIds) → 역할 목록 수집
3. goal, deadline, 멤버 역할을 기반으로 Task 목록 생성
   (규칙: 역할당 phase 배분, deadline 기준 estimatedHours 산정)
4. reasoningMessages 조립
5. DecomposeResponse { reasoningMessages, tasks } 반환 (DB 저장 없음)
```

@Transactional(readOnly = true)

---

## AssignmentService

### `assign(Long projectId, AssignRequest req): AssignResponse`

```
1. taskService.getTasksByProject(projectId, null) → 미배정 Task 목록
2. memberService.getProjectMembers(projectId) → 멤버 목록
3. 각 멤버의 현재 workload 조회 (memberService.getWorkload)
4. strategy에 따라 배정 계획 생성:
   ROLE_AND_LOAD_BALANCE: 역할 매칭 우선, 부하 낮은 멤버 우선 배정
5. AssignResponse { assignments, workloads } 반환 (DB 저장 없음)
```

@Transactional(readOnly = true)

---

## GoalSuggestionService

### `suggest(String goal): GoalSuggestionResponse`

```
1. goal 텍스트 분석
   (규칙 기반: 측정 가능 키워드 없으면 수치화 제안 / LLM: 프롬프트 주입)
2. GoalSuggestionResponse { suggestion } 반환
```

@Transactional(readOnly = true)

---

## 비즈니스 규칙

- AI 응답 결과는 **DB에 저장하지 않음** (Phase 3 설계 미결정 사항)
  → 저장이 필요해지면 별도 ai_result 테이블 추가
- 모든 AI Service는 인터페이스로 선언하여 `RuleBased` → `Llm` 구현체 교체를 지원
- LLM 연동 시 API 키는 `@ConfigurationProperties`로 관리 (하드코딩 금지)
- 배정/분해 결과는 제안(suggestion)일 뿐 — 실제 반영은 프론트엔드에서 확인 후 별도 API 호출
