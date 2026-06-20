# TeamFlow Frontend

AI 팀 협업 도구 **TeamFlow**의 프론트엔드. React + Vite SPA이며 Spring Boot 백엔드(`/api/v1`)와 연동됩니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| Framework | React 18 |
| Build | Vite 5 |
| Router | React Router 6 |
| 상태/인증 | Context API (`AuthContext`) + JWT(localStorage) |
| 스타일 | 순수 CSS (CSS 변수 디자인 토큰) |

## 실행

### 로컬 개발
```bash
npm install
npm run dev        # http://localhost:5173
```
개발 서버는 `vite.config.js`의 proxy로 `/api` 요청을 `http://localhost:8080`(백엔드)로 전달합니다.
따라서 **백엔드를 먼저 실행**해야 로그인 등 API가 동작합니다.

### 프로덕션 빌드
```bash
npm run build      # dist/ 생성
npm run preview    # 빌드 결과 미리보기
```

### Docker (프론트+백엔드 한 번에)
루트의 `docker-compose.yml` 사용 — 자세한 내용은 [루트 README](../README.md) 참고.
```bash
# 프로젝트 루트(TeamFlow)에서
docker compose up --build
# 프론트: http://localhost:3000
```

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `VITE_API_BASE_URL` | `/api/v1` | API 베이스 URL. 미설정 시 상대경로(`/api/v1`)를 사용해 dev proxy/nginx 프록시를 탑니다. 백엔드를 다른 호스트로 직접 호출하려면 절대 URL 지정. |

## 디렉토리 구조

```
src/
├── api/                 # 백엔드 API 레이어
│   ├── client.js        # fetch 래퍼 (JWT 헤더, 응답 봉투/에러 처리)
│   ├── auth.js          # 로그인/회원가입/로그아웃
│   ├── members.js       # 내 정보·멤버·워크로드
│   ├── projects.js      # 프로젝트 CRUD·멤버 관리
│   ├── tasks.js         # 태스크 CRUD·상태/담당자 변경·내 작업
│   ├── meetings.js      # 회의록 목록·상세·저장
│   ├── dashboard.js     # PM/멤버 대시보드
│   ├── ai.js            # AI 회의 요약·태스크 분해
│   ├── invitations.js   # 초대 링크 생성·수락
│   ├── adapt.js         # 백엔드 응답 → 화면 모델 보정
│   ├── mappers.js       # 백엔드 enum ↔ 한글 라벨 변환
│   └── index.js         # 통합 export
├── components/          # Sidebar, Layout, Protected/AdminRoute
├── context/AuthContext.jsx
├── pages/               # 화면 (아래 표 참고)
└── data/mockData.js     # AI 템플릿 등 백엔드 미구현 영역용 목 데이터
```

## API 레이어 사용

```js
import { authApi, projectApi, taskApi, ApiError } from './api'

await authApi.login({ email, password })          // 토큰 자동 저장
const projects = await projectApi.getProjects()   // Project[]
await taskApi.changeTaskStatus(10, 'IN_PROGRESS')

try {
  await projectApi.createProject({ name, goal, deadline, memberIds: [1, 2] })
} catch (e) {
  if (e instanceof ApiError && e.code === 'FORBIDDEN') alert('PM만 가능합니다')
}
```

- 응답 봉투(`{ data, message }`)는 자동 해제되어 `data`를 반환합니다. 목록 엔드포인트는 `.items` 배열을 바로 반환합니다.
- 실패 시 `ApiError`(`status`, `code`, `message`)를 throw 합니다. `code`는 백엔드 `ErrorCode`와 동일합니다.
- 백엔드 enum과 화면 라벨 차이는 `mappers.js`가 흡수합니다 (역할/상태/난이도, 진행률 0~1↔0~100 등).

## 페이지

| 경로 | 화면 | 연동 API |
|------|------|----------|
| `/login`, `/signup` | 로그인 / 회원가입 | `auth/*`, `me` |
| `/` | 프로젝트 목록 | `projects`, `members` |
| `/dashboard` | 내/PM 대시보드 | `dashboard/member`·`dashboard/pm`·`team/workloads` |
| `/dashboard/:id` | 프로젝트 상세(작업·일정) | `projects/{id}`·`tasks` |
| `/create` | 새 프로젝트 | `projects`·`tasks`(생성) |
| `/meeting` | 회의록 AI(생성·저장) | `meetings`(저장) |
| `/meetings` | 저장된 회의록 목록 | `meetings`(조회) |
| `/settings` | 프로필·비밀번호·탈퇴 | `me`(PATCH/DELETE), `me/password` |
| `/admin` | 관리자(PM 전용) | `members` |

## 백엔드 연동 메모

- 인증은 JWT. 로그인/회원가입 성공 시 토큰을 `localStorage('tf_token')`에 저장하고 모든 요청에 `Authorization: Bearer` 헤더를 자동 첨부합니다.
- **PM 역할 = 관리자**로 취급합니다(백엔드에 별도 관리자 플래그 없음).
- AI 작업 분해와 회의 요약은 백엔드의 `/api/v1/ai/decompositions`, `/api/v1/ai/meeting-summaries`와 연동됩니다. OpenAI 키가 없거나 AI 호출이 실패하면 일부 생성 흐름은 클라이언트 템플릿 결과로 폴백합니다.
- 회원가입 또는 로그인 URL에 `?token=...`이 있으면 초대 워크스페이스 합류 플로우가 동작합니다.
- 다음은 백엔드 미지원이라 화면에서 제한적으로만 동작합니다:
  - 관리자 사용자 관리(역할변경·활성화·삭제) — 해당 API 없음, 목록만 실제 조회
  - 작업 우선순위/카테고리 등 일부 필드 — 백엔드 모델에 없음
