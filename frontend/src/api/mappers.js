// 백엔드(enum·평탄 구조) <-> 프론트엔드(한글 라벨·중첩 구조) 변환 헬퍼.
// 백엔드 모델과 기존 UI가 쓰는 모델이 다르므로, 페이지에서 이 변환기를 거쳐 사용한다.

// ── 역할 (MemberRole) ────────────────────────────────────────────────
// 백엔드 enum: PM | FRONTEND | BACKEND | DESIGNER | QA
const ROLE_TO_BACKEND = {
  PM: 'PM',
  Frontend: 'FRONTEND',
  Backend: 'BACKEND',
  Designer: 'DESIGNER',
  QA: 'QA',
  // 백엔드에 없는 역할은 가장 가까운 값으로 매핑
  'AI/ML': 'BACKEND',
  DevOps: 'BACKEND',
  기타: 'QA',
}
const ROLE_TO_DISPLAY = {
  PM: 'PM',
  FRONTEND: 'Frontend',
  BACKEND: 'Backend',
  DESIGNER: 'Designer',
  QA: 'QA',
}
export const toBackendRole = (role) => ROLE_TO_BACKEND[role] ?? 'QA'
export const toDisplayRole = (role) => ROLE_TO_DISPLAY[role] ?? role

// ── 태스크 상태 (TaskStatus) ─────────────────────────────────────────
// 백엔드 enum: TODO | IN_PROGRESS | DONE | BLOCKED
const STATUS_TO_BACKEND = {
  대기중: 'TODO',
  진행중: 'IN_PROGRESS',
  완료: 'DONE',
  차단됨: 'BLOCKED',
}
const STATUS_TO_DISPLAY = {
  TODO: '대기중',
  IN_PROGRESS: '진행중',
  DONE: '완료',
  BLOCKED: '차단됨',
}
export const toBackendStatus = (status) => STATUS_TO_BACKEND[status] ?? 'TODO'
export const toDisplayStatus = (status) => STATUS_TO_DISPLAY[status] ?? '대기중'

// ── 난이도 (TaskDifficulty) ──────────────────────────────────────────
// 백엔드 enum: EASY | MEDIUM | HARD
const DIFF_TO_BACKEND = { 낮음: 'EASY', 중: 'MEDIUM', 높음: 'HARD' }
const DIFF_TO_DISPLAY = { EASY: '낮음', MEDIUM: '중', HARD: '높음' }
export const toBackendDifficulty = (d) => DIFF_TO_BACKEND[d] ?? 'MEDIUM'
export const toDisplayDifficulty = (d) => DIFF_TO_DISPLAY[d] ?? '중'

// ── 프로젝트 상태 (ProjectStatus) ────────────────────────────────────
// 백엔드 enum: ACTIVE | ARCHIVED
export const toDisplayProjectStatus = (status) =>
  status === 'ARCHIVED' ? '완료' : '진행중'

// 백엔드에 우선순위 개념이 없어 난이도/특성 플래그로 유도
const priorityFromTask = (task) => {
  if (task.isCriticalPath || task.isLateRisk) return '높음'
  if (task.difficulty === 'HARD') return '높음'
  if (task.difficulty === 'EASY') return '낮음'
  return '중간'
}

/**
 * 백엔드 TaskResponse -> 기존 UI가 쓰는 태스크 형태.
 * @param {object} task   TaskResponse
 * @param {Map<number, object>} [memberMap] memberId -> Member (담당자 이름 해석용)
 */
export const mapTask = (task, memberMap) => {
  const assignee = task.assigneeId != null ? memberMap?.get(task.assigneeId) : null
  return {
    id: task.id,
    projectId: task.projectId,
    title: task.title,
    assignee: assignee?.name ?? '미배정',
    assigneeId: task.assigneeId ?? null,
    status: toDisplayStatus(task.status),
    priority: priorityFromTask(task),
    category: task.phase,
    phase: task.phase,
    startDate: task.startDate ?? '',
    endDate: task.endDate ?? '',
    difficulty: toDisplayDifficulty(task.difficulty),
    estimatedHours: task.estimatedHours,
    isCriticalPath: task.isCriticalPath,
    isLateRisk: task.isLateRisk,
  }
}

/**
 * 백엔드 MemberResponse -> 기존 UI가 쓰는 멤버 형태.
 */
export const mapMember = (m) => ({
  id: m.id,
  name: m.name,
  role: toDisplayRole(m.role),
  avatar: m.initial || m.name?.[0] || '?',
  initial: m.initial,
  weeklyCapacityHours: m.weeklyCapacityHours,
  skills: m.skills ?? [],
})

/**
 * 백엔드 ProjectResponse -> 기존 UI(Home 카드 등)가 쓰는 프로젝트 형태.
 * @param {object} p          ProjectResponse
 * @param {Map<number, object>} [memberMap] memberId -> mapped Member
 */
export const mapProject = (p, memberMap) => ({
  id: p.id,
  title: p.name,
  goal: p.goal,
  deadline: p.deadline,
  status: toDisplayProjectStatus(p.status),
  progress: Math.round((p.progress ?? 0) * 100),
  members: (p.memberIds ?? [])
    .map((id) => memberMap?.get(id))
    .filter(Boolean),
  memberIds: p.memberIds ?? [],
  taskCount: p.taskCount ?? 0,
  doneTaskCount: p.doneTaskCount ?? 0,
  lateTaskCount: p.lateTaskCount ?? 0,
  health: p.health,
})

/** memberId -> mapped Member 맵을 만든다. */
export const buildMemberMap = (members) => {
  const map = new Map()
  members.forEach((m) => map.set(m.id, m))
  return map
}
