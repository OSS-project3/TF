package com.example.teamflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TeamFlow API")
                        .version("v1")
                        .description("""
                                TeamFlow 팀 프로젝트 관리 도구 백엔드 API 명세.

                                ## 인증
                                - `POST /api/v1/auth/login` 으로 accessToken 발급
                                - 이후 모든 요청에 `Authorization: Bearer {accessToken}` 헤더 필수
                                - 우측 상단 **Authorize** 버튼에 토큰 입력 후 테스트 가능

                                ## 역할 (MemberRole)
                                | 값 | 설명 |
                                |---|---|
                                | PM | 프로젝트 관리자 (프로젝트 생성/수정/삭제 권한) |
                                | FRONTEND | 프론트엔드 개발자 |
                                | BACKEND | 백엔드 개발자 |
                                | DESIGNER | 디자이너 |
                                | QA | QA 엔지니어 |

                                ## 공통 응답 형식
                                ```json
                                // 성공
                                { "data": { ... }, "message": "ok" }
                                // 에러
                                { "error": { "code": "PROJECT_NOT_FOUND", "message": "프로젝트를 찾을 수 없습니다." } }
                                ```

                                ## 공통 에러 코드
                                | HTTP | code | 발생 상황 |
                                |------|------|-----------|
                                | 400 | `INVALID_INPUT` | 요청 필드 유효성 검사 실패 |
                                | 401 | `INVALID_CREDENTIALS` | 이메일/비밀번호 불일치 |
                                | 401 | `WRONG_PASSWORD` | 비밀번호 변경 시 현재 비밀번호 불일치 |
                                | 401 | `GOOGLE_AUTH_INVALID` | Google ID 토큰 검증 실패 또는 clientId 불일치 |
                                | 401 | `INVITE_INVALID` | 유효하지 않은 초대 토큰 |
                                | 401 | _(토큰 없음/만료)_ | Authorization 헤더 누락 또는 JWT 만료 |
                                | 403 | `FORBIDDEN` | PM 역할이 아닌 멤버가 PM 전용 API 호출 |
                                | 404 | `MEMBER_NOT_FOUND` | 존재하지 않는 멤버 ID |
                                | 404 | `PROJECT_NOT_FOUND` | 존재하지 않는 프로젝트 ID |
                                | 404 | `TASK_NOT_FOUND` | 존재하지 않는 태스크 ID |
                                | 404 | `MEETING_NOT_FOUND` | 존재하지 않는 회의록 ID |
                                | 404 | `WORKSPACE_NOT_FOUND` | 존재하지 않는 워크스페이스 ID |
                                | 409 | `DUPLICATE_EMAIL` | 이미 사용 중인 이메일로 회원가입 시도 |
                                | 409 | `DUPLICATE_PROJECT_MEMBER` | 이미 프로젝트에 소속된 멤버 추가 시도 |
                                | 410 | `INVITE_EXPIRED` | 만료된 초대 토큰 (7일 초과) |
                                | 410 | `INVITE_USED` | 이미 사용된 초대 토큰 |
                                | 422 | `CIRCULAR_TASK_DEPENDENCY` | 태스크 선행 관계 순환 감지 |
                                | 500 | `INTERNAL_ERROR` | 서버 내부 오류 |
                                | 503 | `GOOGLE_AUTH_DISABLED` | 서버에 GOOGLE_CLIENT_ID 미설정 (Google 로그인 비활성) |
                                """)
                        .contact(new Contact().name("TeamFlow Backend")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("로그인 후 발급된 JWT access token")));
    }
}
