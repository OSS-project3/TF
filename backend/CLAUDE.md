# TeamFlow Backend

## 프로젝트 개요

팀 프로젝트 관리 도구. 프론트엔드(React)와 백엔드(Spring Boot)가 `d:\TF\`에 통합되어 있다.

```
d:\TF\
  frontend/        React + Vite SPA
  backend/         Spring Boot (Gradle 멀티 프로젝트 루트)
    backend/       실제 Spring Boot 모듈
  docker-compose.yml
```

Base URL: `/api/v1`

---

## 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA + Hibernate |
| DB | PostgreSQL (운영) / H2 인메모리 (로컬·Docker 데모) |
| Build | Gradle |
| Auth | Spring Security + JWT |
| Docs | Swagger (springdoc-openapi) |
| Frontend | React 18 + Vite, `d:\TF\frontend\` |

---

## 아키텍처

- **모놀리식** 구조. 도메인 기준으로 패키지를 분리하여 응집도↑ 결합도↓
- 레이어: `controller → service → repository → domain`
- 타 도메인 Repository 직접 주입 **금지**, Service 통해서만 접근
- 도메인 간 단방향 의존만 허용, 순환 참조 금지
- 3개 이상 도메인 Service 조합 시 Facade 도입

의존 방향:
```
auth → member
project → member
task → project, member
meeting → member, project, task
dashboard → project, task, member
ai → project, task, member
```

---

## 확정된 설계 결정사항

| 항목 | 결정 |
|------|------|
| 도메인 용어 | `Member` 통일 (`User` 사용 금지) |
| progress / health | DB 컬럼 없음. 조회 시 Task 집계로 실시간 계산 후 DTO에 담아 반환 |
| Schedule 엔티티 | 별도 엔티티 없음 — Task의 `startDate/endDate`로 표현 |
| Schedule API | **전용 엔드포인트 불필요** — 기존 Task API 재사용 |
| 인증 | 이메일/비밀번호 → JWT. payload: `{ "sub": memberId, "role": "PM" }` |
| 로그인 엔드포인트 | `POST /api/v1/auth/login` — `{ email, password }` → `{ accessToken }` |
| Member 인증 필드 | `email`(UNIQUE), `password`(BCrypt) — `Member` 엔티티에 포함 |
| Service 인터페이스 | 구현체 하나면 클래스로 직접 작성. AI 서비스처럼 교체 가능성 있는 경우만 인터페이스 도입 |
| Setter | Entity Setter 전체 공개 금지. 상태 변경은 의미있는 메서드(`complete()` 등)로 캡슐화 |
| 빌드 | Gradle 멀티 프로젝트. `settings.gradle`(루트) + `backend/build.gradle` |
| AI 단일 호출 | 프론트용 단일 호출 엔드포인트(`/ai/decompositions`, `/ai/meeting-summaries`) 별도 제공. Q&A 세션 방식은 Swagger 테스트용으로 유지 |
| gitBranch 해제 | `PATCH /tasks/{id}` 에 `gitBranch: ""` (빈 문자열) 전송 시 null로 저장됨 |

---

## 패키지 구조

패키지: `com.example.teamflow` (확정)

```
com.example.teamflow
  TeamflowApplication.java
  api/          AuthController, MemberController, ProjectController, TaskController
                DashboardController, MeetingController, AiController, WebhookController
  config/       JpaConfig, SwaggerConfig, SchedulerConfig
  common/
    entity/     BaseTimeEntity
    enums/      MemberRole, TaskStatus, TaskDifficulty, ProjectStatus, ProjectHealth, ScheduleKind
                AgentType, AiSessionStatus
    exception/  ErrorCode, BusinessException, GlobalExceptionHandler
    response/   ApiResponse, PageResponse
  domain/
    member/     entity(Member·MemberSkill), repository, service(MemberService·AuthService), dto
    project/    entity(Project·ProjectMember), repository, service(ProjectService), dto
    task/       entity(Task·TaskDependency·TaskExecutionLog), repository,
                service(TaskService·TaskAggregationService·WebhookService), dto(+GithubPrPayload)
    meeting/    entity(Meeting·MeetingAttendee·MeetingSummaryItem·MeetingTodo),
                repository, service(MeetingService), facade(MeetingTaskFacade), dto
    dashboard/  service(DashboardService), dto
    ai/         agent(RequirementAgent·TaskDecomposeAgent·AssignmentAgent·RiskAgent·MeetingAgent)
                detector(BottleneckDetector)
                dto(AiAgentResult·AiSummaryResponse·AiRiskResponse·AiDecomposeRequest·AiDecomposeResponse
                    MeetingAiRequest·BottleneckReport 등)
                entity(AiSession·AiRequestHistory·AgentDecisionLog)
                facade(AiProjectFacade)
                repository, scheduler(MonitoringScheduler)
                service(AiProjectService·MeetingAiService·MonitoringService)
  infra/
    openai/     OpenAiConfig, OpenAiClient
    security/   JwtTokenProvider, JwtAuthFilter, SecurityConfig, TokenBlacklist
