# TeamFlow Backend

## 프로젝트 개요

팀 프로젝트 관리 도구. 프론트엔드(React)는 `frontend/`에 구현되어 있으며 현재 seed data + local state로 동작 중. 백엔드 연결 시 `docs/API_REQUIREMENTS.md`의 API로 대체한다.

Base URL: `/api/v1`

---

## 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA + Hibernate |
| DB | PostgreSQL |
| Build | Gradle |
| Auth | Spring Security + JWT |
| Docs | Swagger (springdoc-openapi) |

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
| Schedule API | **전용 엔드포인트 불필요** — 프론트 Schedule 화면이 단일 프로젝트 기준이고 `projectName`은 props로 보유, `kind` 판정에 필요한 `isCriticalPath·isLateRisk·status`도 `TaskResponse`에 포함됨. 기존 Task API 2개로 커버: 목록 `GET /projects/{id}/tasks`, 추가 `POST /projects/{id}/tasks` |
| Schedule 일정 추가 | 프론트 모달이 `estimatedHours·difficulty`를 입력받지 않으므로 연결 시 기본값(`estimatedHours: 0, difficulty: "EASY"`) 전송 또는 해당 필드를 선택적으로 변경 필요 |
| 인증 | 이메일/비밀번호 → JWT. payload: `{ "sub": memberId, "role": "PM" }` |
| 로그인 엔드포인트 | `POST /api/v1/auth/login` — `{ email, password }` → `{ accessToken }` |
| Member 인증 필드 | `email`(UNIQUE), `password`(BCrypt) — `Member` 엔티티에 포함 |
| Service 인터페이스 | 구현체 하나면 클래스로 직접 작성. AI 서비스처럼 교체 가능성 있는 경우만 인터페이스 도입 |
| Setter | Entity Setter 전체 공개 금지. 상태 변경은 의미있는 메서드(`complete()` 등)로 캡슐화 |
| 빌드 | Gradle 멀티 프로젝트. `settings.gradle`(루트) + `backend/build.gradle` |

---

## 패키지 구조

패키지: `com.example.teamflow` (확정)

