# Task 도메인 — ERD

의존 도메인: [project](../project/erd.md), [member](../member/erd.md)

---

## 엔티티

### Task

테이블명: `task`

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `project_id` | `BIGINT` | NOT NULL, FK → `project.id` | 소속 프로젝트 |
| `assignee_id` | `BIGINT` | NULL, FK → `member.id` | 담당자 (미배정 시 NULL) |
| `title` | `VARCHAR(200)` | NOT NULL | 작업 제목 |
| `phase` | `VARCHAR(20)` | NOT NULL | `리서치` \| `설계` \| `디자인` \| `개발` \| `QA` |
| `estimated_hours` | `INT` | NOT NULL | 예상 소요 시간 |
| `difficulty` | `VARCHAR(10)` | NOT NULL | `EASY` \| `MEDIUM` \| `HARD` |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'TODO'` | `TODO` \| `IN_PROGRESS` \| `DONE` \| `BLOCKED` |
| `start_date` | `DATE` | NULL | 시작일 |
| `end_date` | `DATE` | NULL | 종료일 |
| `is_critical_path` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | 크리티컬 패스 여부 |
| `is_late_risk` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | 지연 위험 여부 |
| `created_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |
| `updated_at` | `DATETIME` | NOT NULL, DEFAULT NOW() | |

---

### TaskDependency

테이블명: `task_dependency`

Task 간 선행 관계 N:N 중간 테이블. API 응답에서 `dependencyTaskIds: string[]`로 노출.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `task_id` | `BIGINT` | NOT NULL, FK → `task.id` | 현재 작업 |
| `prerequisite_task_id` | `BIGINT` | NOT NULL, FK → `task.id` | 선행 작업 (완료돼야 task_id 시작 가능) |

UNIQUE(`task_id`, `prerequisite_task_id`)  
순환 의존 방지는 서비스 레이어에서 처리 (DB 제약 없음).

---

## 관계

```
project (1) ──< task (N)
                task.project_id → project.id

member (1) ──< task (N)   [assignee]
               task.assignee_id → member.id (nullable)

task (1) ──< task_dependency (N) >── task (1)   [선행 관계]
             task_dependency.task_id → task.id
             task_dependency.prerequisite_task_id → task.id
```

---

## Spring 엔티티 매핑

```
com.teamflow.task.domain
  ├── Task.java            ← BaseTimeEntity 상속
  └── TaskDependency.java

com.teamflow.task.repository
  ├── TaskRepository.java
  └── TaskDependencyRepository.java
```

Task 엔티티 스켈레톤:

```java
@Entity @Table(name = "task")
public class Task extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    private Long assigneeId;  // nullable

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskDifficulty difficulty;

    // 상태 변경 메서드 (Setter 공개 금지)
    public void changeStatus(TaskStatus newStatus) {
        this.status = newStatus;
    }

    public void assignTo(Long memberId) {
        this.assigneeId = memberId;
    }
}
```

> Task는 `projectId`, `assigneeId`를 **ID 참조**로만 보유. 연관 엔티티 직접 참조(`@ManyToOne`) 사용 시 도메인 간 의존이 생기므로 ID 참조 방식 권장.