```

---

## 공통 규칙

**응답 형식**
```json
{ "data": { ... }, "message": "ok" }
{ "data": { "items": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 } }
{ "error": { "code": "PROJECT_NOT_FOUND", "message": "..." } }
```

**인증**
- `/api/v1/auth/**`, `/api/v1/webhooks/**` 인증 불필요
- 그 외 전체 `Authorization: Bearer {token}` 필수
- `JwtAuthFilter` → `SecurityContext` 저장

**주요 Enum**
- `TaskStatus`: `TODO` `IN_PROGRESS` `DONE` `BLOCKED`
- `TaskDifficulty`: `EASY` `MEDIUM` `HARD`
- `ProjectHealth`: `OK` `WARN` `BAD` `IDLE`
- `MemberRole`: `PM` `FRONTEND` `BACKEND` `DESIGNER` `QA`

**설정 파일**
- `application.properties` (공통) + `application-local.properties` (H2, 로컬 개발) + `application-docker.properties` (H2, Docker 데모)
- `.properties` 형식으로 통일 (yml 미사용)
- `application-local.properties`는 git/docker 모두 제외 — 시크릿 포함

**코딩 규칙 (핵심)**
- `System.out.println()` 금지 → SLF4J Logger 사용
- `@Autowired` 필드 주입 금지 → 생성자 주입(`@RequiredArgsConstructor`)
- `@Transactional(readOnly = true)` 읽기 전용 메서드 필수
- Entity를 Controller까지 노출 금지 → DTO 변환은 Service에서
- DTO 네이밍: `{도메인}{동작}Request` / `{도메인}Response`

---

## 구현 현황

### ✅ Phase 1 — 완료 (Swagger 명세 포함)

> Swagger UI: `http://localhost:8080/swagger-ui.html`
> 우측 상단 Authorize 버튼 → Bearer 토큰 입력 후 전체 엔드포인트 테스트 가능

| 엔드포인트 | 파일 |
|------------|------|
| `POST /api/v1/auth/login` | `AuthController` → `AuthService` |
| `GET /api/v1/me` | `MemberController` → `MemberService` |
| `GET /api/v1/members` | `MemberController` → `MemberService` |
| `GET /api/v1/members/{id}` | `MemberController` → `MemberService` |
| `GET /api/v1/projects` | `ProjectController` → `ProjectService` |
| `POST /api/v1/projects` | `ProjectController` → `ProjectService` |
| `GET /api/v1/projects/{id}` | `ProjectController` → `ProjectService` |
| `GET /api/v1/projects/{id}/members` | `ProjectController` → `ProjectService` |
| `PUT /api/v1/projects/{id}/members` | `ProjectController` → `ProjectService` |
| `PATCH /api/v1/projects/{id}` | `ProjectController` → `ProjectService` |
| `DELETE /api/v1/projects/{id}` | `ProjectController` → `ProjectService` |
| `POST/DELETE /api/v1/projects/{id}/members/{id}` | `ProjectController` → `ProjectService` |
| `GET /api/v1/projects/{id}/tasks` | `TaskController` → `TaskService` |
| `POST /api/v1/projects/{id}/tasks` | `TaskController` → `TaskService` |
| `PATCH /api/v1/tasks/{id}/status` | `TaskController` → `TaskService` |
| `PATCH /api/v1/tasks/{id}` | `TaskController` → `TaskService` |
| `PATCH /api/v1/tasks/{id}/assignee` | `TaskController` → `TaskService` |
| `GET /api/v1/me/tasks` | `TaskController` → `TaskService` |

### ✅ Phase 2 — 완료 (Swagger 명세 포함)

**Meeting (2026-06-16)**

| 엔드포인트 | 파일 |
|------------|------|
| `GET /api/v1/meetings` | `MeetingController` → `MeetingService` |
| `GET /api/v1/meetings/{id}` | `MeetingController` → `MeetingService` |
| `POST /api/v1/meetings` | `MeetingController` → `MeetingService` |
| `POST /api/v1/meetings/{id}/ai-summary` | `AiController` → `MeetingAiService` |
| `POST /api/v1/meetings/{id}/tasks` | `MeetingController` → `MeetingTaskFacade` |

**Dashboard (2026-06-16)**

| 엔드포인트 | 파일 |
|------------|------|
| `GET /api/v1/dashboard/pm` | `DashboardController` → `DashboardService` |
| `GET /api/v1/dashboard/member` | `DashboardController` → `DashboardService` |
| `GET /api/v1/members/{id}/workload` | `MemberController` → `MemberService` |
| `GET /api/v1/team/workloads` | `MemberController` → `MemberService` |

**인증·인가·보안 (2026-06-16)**

| 엔드포인트 | 파일 |
|------------|------|
| `POST /api/v1/auth/logout` | `AuthController` → `AuthService` |
| `POST /api/v1/auth/register` | `AuthController` → `AuthService` |
| `PATCH /api/v1/me` | `MemberController` → `MemberService` |
| `PATCH /api/v1/me/password` | `MemberController` → `MemberService` |
| `DELETE /api/v1/me` | `MemberController` → `MemberService` |

- `TokenBlacklist`: jti(UUID) 기반 인메모리 블랙리스트
- `@PreAuthorize("hasRole('PM')")`: `POST /projects`, `PATCH/DELETE /projects/{id}`, `PUT/POST/DELETE /projects/{id}/members/**`, `GET /dashboard/pm`

### ✅ Phase 3 — 완료 (AI Agent, 2026-06-19)

**AI 엔드포인트**

| 엔드포인트 | 파일 | 용도 |
|------------|------|------|
| `POST /api/v1/meetings/{id}/ai-summary` | `AiController` → `MeetingAiService` | 저장된 회의록 ID 기반 요약 (Swagger 테스트용) |
| `POST /api/v1/projects/ai-generate` | `AiController` → `AiProjectFacade` | Q&A 세션 시작 (Swagger 테스트용) |
| `POST /api/v1/projects/ai-generate/{sessionId}/answers` | `AiController` → `AiProjectFacade` | Q&A 답변 제출 → 프로젝트+태스크 생성 |
| `GET /api/v1/projects/{id}/risks` | `AiController` → `MonitoringService` | 위험 요소 조회 |
| `POST /api/v1/admin/monitoring/trigger` | `AiController` → `MonitoringService` | 수동 모니터링 실행 (PM 전용) |

**AI Agent 구성**

| Agent | 역할 | 입력 → 출력 |
|-------|------|------------|
| `RequirementAgent` | 기능 설명 분석 → 추가 질문 생성 | feature → `List<QuestionItem>` |
| `TaskDecomposeAgent` | 기능 + 답변 → 태스크 분해 | feature + answers → `DecomposeResult` |
| `AssignmentAgent` | 태스크 목록 + 멤버 → 담당자 배정 | titles + members → `List<Assignment>` |
| `RiskAgent` | 병목 보고서 → 위험 항목 + 권고 | `BottleneckReport` → `AiRiskResponse` |
| `MeetingAgent` | 회의 내용 분석 → 요약 + TODO | `MeetingResponse` → `AiSummaryResponse` |

- `AiSummaryResponse.todos` 타입: `List<TodoItem>` — `TodoItem(String title, String assignee)`
  - `assignee` 필드명 확정 (구 `assigneeName`에서 변경됨)
- `MonitoringScheduler`: 매일 오전 9시 자동 실행, 매 시간 만료 세션 정리

### ✅ Phase 4 — GitHub Webhook 연동 (2026-06-20)

| 엔드포인트 | 파일 |
|------------|------|
| `POST /api/v1/webhooks/github` | `WebhookController` → `WebhookService` → `TaskService` |

- `Task.gitBranch` 컬럼 추가 (`VARCHAR(100)`, nullable)
- `TaskService.completeByGitBranch(branch)`: 브랜치명 일치 미완료 태스크 일괄 DONE
- `TaskService.linkGitBranch`: 빈 문자열(`""`) 입력 시 null 저장 → 브랜치 해제 가능
- `WebhookService.verifySignature()`: HMAC-SHA256, secret 미설정 시 skip
- `SecurityConfig`: `/api/v1/webhooks/**` permitAll

**사용 흐름**
1. PM이 ProjectDetail에서 태스크 옆 `⎇` 클릭 → 브랜치명 입력 → `PATCH /tasks/{id} { gitBranch }`
2. GitHub 웹훅 설정: URL=`https://{서버}/api/v1/webhooks/github`, Content-type=`application/json`
3. PR 머지 → GitHub가 자동 호출 → 브랜치 일치 태스크 DONE

### ✅ Phase 5 — 프론트엔드 연동 검증 및 수정 (2026-06-20)

**디렉토리 통합**
- 백엔드(`d:\teamflow-backend\`)를 `d:\TF\backend\`로 통합
- `docker-compose.yml` — `SPRING_PROFILES_ACTIVE=docker` 추가
- `application-docker.properties` 신규 — Docker용 H2 설정 (시크릿 없음)
- `backend/.dockerignore` — `application-local.properties` 제외

**AI 엔드포인트 불일치 수정**

프론트엔드가 단일 호출을 기대하는 반면 백엔드는 세션 기반 다단계 방식이었음. 프론트용 래퍼 엔드포인트 추가.

| 추가 엔드포인트 | 파일 | 설명 |
|----------------|------|------|
| `POST /api/v1/ai/meeting-summaries` | `AiController` → `MeetingAiService.summarizeFromNotes` | raw notes 직접 요약. 요청: `{ notes }`, 응답: `AiSummaryResponse` |
| `POST /api/v1/ai/decompositions` | `AiController` → `TaskDecomposeAgent` | 단일 호출 태스크 분해. 요청: `{ goal, deadline?, memberIds? }`, 응답: `{ tasks: [{title, phase, estimatedHours, difficulty}] }` |

- `TodoItem.assigneeName` → `TodoItem.assignee` 필드명 수정 (프론트 `t.assignee` 일치)
- `MeetingAgent` 시스템 프롬프트 JSON 키도 동일하게 수정

**프론트엔드 확인된 연결 현황**

| 화면 | 핵심 API | 상태 |
|------|---------|------|
| 로그인/회원가입 | `POST /auth/login`, `/auth/register` | ✅ |
| 대시보드 (PM) | `GET /dashboard/pm` → `PmDashboardResponse` 필드 일치 | ✅ |
| 대시보드 (멤버) | `GET /dashboard/member` → `MemberDashboardResponse` 필드 일치 | ✅ |
| 프로젝트 목록 | `GET /projects` → `page.items` 패턴 일치 | ✅ |
| 프로젝트 생성 | `POST /projects` → `{ id }` + `POST /ai/decompositions` → 태스크 자동 생성 | ✅ |
| 프로젝트 상세 | `GET /projects/{id}/tasks` → `adaptTask` 매핑 | ✅ |
| 태스크 브랜치 연결 | `PATCH /tasks/{id} { gitBranch }` — PM 인라인 UI | ✅ |
| 팀 | `GET /members`, `GET /team/workloads` | ✅ |
| 회의록 | `GET /meetings`, `POST /meetings` | ✅ |
| 회의 AI 요약 | `POST /ai/meeting-summaries` → `{ summary, todos[{title, assignee}] }` | ✅ |
| 일정 | `GET /projects/{id}/tasks` 재사용, 태스크 CRUD 없음 | ✅ |
| 설정 | `PATCH /me`, `PATCH /me/password`, `DELETE /me` | ✅ |

**프론트엔드 변경 파일 (2026-06-20)**
- `frontend/src/api/adapt.js` — `adaptTask`에 `gitBranch` 필드 추가
- `frontend/src/screens/ProjectDetail/ProjectDetail.jsx` — PM 전용 인라인 브랜치 연결/해제 UI
- *(기존)* `frontend/src/api/ai.js` — `POST /ai/meeting-summaries`, `POST /ai/decompositions` 호출
- *(기존)* `frontend/src/screens/Meetings/Meetings.jsx` — AI 요약 → 회의록 저장 플로우

### ✅ Phase 6 — 워크스페이스 격리 · 초대링크 · 프로젝트 개선 · 태스크 일정 (2026-06-21)

#### 워크스페이스 격리 (Workspace Isolation)

**백엔드 신규 파일**
- `domain/workspace/entity/Workspace.java` — `id`, `name`, `createdByMemberId`, `createdAt`; `static create(name, memberId)`
- `domain/invitation/entity/Invitation.java` — UUID token, 7일 만료, `usedAt`, `consume()`, `isExpired()`, `isUsed()`
- `domain/invitation/service/InvitationService.java` — `create(workspaceId, memberId)` → token 반환; `consume(token)` → workspaceId 반환 (INVITE_INVALID/USED/EXPIRED)
- `common/security/WorkspaceContext.java` — `SecurityContextHolder`에서 `authentication.getDetails()` (Long workspaceId) 읽기

**JWT 구조 변경**
- `JwtTokenProvider.generateToken(memberId, role, workspaceId)` — `wid` 클레임 추가
- `JwtAuthFilter` — `workspaceId = jwtTokenProvider.getWorkspaceId(token)` → `authentication.setDetails(workspaceId)`
- `Member.workspaceId` 필드 추가 (nullable Long)

**서비스 격리 적용**
- `AuthService.register()` — inviteToken 있으면 초대 워크스페이스 참여, 없으면 신규 워크스페이스 자동 생성
- `AuthService.login()` — `member.getWorkspaceId()`로 JWT 발급
- `AuthService.acceptInvitation(token, memberId)` — 기존 사용자 워크스페이스 합류 + 신규 JWT 반환
- `ProjectService`, `MemberService`, `MeetingService` — 모든 조회/생성에 `WorkspaceContext.get()` 적용
- `Project.workspaceId`, `Meeting.workspaceId` 필드 추가

**신규 엔드포인트**

| 엔드포인트 | 파일 | 설명 |
|------------|------|------|
| `POST /api/v1/invitations` | `InvitationController` → `InvitationService` | 초대 토큰 생성 (PM 전용) |
| `POST /api/v1/invitations/accept` | `InvitationController` → `AuthService` | 기존 사용자 워크스페이스 합류 → 신규 JWT |

**ErrorCode 추가**: `INVITE_INVALID`, `INVITE_EXPIRED`, `INVITE_USED`, `WORKSPACE_NOT_FOUND`

#### 초대링크 플로우

- 초대 URL: `{window.location.origin}/signup?token={uuid}` — 배포 시 도메인 자동 적용
- 신규 사용자: Signup 페이지에서 token 인식 → 회원가입 시 해당 워크스페이스 합류
- 기존 사용자: Login 페이지에서 token 인식 → 로그인 후 `POST /invitations/accept` 자동 호출 → 신규 JWT 저장
- "로그인" 링크 보존: Signup의 로그인 버튼이 `/login?token={token}` 으로 이동 (토큰 유실 버그 수정)

**프론트엔드 파일**
- `frontend/src/api/invitations.js` — `createInvitation()`, `acceptInvitation(token, remember)`
- `frontend/src/api/index.js` — `invitationApi` export 추가
- `frontend/src/pages/Signup/Signup.jsx` — token 파라미터 인식, 초대 배너, 로그인 링크 token 보존
- `frontend/src/pages/Login/Login.jsx` — token 파라미터 인식, 로그인 후 acceptInvitation 호출
- `frontend/src/context/AuthContext.jsx` — `register(name, email, password, role, inviteToken)` 파라미터 추가
- `frontend/src/screens/Settings/Settings.jsx` — "팀원 초대" 카드, 초대링크 생성·복사 UI

#### 프로젝트 생성 개선

**CreateProjectModal 전면 재작성** (`frontend/src/screens/Projects/CreateProjectModal.jsx`)
- 모드 선택 화면: **AI 자동 생성** | **직접 생성**
- AI 모드: 3단계 (기본 정보 → 팀원 선택 → AI 태스크 편집 가능 미리보기)
- 직접 생성 모드: 2단계 (기본 정보/팀원 → 태스크 직접 입력)
- 태스크 입력 그리드: `1fr | 68px | 96px | 96px | 84px | 96px | 48px | 24px` (제목 | 단계 | 시작일 | 마감일 | 난이도 | 담당자 | 시간(선택) | ×)

**ProjectDetail 개선** (`frontend/src/screens/ProjectDetail/ProjectDetail.jsx`)
- PM 전용 [수정][삭제] 버튼 (프로젝트 헤더)
- 수정 모달: 프로젝트명, 마감일, 팀원 선택
- 삭제 확인 모달 → `onArchive(project.id)`
- 프로젝트 목표/설명 상세 화면에서 숨김
- PM 전용 태스크 CRUD: `+ 작업 추가` 버튼, 인라인 편집, 삭제(×)

**Workspace.jsx 핸들러 추가**
- `updateProject(projectId, { name, goal, deadline, memberIds })` — PATCH + 멤버 교체
- `archiveProject(projectId)` — DELETE + 목록으로 돌아가기
- `createProject()` — 태스크 생성 시 `startDate`/`endDate` 전달

#### 태스크 일정 기반 입력 전환

**백엔드**
- `TaskCreateRequest.estimatedHours`: `int` (필수) → `Integer` (선택, null 허용)
- `TaskService.createTask()`: `estimatedHours != null ? estimatedHours : 0` 처리
- `TaskService.deleteTask()` 추가: 의존관계(`TaskDependency` 양방향) + 실행로그 먼저 삭제 후 태스크 삭제
- `TaskDependencyRepository.deleteAllByPrerequisiteTaskId(Long)` 추가
- `TaskExecutionLogRepository.deleteAllByTaskId(Long)` 추가
- `DELETE /api/v1/tasks/{taskId}` 엔드포인트 추가

**프론트엔드 — 태스크 입력 필드 변경**
- `estimatedHours`: 필수 숫자 → 선택 입력 (빈 값 허용)
- `startDate` / `endDate`: date picker로 기본 입력
- `frontend/src/api/tasks.js` — `deleteTask(taskId)` 추가
- `ProjectDetail.jsx` 태스크 추가 폼: 7컬럼 그리드 (제목 | 단계 | 시작일 | 마감일 | 난이도 | 담당자 | 시간(선택))
- `ProjectDetail.jsx` 인라인 편집: 동일 7컬럼 그리드에 date picker 포함
- `ProjectDetail.jsx` 태스크 표시: `{t.hours}h` → `~MM/DD` (endDate), hours > 0이면 fallback

---

## 의사결정 완료 사항

1. **AI 생성 프로젝트 — 배정 멤버 자동 ProjectMember 등록**: `AiProjectFacade.completeGeneration()` 에서 자동 추가
2. **TokenBlacklist 인메모리 유지**: 대학 프로젝트 규모에서 허용
3. **MonitoringService 수동 트리거**: `POST /api/v1/admin/monitoring/trigger` (PM 전용)
4. **만료된 AiSession 정리**: `@Scheduled(cron = "0 0 * * * *")` 매 시간 자동 삭제
5. **MeetingTodo → Task 변환**: `MeetingTaskFacade` + `POST /api/v1/meetings/{id}/tasks`
6. **AI 단일 호출 엔드포인트**: 프론트 단일 호출 패턴 수용. Q&A 세션(`/ai-generate`)은 Swagger 테스트 전용으로 유지
7. **워크스페이스 격리**: `workspaceId`를 JWT `wid` 클레임에 저장, `JwtAuthFilter`에서 `authentication.setDetails(workspaceId)`, `WorkspaceContext.get()`으로 조회 — ThreadLocal 없이 SecurityContext 활용
8. **초대 수락 엔드포인트 위치**: `InvitationController`에 두고 `AuthService` 주입 (초대 관련 로직이지만 JWT 재발급을 수반하므로)
9. **기존 사용자 초대 합류**: `POST /invitations/accept` — 토큰 소비 + `member.workspaceId` 업데이트 + 신규 JWT 발급. 프론트 Login에서 로그인 성공 후 자동 호출
10. **태스크 삭제 cascade**: `TaskDependency` 양방향(taskId/prerequisiteTaskId) + `TaskExecutionLog` 먼저 삭제 후 태스크 삭제 (외래키 제약 없이 수동 처리)
11. **estimatedHours 선택 전환**: 일정(startDate/endDate) 기반이 주 입력. hours는 null 허용, 0 저장. UI에서 마지막 컬럼에 선택 입력으로 배치

---

## 상세 명세 위치

| 도메인 | ERD | API | Service 흐름 |
|--------|-----|-----|-------------|
| 공통 규칙 | — | [conventions.md](docs/common/conventions.md) | — |
| Member | [erd.md](docs/member/erd.md) | [api.md](docs/member/api.md) | [service-flow.md](docs/member/service-flow.md) |
| Project | [erd.md](docs/project/erd.md) | [api.md](docs/project/api.md) | [service-flow.md](docs/project/service-flow.md) |
| Task + Schedule | [erd.md](docs/task/erd.md) | [api.md](docs/task/api.md) | [service-flow.md](docs/task/service-flow.md) |
| Meeting | [erd.md](docs/meeting/erd.md) | [api.md](docs/meeting/api.md) | [service-flow.md](docs/meeting/service-flow.md) |
| Dashboard | — | [api.md](docs/dashboard/api.md) | [service-flow.md](docs/dashboard/service-flow.md) |
| AI | — | [api.md](docs/ai/api.md) | [service-flow.md](docs/ai/service-flow.md) |

---

## 프로젝트 시작 방법

### Docker Compose (권장 — 프론트+백엔드 한 번에)

```bash
# 1. OpenAI 키 설정 (AI 기능 사용 시)
cp .env.example .env   # 루트 d:\TF\.env
# OPENAI_API_KEY=sk-... 입력

# 2. 실행
docker compose up --build

# 접속
# 프론트엔드: http://localhost:3000
# Swagger:    http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console
```

### 로컬 개발 (IntelliJ + npm)

1. IntelliJ에서 `d:\TF\backend\backend\build.gradle`을 Gradle 프로젝트로 임포트
2. Active profile: `local` (H2 자동 사용, `application-local.properties` 필요)
3. 실행: `TeamflowApplication.main()` (포트 8080)
4. 프론트: `d:\TF\frontend\`에서 `npm run dev` (포트 5173, `/api` → 8080 프록시)
