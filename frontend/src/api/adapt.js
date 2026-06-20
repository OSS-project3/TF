// 백엔드 API 응답 → 참조 UI 컴포넌트가 기대하는 형태로 변환.
// 참조 UI는 문자열 id, 소문자 enum(health/difficulty), {init,hours} 필드를 사용한다.
import { toDisplayRole } from './mappers.js'

const HEALTH = { OK: 'ok', WARN: 'warn', BAD: 'bad', IDLE: 'idle' }
const DIFF = { EASY: 'easy', MEDIUM: 'medium', HARD: 'hard' }

/** MemberResponse → 참조 Member */
export const adaptMember = (m) => ({
  id: String(m.id),
  name: m.name,
  role: toDisplayRole(m.role),
  init: m.initial || m.name?.[0] || '?',
  hours: m.weeklyCapacityHours ?? 40,
  skills: m.skills ?? [],
})

/** ProjectResponse → 참조 Project */
export const adaptProject = (p) => ({
  id: String(p.id),
  name: p.name,
  goal: p.goal,
  deadline: p.deadline,
  progress: p.progress ?? 0,
  members: (p.memberIds ?? []).map(String),
  tasks: p.taskCount ?? 0,
  done: p.doneTaskCount ?? 0,
  late: p.lateTaskCount ?? 0,
  health: HEALTH[p.health] ?? 'idle',
})

/** TaskResponse → 참조 Task (+ 실제 백엔드 필드 보존) */
export const adaptTask = (t) => ({
  id: String(t.id),
  projectId: String(t.projectId),
  phase: t.phase,
  title: t.title,
  hours: t.estimatedHours ?? 0,
  difficulty: DIFF[t.difficulty] ?? 'medium',
  deps: (t.dependencyTaskIds ?? []).map(String),
  // 실데이터 연동용 추가 필드
  status: t.status,                              // TODO|IN_PROGRESS|DONE|BLOCKED
  assigneeId: t.assigneeId != null ? String(t.assigneeId) : null,
  startDate: t.startDate ?? null,
  endDate: t.endDate ?? null,
  critical: !!t.isCriticalPath,
  lateRisk: !!t.isLateRisk,
  gitBranch: t.gitBranch ?? null,
})

export const indexBy = (arr, key = 'id') => {
  const m = new Map()
  arr.forEach((x) => m.set(x[key], x))
  return m
}
