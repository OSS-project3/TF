# AGENT.md — 전체 행동 규칙

## 1. 프로젝트 개요

본 문서는 **TeamFlow AI** 서비스의 백엔드 시스템을 Spring Boot 기반으로 설계·구현하기 위한 명세서이다.

프로젝트 관리, 팀원 관리, 작업 관리, 일정 생성, 회의록 관리, AI 연동, 진행률 집계 기능을 포함한다.
---

## 2. 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA + Hibernate |
| DB | PostgreSQL |
| Migration | Flyway |
| Storage | AWS S3 |
| Build | Gradle |
| Auth | Spring Security + JWT |
| Docs | Swagger (springdoc-openapi) |

---

## 3. 코드 작성 원칙

### 3-1. 레이어 역할 엄수

- **Controller** — HTTP 요청/응답만 처리. 비즈니스 로직 금지.
- **Service** — 비즈니스 로직 전담. 트랜잭션 경계 여기서 관리.
- **Repository** — DB 접근만 담당. 쿼리 외 로직 금지.
- **Entity** — 상태 변경 메서드는 엔티티 내부에 위치(도메인 모델 원칙).

### 3-2. DTO 규칙

- Entity를 Controller까지 절대 노출하지 않는다.
- Request DTO는 `@Valid` 어노테이션으로 입력 검증.
- Response DTO는 필요한 필드만 포함(최소 노출 원칙).
- 네이밍: `{도메인}{동작}Request.java` / `{도메인}Response.java`
- DTO ↔ Entity 변환은 Service 또는 DTO의 정적 팩토리 메서드(from)에서 수행한다.
- 단순 데이터 전달 목적이며 상태 변경이 필요 없는 DTO는 `record` 사용을 권장한다.

### 3-3. 예외 처리

- 모든 예외는 `GlobalExceptionHandler`에서 일괄 처리.
- 커스텀 예외는 `common/exception/` 하위에 위치.
- 예외 응답 형식은 `docs/api/API_REQUIREMENTS.md` 에러 형식을 따른다.

### 3-4. 트랜잭션

- 읽기 전용 메서드는 반드시 `@Transactional(readOnly = true)` 사용.
- 쓰기 메서드는 `@Transactional` 명시.
- Service 외 레이어에서 `@Transactional` 사용 금지.

### 3-5. 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `ApplicationService` |
| 메서드/변수 | camelCase | `findByCardNumber` |
| 상수 | UPPER_SNAKE | `MAX_PHOTO_SIZE` |
| DB 컬럼 | snake_case | `birth_date` |
| API 경로 | kebab-case | `/project-members` |

### 3-6. 금지 사항

- `System.out.println()` 사용 금지 → SLF4J Logger 사용
- `@Autowired` 필드 주입 금지 → 생성자 주입 사용
- 도메인 간 Entity 직접 참조 금지 → ID 참조 또는 Service 경유
- 하드코딩된 설정값 금지 → `application.yml` 또는 `@ConfigurationProperties`

---

## 4. 패키지 구조 원칙

```
com.teamflow/
├── domain/          ← 도메인 기준 분리 (핵심 원칙)
│   ├── {domain}/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── dto/
├── api/             ← 사용자 API 컨트롤러
├── common/          ← 공통 유틸, 예외, 응답 형식
├── infra/           ← 외부 시스템 연동 (Security 등)
└── config/          ← 설정 클래스
```

- 도메인 간 의존 방향: `api → service → repository → entity`
- 역방향 의존 절대 금지

---

## 5. 관련 문서

| 문서 | 설명 |
|------|------|
| `docs/arch/BACKEND_REQUIREMENTS.md` | 모놀리식 내부 모듈 구조 및 레이어 규칙 |
| `docs/api/API_REQUIREMENTS.md` | 엔드포인트·요청/응답·에러 형식 |
| `docs/db/ERD_REQUIREMENTS.md` | 엔티티 관계와 제약조건 |
| `docs/frontend/front.md` | 프론트엔드 페이지별 명세 |