```
com.example.teamflow
  TeamflowApplication.java
  api/          AuthController, MemberController, ProjectController, TaskController
  config/       JpaConfig, SwaggerConfig
  common/
    entity/     BaseTimeEntity
    enums/      MemberRole, TaskStatus, TaskDifficulty, ProjectStatus, ProjectHealth, ScheduleKind
    exception/  ErrorCode, BusinessException, GlobalExceptionHandler
    response/   ApiResponse, PageResponse
  domain/
    member/     entity(Member·MemberSkill), repository, service(MemberService·AuthService), dto
    project/    entity(Project·ProjectMember), repository, service(ProjectService), dto
    task/       entity(Task·TaskDependency), repository, service(TaskService·TaskAggregationService), dto
    meeting/    (Phase 2)
  infra/
    security/   JwtTokenProvider, JwtAuthFilter, SecurityConfig
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
- `/api/v1/auth/**` 인증 불필요
- 그 외 전체 `Authorization: Bearer {token}` 필수
- `JwtAuthFilter` → `SecurityContext` 저장

**주요 Enum**
- `TaskStatus`: `TODO` `IN_PROGRESS` `DONE` `BLOCKED`
- `TaskDifficulty`: `EASY` `MEDIUM` `HARD`
- `ProjectHealth`: `OK` `WARN` `BAD` `IDLE`
- `MemberRole`: `PM` `FRONTEND` `BACKEND` `DESIGNER` `QA`

**설정 파일**
- `application.properties` (공통) + `application-local.properties` (H2, 로컬 개발)
- `.properties` 형식으로 통일 (yml 미사용)

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

**Swagger 작업 내역 (2026-06-05)**
- `config/SwaggerConfig.java` 신규 생성 — JWT Bearer 인증 스킴, API 전체 설명 포함
- 컨트롤러 4개 (`Auth`, `Member`, `Project`, `Task`) `@Tag` · `@Operation` · `@SecurityRequirement` · `@Parameter` 애노테이션 추가
- 요청 DTO 8개 (`LoginRequest`, `ProjectCreateRequest`, `ProjectUpdateRequest`, `ProjectMemberReplaceRequest`, `TaskCreateRequest`, `TaskUpdateRequest`, `TaskStatusUpdateRequest`, `TaskAssigneeUpdateRequest`) `@Schema` 애노테이션으로 예시값 추가

**프론트엔드 연결 가능 범위 (Phase 1 기준)**
| 프론트 화면 | 연결 가능 |
|------------|-----------|
| 로그인 | ✅ |
| 팀/멤버 목록 | ✅ |
| 프로젝트 목록·상세 | ✅ |
| 태스크 보드 | ✅ |
| 내 태스크 | ✅ |
| 대시보드 | ✅ |
| 일정(Schedule) | ✅ (Task API 재사용, 별도 구현 불필요) |
| 회의(Meetings) | ✅ |

### ✅ Phase 2 — 완료 (Swagger 명세 포함)

**Meeting 작업 내역 (2026-06-16)**

| 엔드포인트 | 파일 |
|------------|------|
| `GET /api/v1/meetings` | `MeetingController` → `MeetingService` |
| `GET /api/v1/meetings/{id}` | `MeetingController` → `MeetingService` |
| `POST /api/v1/meetings` | `MeetingController` → `MeetingService` |

- 엔티티 4개: `Meeting`, `MeetingAttendee`, `MeetingSummaryItem`, `MeetingTodo`
- Repository 4개, DTO 5개 (`MeetingResponse`, `MeetingTodoResponse`, `MeetingCreateRequest`, `MeetingTodoRequest`, `MeetingCreateResponse`)
- `MeetingService`, `MeetingController` — Swagger `@Tag` · `@Operation` · `@SecurityRequirement` · `@Parameter` · `@Schema` 포함
- Phase 3 (`POST /meeting-summaries` AI 요약, `POST /meetings/{id}/tasks` TODO→Task 변환)는 미구현

**Dashboard 작업 내역 (2026-06-16)**

| 엔드포인트 | 파일 |
|------------|------|
| `GET /api/v1/dashboard/pm` | `DashboardController` → `DashboardService` |
| `GET /api/v1/dashboard/member` | `DashboardController` → `DashboardService` |
| `GET /api/v1/members/{id}/workload` | `MemberController` → `MemberService` |
| `GET /api/v1/team/workloads` | `MemberController` → `MemberService` |

- `DashboardService`: `ProjectService` + `MemberService` + `TaskService` 집계 (읽기 전용, Facade 미도입)
- `MemberService.getWorkload / getTeamWorkloads`: `TaskAggregationService` 사용 (`TaskService` 직접 의존 시 순환 발생 방지)
- `TaskAggregationService.findByAssigneeAndDateRange`: from/to null 시 전체 기간 폴백 처리
- DTO: `WorkloadResponse`, `TeamWorkloadResponse`, `PmDashboardResponse`, `MemberDashboardResponse`, `ProjectSummary`
- ~~`GET /schedules` · `POST /schedules` · `PATCH /schedules/{id}`~~ → **불필요 (기존 Task API 재사용)**

### ✅ 인증·인가·보안 (2026-06-16)

**JWT 무효화 (로그아웃)**

| 엔드포인트 | 파일 |
|------------|------|
| `POST /api/v1/auth/logout` | `AuthController` → `AuthService` |

- `TokenBlacklist`: jti(UUID) 기반 인메모리 블랙리스트, 만료 시 자동 제거
- `JwtTokenProvider`: 토큰 생성 시 `jti` 클레임 추가, `getJti / getExpiration` 메서드
- `JwtAuthFilter`: 블랙리스트 jti 체크 후 SecurityContext 설정 (유효 토큰이어도 블랙리스트면 인증 거부)

**인가 (Authorization)**
- `POST /projects`, `PATCH /projects/{id}`, `DELETE /projects/{id}`, `PUT/POST/DELETE /projects/{id}/members/**` → `@PreAuthorize("hasRole('PM')")`
- `GET /dashboard/pm` → `@PreAuthorize("hasRole('PM')")`
- 그 외 전체 → `anyRequest().authenticated()` (SecurityConfig 전역)

### ✅ 회원 CRUD (2026-06-16)

| 엔드포인트 | 설명 |
|------------|------|
| `POST /api/v1/auth/register` | 회원가입 + JWT 즉시 발급 |
| `PATCH /api/v1/me` | 이름·이니셜·가용시간·스킬 수정 |
| `PATCH /api/v1/me/password` | 현재 비밀번호 확인 후 변경 |
| `DELETE /api/v1/me` | 프로젝트 멤버십 제거 → 계정 삭제 → 토큰 무효화 |

- `RegisterRequest`, `MemberUpdateRequest`, `PasswordChangeRequest` DTO 추가
- `Member.updateProfile / clearSkills` 메서드 추가
- `ErrorCode`: `DUPLICATE_EMAIL`, `WRONG_PASSWORD` 추가
- `ProjectMemberRepository.deleteAllByMemberId`, `ProjectService.removeMemberFromAllProjects` 추가
- `MemberService`: `PasswordEncoder` 주입, `updateMe / changePassword / deleteMe` 추가
- `AuthService.register` 추가

### ✅ Swagger 에러 응답 문서화 (2026-06-16)

- `SwaggerConfig` 전역 설명에 공통 에러 코드 표 추가 (400/401/403/404/409/422/500)
- 컨트롤러 6개 전체 엔드포인트에 `@io.swagger.v3.oas.annotations.responses.ApiResponse` 추가
  - 이름 충돌 (`com.example.teamflow.common.response.ApiResponse`) 때문에 완전 한정명(FQN) 사용
  - 400(INVALID_INPUT), 401(WRONG_PASSWORD 등), 403(FORBIDDEN), 404(*_NOT_FOUND), 409(DUPLICATE_*) 명시

### ⬜ Phase 3 — 미구현 (AI 기능)

- AI 기능 전체 (`/ai/**` · `/meeting-summaries` · `/schedule-proposals`)

---

## 상세 명세 위치

도메인별 분리된 명세 (구현 시 해당 도메인 폴더만 읽을 것):

| 도메인 | ERD | API | Service 흐름 |
|--------|-----|-----|-------------|
| 공통 규칙 | — | [conventions.md](docs/common/conventions.md) | — |
| Member | [erd.md](docs/member/erd.md) | [api.md](docs/member/api.md) | [service-flow.md](docs/member/service-flow.md) |
| Project | [erd.md](docs/project/erd.md) | [api.md](docs/project/api.md) | [service-flow.md](docs/project/service-flow.md) |
| Task + Schedule | [erd.md](docs/task/erd.md) | [api.md](docs/task/api.md) | [service-flow.md](docs/task/service-flow.md) |
| Meeting | [erd.md](docs/meeting/erd.md) | [api.md](docs/meeting/api.md) | [service-flow.md](docs/meeting/service-flow.md) |
| Dashboard | — | [api.md](docs/dashboard/api.md) | [service-flow.md](docs/dashboard/service-flow.md) |
| AI | — | [api.md](docs/ai/api.md) | [service-flow.md](docs/ai/service-flow.md) |

기타:
| 파일 | 내용 |
|------|------|
| `docs/arch/BACKEND_REQUIREMENTS.md` | 아키텍처·레이어 규칙 |
| `docs/agent/AGENT.md` | 코드 작성 원칙·네이밍 컨벤션 |
| `frontend/src/` | 프론트엔드 소스 |

## 프로젝트 시작 방법

1. IntelliJ에서 `backend/build.gradle`을 Gradle 프로젝트로 임포트
2. Active profile: `local` (H2 인메모리 DB 자동 사용)
3. 실행: `TeamflowApplication.main()`
4. H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:teamflow`)
5. Swagger: `http://localhost:8080/swagger-ui.html`
