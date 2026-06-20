package com.example.teamflow.config;

import com.example.teamflow.common.enums.MemberRole;
import com.example.teamflow.common.enums.TaskDifficulty;
import com.example.teamflow.common.enums.TaskStatus;
import com.example.teamflow.domain.invitation.service.MemberInviteService;
import com.example.teamflow.domain.member.entity.Member;
import com.example.teamflow.domain.member.repository.MemberRepository;
import com.example.teamflow.domain.project.dto.ProjectCreateRequest;
import com.example.teamflow.domain.project.service.ProjectService;
import com.example.teamflow.domain.task.dto.TaskCreateRequest;
import com.example.teamflow.domain.task.service.TaskService;
import com.example.teamflow.domain.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 로컬/데모 환경(H2 인메모리)용 초기 데이터 시더.
 * - 로그인 화면의 데모 계정(admin@teamflow.ai / demo@teamflow.ai)이 실제로 동작하도록 멤버를 생성한다.
 * - 데모 프로젝트/태스크를 만들어 빈 화면이 아니도록 한다.
 * - 새로 추가된 "이메일 초대(참가 요청)" 기능도 바로 체험할 수 있게 PENDING 초대 1건을 만든다.
 *
 * 워크스페이스 격리 모델에 맞춰, 데모 멤버를 모두 같은 워크스페이스에 직접 생성한다.
 * (AuthService.register 는 멤버마다 새 워크스페이스를 만들기 때문에 여기서는 사용하지 않는다.)
 * 운영(PostgreSQL 등)에서는 local/docker 프로파일이 아니므로 실행되지 않는다.
 */
@Component
@Profile({"local", "docker"})
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final WorkspaceService workspaceService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final MemberInviteService memberInviteService;

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) return; // 이미 시드됨

        log.info("[DataSeeder] 데모 데이터 시드 시작");

        // ── 데모 워크스페이스 + 멤버 (로그인 화면 데모 계정과 일치) ──
        Long demoWs = workspaceService.create("데모 워크스페이스", null).getId();
        Long admin   = member("관리자", "admin@teamflow.ai",   "Admin!Teamflow2026", MemberRole.PM,       "관", 40, List.of("기획", "관리"),      demoWs);
        Long minjun  = member("김민준", "demo@teamflow.ai",    "Demo!Teamflow2026",  MemberRole.FRONTEND, "김", 40, List.of("React", "TypeScript"), demoWs);
        Long seoyeon = member("이서연", "seoyeon@teamflow.ai", "Member!Teamflow2026",  MemberRole.BACKEND,  "이", 40, List.of("Java", "Spring"),     demoWs);
        Long jiho    = member("박지호", "jiho@teamflow.ai",    "Member!Teamflow2026",  MemberRole.DESIGNER, "박", 35, List.of("Figma", "UI"),        demoWs);
        Long sua     = member("최수아", "sua@teamflow.ai",     "Member!Teamflow2026",  MemberRole.QA,       "최", 30, List.of("QA", "테스트"),       demoWs);

        // ── 프로젝트/태스크 (PM 권한 + 데모 워크스페이스 컨텍스트에서 생성) ──
        runAsWorkspace(admin, demoWs, () -> {
            Long p1 = projectService.createProject(new ProjectCreateRequest(
                    "쇼핑몰 웹 개발",
                    "반응형 이커머스 쇼핑몰 웹사이트 개발 및 배포",
                    LocalDate.now().plusWeeks(6),
                    List.of(admin, minjun, seoyeon, jiho)
            )).id();
            task(p1, "UI/UX 와이어프레임 설계", "기획", 16, TaskDifficulty.MEDIUM, jiho,   -20, -15, TaskStatus.DONE);
            task(p1, "메인 페이지 개발",        "개발", 24, TaskDifficulty.MEDIUM, minjun, -14,  -7, TaskStatus.DONE);
            task(p1, "상품 목록 페이지",         "개발", 20, TaskDifficulty.MEDIUM, minjun,  -6,   0, TaskStatus.IN_PROGRESS);
            task(p1, "회원 인증 API",            "개발", 24, TaskDifficulty.HARD,   seoyeon,-14,  -7, TaskStatus.DONE);
            task(p1, "결제 시스템 연동",          "개발", 32, TaskDifficulty.HARD,   seoyeon, -3,   7, TaskStatus.IN_PROGRESS);
            task(p1, "장바구니 기능",            "개발", 12, TaskDifficulty.EASY,   minjun,   3,   9, TaskStatus.TODO);
            task(p1, "관리자 대시보드",          "개발", 24, TaskDifficulty.HARD,   seoyeon,  8,  15, TaskStatus.TODO);

            Long p2 = projectService.createProject(new ProjectCreateRequest(
                    "AI 챗봇 서비스",
                    "고객 지원 자동화를 위한 AI 챗봇 플랫폼 개발",
                    LocalDate.now().plusWeeks(10),
                    List.of(admin, sua, seoyeon, minjun)
            )).id();
            task(p2, "LLM 모델 선정 및 테스트", "기획", 16, TaskDifficulty.HARD,   sua,    -10, -5, TaskStatus.DONE);
            task(p2, "프롬프트 엔지니어링",      "개발", 24, TaskDifficulty.HARD,   sua,     -4,  5, TaskStatus.IN_PROGRESS);
            task(p2, "챗봇 UI 개발",            "개발", 20, TaskDifficulty.MEDIUM, minjun,   3, 10, TaskStatus.TODO);
            task(p2, "API 서버 구축",           "개발", 24, TaskDifficulty.MEDIUM, seoyeon, -8,  0, TaskStatus.DONE);
        });

        // ── 초대 기능 데모: 별도 워크스페이스의 '신입사원'에게 데모 워크스페이스 참가 요청을 보낸 상태 ──
        // newbie@teamflow.ai 로 로그인 → 팀 탭에서 "받은 참가 요청" 수락 흐름을 바로 체험 가능.
        Long newbieWs = workspaceService.create("신입사원의 워크스페이스", null).getId();
        member("신입사원", "newbie@teamflow.ai", "Member!Teamflow2026", MemberRole.FRONTEND, "신", 40, List.of("React"), newbieWs);
        memberInviteService.invite(demoWs, admin, "newbie@teamflow.ai");

        log.info("[DataSeeder] 데모 데이터 시드 완료 (멤버 6, 프로젝트 2, 초대 1)");
    }

    private Long member(String name, String email, String password, MemberRole role,
                        String initial, int capacity, List<String> skills, Long workspaceId) {
        Member m = Member.create(name, role, initial, capacity, email,
                passwordEncoder.encode(password), workspaceId);
        skills.forEach(m::addSkill);
        return memberRepository.save(m).getId();
    }

    private void task(Long projectId, String title, String phase, int hours, TaskDifficulty difficulty,
                      Long assigneeId, int startOffsetDays, int endOffsetDays, TaskStatus status) {
        LocalDate base = LocalDate.now();
        Long taskId = taskService.createTask(projectId, new TaskCreateRequest(
                title, phase, hours, difficulty, assigneeId,
                base.plusDays(startOffsetDays), base.plusDays(endOffsetDays), null, null
        )).id();
        if (status != TaskStatus.TODO) {
            taskService.changeStatus(taskId, status, assigneeId);
        }
    }

    /** 시더에서 워크스페이스 격리 서비스(createProject 등)를 호출하기 위해 임시 보안 컨텍스트를 설정한다. */
    private void runAsWorkspace(Long memberId, Long workspaceId, Runnable action) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_PM")));
        auth.setDetails(workspaceId);
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
