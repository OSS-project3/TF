# TeamFlow

AI가 팀 프로젝트의 일정·업무 분담·회의 정리를 돕는 팀 협업 도구.
React 프론트엔드와 Spring Boot 백엔드로 구성된 모노레포입니다.

## 구성

```
AutoFlow/
├── frontend/           # React + Vite SPA   (자세히: frontend/README.md)
├── backend/            # Spring Boot + JPA   (자세히: backend/CLAUDE.md)
└── docker-compose.yml  # 프론트 + 백엔드 한 번에 실행
```

## 빠른 시작 — Docker (권장)

도커만 있으면 한 번에 실행됩니다.

```bash
# (선택) AI 기능을 쓰려면 .env에 OpenAI 키 입력
#   .env 파일이 이미 있습니다. OPENAI_API_KEY=sk-... 채우면 됩니다.
docker compose up --build
```

| 서비스 | 주소 |
|--------|------|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

종료:
```bash
docker compose down
```

### 데모 계정 (시드 데이터)

로컬/도커 실행 시 데모 멤버·프로젝트가 자동 생성됩니다. 로그인 화면 하단의 데모 버튼으로 바로 접속할 수 있습니다.

| 계정 | 이메일 | 비밀번호 | 역할 |
|------|--------|----------|------|
| 관리자 | `admin@teamflow.ai` | `admin1234` | PM (관리자) |
| 일반 사용자 | `demo@teamflow.ai` | `demo1234` | Frontend |

> H2 인메모리라 `docker compose down` 시 초기화되고, 다시 올리면 데모 데이터가 재생성됩니다.

### AI 기능 (OpenAI)

`.env`의 `OPENAI_API_KEY`를 채우면 **AI 작업 분해**(프로젝트 생성)와 **AI 회의 요약**(회의록 AI)이 실제 OpenAI로 동작합니다.
키가 없으면 백엔드는 `AI_DISABLED`를 반환하고, 프론트는 자동으로 템플릿 결과로 폴백하여 앱은 그대로 동작합니다.

```bash
# .env
OPENAI_API_KEY=sk-여기에_키
OPENAI_MODEL=gpt-4o-mini   # 선택
```
키를 바꾼 뒤에는 백엔드만 재시작: `docker compose up -d --build backend`

> 프론트(nginx)가 `/api` 요청을 백엔드 컨테이너로 프록시하므로 CORS 설정 없이 동작합니다.
> DB는 **임베디드 H2(인메모리)** 라 컨테이너를 내리면 데이터가 초기화됩니다. 첫 실행 시
> 화면에서 **회원가입(역할 PM)** 으로 계정을 만들면 관리자/프로젝트 생성 권한을 갖습니다.

## 개별 실행 (도커 없이)

**백엔드** (Java 17)
```bash
cd backend
# gradlew 래퍼가 없으면 로컬 gradle 사용
gradle :backend:bootRun
# → http://localhost:8080  (active profile: local, H2 인메모리)
```

**프론트엔드** (Node 20)
```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173  (/api 요청은 :8080으로 프록시)
```

## 기술 스택

| 영역 | 스택 |
|------|------|
| Frontend | React 18, Vite 5, React Router 6, Context API |
| Backend | Java 17, Spring Boot 3.3, Spring Data JPA, Spring Security(JWT), springdoc(Swagger) |
| DB | H2(로컬·도커) / PostgreSQL(운영 설정) |
| 빌드/배포 | Gradle, Docker, nginx |

## 주요 기능

- **인증** — 회원가입/로그인/로그아웃 (JWT)
- **프로젝트** — 목록·상세·생성, 멤버 관리, 진행률·health 자동 집계
- **태스크** — 보드/일정(간트), 상태·담당자 변경, 내 작업
- **대시보드** — 개인 워크로드·내 작업, PM용 팀 현황·팀 워크로드
- **회의록** — AI 요약(생성) 및 저장된 회의록 목록 조회
- **설정** — 프로필·비밀번호 변경·회원 탈퇴
- **관리자** — 멤버 목록(PM 전용)

## 환경 변수

| 변수 | 위치 | 기본값 | 설명 |
|------|------|--------|------|
| `OPENAI_API_KEY` | backend (.env) | (빈 값) | OpenAI API 키. 채우면 AI 기능 활성화 |
| `OPENAI_MODEL` | backend (.env) | `gpt-4o-mini` | 사용할 OpenAI 모델 |
| `JWT_SECRET` | backend | 내장 기본값 | JWT 서명 키 (운영 시 반드시 교체) |
| `VITE_API_BASE_URL` | frontend | `/api/v1` | API 베이스 URL (미설정 시 프록시 사용) |

## 더 보기

- 프론트엔드 상세: [`frontend/README.md`](frontend/README.md)
- 백엔드 상세/아키텍처: [`backend/CLAUDE.md`](backend/CLAUDE.md)
- API 명세: Swagger UI (`/swagger-ui.html`)
