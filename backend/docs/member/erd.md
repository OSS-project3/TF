# Member 도메인 — ERD

의존 도메인: 없음 (최상위 독립 도메인)

---

## 엔티티

### Member

테이블명: `member`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 내부 식별자 |
| `name` | `VARCHAR(50)` | NOT NULL | 이름 (예: "김민서") |
| `role` | `VARCHAR(20)` | NOT NULL | `PM` \| `FRONTEND` \| `BACKEND` \| `DESIGNER` \| `QA` |
| `initial` | `VARCHAR(5)` | NOT NULL | 아바타 이니셜 (예: "민") |
| `weekly_capacity_hours` | `INT` | NOT NULL | 주간 가용 시간 (예: 35) |
| `email` | `VARCHAR(100)` | NOT NULL, UNIQUE | 로그인 이메일 |
| `password` | `VARCHAR(255)` | NOT NULL | BCrypt 해시 |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |

---

### MemberSkill

테이블명: `member_skill`

API 응답에서 `skills: string[]`로 노출. 스킬은 멤버별 독립 소유 — 공유 테이블 없음.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `member_id` | `BIGINT` | NOT NULL, FK → `member.id` | 소유 멤버 |
| `skill` | `VARCHAR(50)` | NOT NULL | 스킬명 (예: "React", "기획") |

---

## 관계

```
member (1) ──< member_skill (N)
               member_skill.member_id → member.id
```

---

## Spring 엔티티 매핑

```
com.teamflow.member.domain
  ├── Member.java       ← BaseTimeEntity 상속
  └── MemberSkill.java  ← BaseTimeEntity 상속 불필요

com.teamflow.member.repository
  ├── MemberRepository.java
  └── MemberSkillRepository.java
```

Member 엔티티 스켈레톤:

```java
@Entity @Table(name = "member")
public class Member extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Column(nullable = false, length = 5)
    private String initial;

    @Column(nullable = false)
    private int weeklyCapacityHours;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberSkill> skills = new ArrayList<>();
}
```
