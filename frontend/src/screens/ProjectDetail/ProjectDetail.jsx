import { useState, useEffect, useMemo } from 'react'
import './ProjectDetail.css'
import Avatar from '../../ui/Avatar/Avatar'
import ProgressBar from '../../ui/ProgressBar/ProgressBar'
import HealthBadge from '../../ui/HealthBadge/HealthBadge'
import Segmented from '../../ui/Segmented/Segmented'
import CheckMark from '../../ui/CheckMark/CheckMark'
import { daysFromNow } from '../../utils/date'
import { taskApi } from '../../api'
import { adaptTask } from '../../api/adapt'
import { toBackendStatus } from '../../api/mappers'

const PHASES = ['기획', '디자인', '개발', '테스트', '배포']
const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD']
const todayStr = () => new Date().toISOString().slice(0, 10)
const maxDateStr = () => new Date(new Date().setFullYear(new Date().getFullYear() + 5)).toISOString().slice(0, 10)

export default function ProjectDetail({ project, members, back, role, currentUser, onUpdate, onArchive }) {
  const isPM = role === 'pm'
  const isMember = role === 'member'

  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [view, setView] = useState('tasks')
  const [editingBranchId, setEditingBranchId] = useState(null)
  const [branchInput, setBranchInput] = useState('')

  // 편집 모달 상태
  const [editOpen, setEditOpen] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState(false)

  // 편집 폼 필드
  const [editName, setEditName] = useState(project.name)
  const [editDeadline, setEditDeadline] = useState(project.deadline)
  const [editMembers, setEditMembers] = useState(project.members.map(String))
  const [editSaving, setEditSaving] = useState(false)
  const [editError, setEditError] = useState('')

  // 새 태스크 추가 폼
  const [addTaskOpen, setAddTaskOpen] = useState(false)
  const [newTask, setNewTask] = useState({ title: '', phase: '개발', startDate: '', endDate: '', difficulty: 'MEDIUM', assigneeId: '', estimatedHours: '' })
  const [addingTask, setAddingTask] = useState(false)

  // 태스크 인라인 편집
  const [editingTask, setEditingTask] = useState(null) // { id, title, phase, startDate, endDate, difficulty, assigneeId, estimatedHours }

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    taskApi.getProjectTasks(Number(project.id))
      .then(list => { if (!cancelled) setTasks(list.map(adaptTask)) })
      .catch(() => { if (!cancelled) setTasks([]) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [project.id])

  const projectMembers = members.filter(m => project.members.includes(m.id))
  const memberById = useMemo(() => new Map(members.map(m => [m.id, m])), [members])

  const isDone = (t) => t.status === 'DONE'

  // ─── Git 브랜치 편집 ───
  function startEditBranch(t) { setEditingBranchId(t.id); setBranchInput(t.gitBranch || '') }
  async function saveBranch(t) {
    const branch = branchInput.trim()
    setTasks(prev => prev.map(x => x.id === t.id ? { ...x, gitBranch: branch || null } : x))
    setEditingBranchId(null)
    try { await taskApi.updateTask(Number(t.id), { gitBranch: branch }) }
    catch { setTasks(prev => prev.map(x => x.id === t.id ? { ...x, gitBranch: t.gitBranch } : x)) }
  }

  // ─── 완료 토글 ───
  async function toggleDone(t) {
    const next = isDone(t) ? 'TODO' : 'DONE'
    setTasks(prev => prev.map(x => x.id === t.id ? { ...x, status: next } : x))
    try { await taskApi.changeTaskStatus(Number(t.id), toBackendStatus(next === 'DONE' ? '완료' : '대기중')) }
    catch { setTasks(prev => prev.map(x => x.id === t.id ? { ...x, status: t.status } : x)) }
  }

  // ─── 태스크 삭제 ───
  async function deleteTask(t) {
    setTasks(prev => prev.filter(x => x.id !== t.id))
    try { await taskApi.deleteTask(Number(t.id)) }
    catch { setTasks(prev => [...prev, t]) }
  }

  // ─── 태스크 추가 ───
  async function addTask() {
    if (!newTask.title.trim()) return
    setAddingTask(true)
    try {
      const created = await taskApi.createTask(Number(project.id), {
        title: newTask.title.trim(),
        phase: newTask.phase,
        startDate: newTask.startDate || null,
        endDate: newTask.endDate || null,
        estimatedHours: Number(newTask.estimatedHours) > 0 ? Number(newTask.estimatedHours) : null,
        difficulty: newTask.difficulty,
        assigneeId: newTask.assigneeId ? Number(newTask.assigneeId) : null,
      })
      const fresh = await taskApi.getProjectTasks(Number(project.id))
      setTasks(fresh.map(adaptTask))
      setAddTaskOpen(false)
      setNewTask({ title: '', phase: '개발', startDate: '', endDate: '', difficulty: 'MEDIUM', assigneeId: '', estimatedHours: '' })
    } catch { /* 무시 */ }
    finally { setAddingTask(false) }
  }

  // ─── 태스크 인라인 저장 ───
  async function saveEditTask() {
    if (!editingTask || !editingTask.title.trim()) return
    const prev = tasks.find(t => t.id === editingTask.id)
    setTasks(ts => ts.map(t => t.id === editingTask.id ? {
      ...t, title: editingTask.title, phase: editingTask.phase,
      startDate: editingTask.startDate || null, endDate: editingTask.endDate || null,
      hours: Number(editingTask.estimatedHours) || 0, difficulty: editingTask.difficulty.toLowerCase(),
      assigneeId: editingTask.assigneeId ? String(editingTask.assigneeId) : null,
    } : t))
    setEditingTask(null)
    try {
      await taskApi.updateTask(Number(editingTask.id), {
        title: editingTask.title, phase: editingTask.phase,
        startDate: editingTask.startDate || null,
        endDate: editingTask.endDate || null,
        estimatedHours: Number(editingTask.estimatedHours) > 0 ? Number(editingTask.estimatedHours) : null,
        difficulty: editingTask.difficulty,
      })
      if (editingTask.assigneeId) {
        await taskApi.changeTaskAssignee(Number(editingTask.id), Number(editingTask.assigneeId))
      }
    } catch {
      if (prev) setTasks(ts => ts.map(t => t.id === prev.id ? prev : t))
    }
  }

  // ─── 프로젝트 편집 저장 ───
  function openEdit() {
    setEditName(project.name)
    setEditDeadline(project.deadline)
    setEditMembers(project.members.map(String))
    setEditError('')
    setEditOpen(true)
  }
  async function saveEdit() {
    setEditSaving(true); setEditError('')
    try {
      await onUpdate(project.id, { name: editName, goal: project.goal, deadline: editDeadline, memberIds: editMembers })
      setEditOpen(false)
    } catch (e) { setEditError(e?.message || '저장에 실패했습니다.') }
    finally { setEditSaving(false) }
  }

  // ─── 프로젝트 삭제(아카이브) ───
  async function confirmDelete() {
    try { await onArchive(project.id) }
    catch { setDeleteConfirm(false) }
  }

  // ─── 뷰 계산 ───
  const tasksByPhase = useMemo(() => {
    const m = {}
    tasks.forEach(t => { (m[t.phase] = m[t.phase] ?? []).push(t) })
    return m
  }, [tasks])

  const loadByMember = useMemo(() => {
    const o = {}
    projectMembers.forEach(m => { o[m.id] = { hours: 0, tasks: 0 } })
    tasks.forEach(t => {
      if (t.assigneeId && o[t.assigneeId]) {
        o[t.assigneeId].hours += t.hours
        o[t.assigneeId].tasks += 1
      }
    })
    return o
  }, [tasks, projectMembers])

  const visibleTasks = isMember ? tasks.filter(t => t.assigneeId === currentUser.id) : tasks

  const toggleEditMember = (id) =>
    setEditMembers(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])

  return (
    <div className="page" data-screen-label="Project Detail">
      {/* ─── 헤더 ─── */}
      <div className="page-head">
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="row" style={{ marginBottom: 6 }}>
            <button className="btn btn-ghost" onClick={back} style={{ padding: '2px 8px', fontSize: 12 }}>← 프로젝트</button>
            <HealthBadge health={project.health} />
            <span className="tiny">D-{daysFromNow(project.deadline)}</span>
          </div>
          <div className="row" style={{ alignItems: 'flex-start', gap: 12 }}>
            <h1 style={{ flex: 1, minWidth: 0 }}>{project.name}</h1>
            {isPM && (
              <div className="row" style={{ gap: 6, flexShrink: 0, marginTop: 4 }}>
                <button className="btn" style={{ fontSize: 12, padding: '4px 10px' }} onClick={openEdit}>수정</button>
                <button className="btn" style={{ fontSize: 12, padding: '4px 10px', color: 'var(--bad)', borderColor: 'var(--bad)' }} onClick={() => setDeleteConfirm(true)}>삭제</button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ─── 본문 ─── */}
      <div style={{ display: 'grid', gridTemplateColumns: isMember ? '1fr' : '1.5fr 1fr', gap: 20 }}>
        <div>
          <div className="row" style={{ marginBottom: 12 }}>
            <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>
              {isMember ? '내 작업' : `작업 (${visibleTasks.length})`}
            </h2>
            <span className="tiny">
              {isMember ? `${visibleTasks.length}건 담당` : `완료 ${tasks.filter(isDone).length}/${tasks.length}`}
            </span>
            <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center' }}>
              {isPM && (
                <button className="btn btn-primary" style={{ fontSize: 11, padding: '3px 8px' }} onClick={() => setAddTaskOpen(true)}>+ 작업 추가</button>
              )}
              <Segmented value={view} onChange={setView} options={[
                { value: 'tasks', label: '리스트' },
                { value: 'phase', label: '단계' },
              ]} />
            </div>
          </div>

          {/* 태스크 추가 폼 */}
          {addTaskOpen && isPM && (
            <div className="card" style={{ marginBottom: 12, padding: 12 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 68px 96px 96px 84px 96px 52px', gap: 6, marginBottom: 6 }}>
                <span className="tiny muted" style={{ fontSize: 10 }}>제목</span>
                <span className="tiny muted" style={{ fontSize: 10 }}>단계</span>
                <span className="tiny muted" style={{ fontSize: 10 }}>시작일</span>
                <span className="tiny muted" style={{ fontSize: 10 }}>마감일</span>
                <span className="tiny muted" style={{ fontSize: 10 }}>난이도</span>
                <span className="tiny muted" style={{ fontSize: 10 }}>담당자</span>
                <span className="tiny muted" style={{ fontSize: 10 }}>시간(선택)</span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 68px 96px 96px 84px 96px 52px', gap: 6, marginBottom: 8 }}>
                <input placeholder="작업 제목" value={newTask.title}
                  onChange={e => setNewTask(p => ({ ...p, title: e.target.value }))}
                  style={{ fontSize: 12, padding: '4px 6px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }}
                  autoFocus onKeyDown={e => e.key === 'Enter' && addTask()} />
                <select value={newTask.phase} onChange={e => setNewTask(p => ({ ...p, phase: e.target.value }))}
                  style={{ fontSize: 11, padding: '4px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }}>
                  {PHASES.map(ph => <option key={ph}>{ph}</option>)}
                </select>
                <input type="date" value={newTask.startDate} onChange={e => setNewTask(p => ({ ...p, startDate: e.target.value }))}
                  style={{ fontSize: 11, padding: '4px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }} />
                <input type="date" value={newTask.endDate} onChange={e => setNewTask(p => ({ ...p, endDate: e.target.value }))}
                  style={{ fontSize: 11, padding: '4px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }} />
                <select value={newTask.difficulty} onChange={e => setNewTask(p => ({ ...p, difficulty: e.target.value }))}
                  style={{ fontSize: 11, padding: '4px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }}>
                  {DIFFICULTIES.map(d => <option key={d}>{d}</option>)}
                </select>
                <select value={newTask.assigneeId} onChange={e => setNewTask(p => ({ ...p, assigneeId: e.target.value }))}
                  style={{ fontSize: 11, padding: '4px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }}>
                  <option value="">미배정</option>
                  {projectMembers.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                </select>
                <input type="number" min={1} value={newTask.estimatedHours} placeholder="-"
                  onChange={e => setNewTask(p => ({ ...p, estimatedHours: e.target.value }))}
                  style={{ fontSize: 11, padding: '4px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)', textAlign: 'right' }} />
              </div>
              <div className="row" style={{ gap: 6 }}>
                <button className="btn btn-primary" style={{ fontSize: 12, padding: '4px 10px' }} onClick={addTask} disabled={addingTask || !newTask.title.trim()}>
                  {addingTask ? '추가 중…' : '추가'}
                </button>
                <button className="btn btn-ghost" style={{ fontSize: 12, padding: '4px 10px' }} onClick={() => setAddTaskOpen(false)}>취소</button>
              </div>
            </div>
          )}

          <div className="card" style={{ padding: 0, borderTop: '1px solid var(--line)', borderBottom: '1px solid var(--line)' }}>
            {loading && <div className="placeholder" style={{ border: 0, padding: 24 }}><div className="mono">LOADING</div><div>작업을 불러오는 중…</div></div>}

            {!loading && view === 'tasks' && visibleTasks.map(t => {
              const assignee = t.assigneeId ? memberById.get(t.assigneeId) : null
              const done = isDone(t)
              const mine = isMember && t.assigneeId === currentUser.id
              const isEditing = editingTask?.id === t.id

              return (
                <div key={t.id} className={'task' + (done ? ' done' : '')} style={{ margin: '0 18px', padding: '10px 0', background: mine && !done ? 'oklch(0.62 0.18 264 / 0.04)' : 'transparent' }}>
                  <button className={'task-check' + (done ? ' done' : '')} disabled={isMember && !mine} onClick={() => toggleDone(t)}>
                    <CheckMark />
                  </button>

                  {isEditing ? (
                    /* 인라인 편집 모드 */
                    <div style={{ flex: 1, display: 'grid', gridTemplateColumns: '1fr 68px 96px 96px 84px 96px 52px', gap: 4, alignItems: 'center' }}>
                      <input value={editingTask.title} onChange={e => setEditingTask(p => ({ ...p, title: e.target.value }))}
                        style={{ fontSize: 12, padding: '3px 5px', border: '1px solid var(--ai)', borderRadius: 4, background: 'var(--surface)' }}
                        autoFocus onKeyDown={e => { if (e.key === 'Enter') saveEditTask(); if (e.key === 'Escape') setEditingTask(null) }} />
                      <select value={editingTask.phase} onChange={e => setEditingTask(p => ({ ...p, phase: e.target.value }))}
                        style={{ fontSize: 11, padding: '3px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }}>
                        {PHASES.map(ph => <option key={ph}>{ph}</option>)}
                      </select>
                      <input type="date" value={editingTask.startDate} onChange={e => setEditingTask(p => ({ ...p, startDate: e.target.value }))}
                        style={{ fontSize: 11, padding: '3px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }} />
                      <input type="date" value={editingTask.endDate} onChange={e => setEditingTask(p => ({ ...p, endDate: e.target.value }))}
                        style={{ fontSize: 11, padding: '3px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }} />
                      <select value={editingTask.difficulty} onChange={e => setEditingTask(p => ({ ...p, difficulty: e.target.value }))}
                        style={{ fontSize: 11, padding: '3px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }}>
                        {DIFFICULTIES.map(d => <option key={d}>{d}</option>)}
                      </select>
                      <select value={editingTask.assigneeId ?? ''} onChange={e => setEditingTask(p => ({ ...p, assigneeId: e.target.value || null }))}
                        style={{ fontSize: 11, padding: '3px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }}>
                        <option value="">미배정</option>
                        {projectMembers.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                      </select>
                      <input type="number" min={1} value={editingTask.estimatedHours} placeholder="-"
                        onChange={e => setEditingTask(p => ({ ...p, estimatedHours: e.target.value }))}
                        style={{ fontSize: 11, padding: '3px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)', textAlign: 'right' }} />
                    </div>
                  ) : (
                    /* 일반 표시 모드 */
                    <div className="task-title" style={{ flex: 1 }}>
                      <span className="badge" style={{ fontSize: 10 }}>{t.phase}</span>
                      <span className="t">{t.title}</span>
                    </div>
                  )}

                  {!isEditing && (
                    <>
                      <span className="task-meta">
                        {t.endDate
                          ? `~${t.endDate.slice(5).replace('-', '/')}`
                          : t.hours > 0 ? `${t.hours}h` : '—'}
                      </span>
                      <span className="task-meta" style={{ color: t.difficulty === 'hard' ? 'var(--bad)' : t.difficulty === 'medium' ? 'var(--warn)' : 'var(--ok)' }}>{t.difficulty}</span>
                      <span className="task-assignee">
                        {assignee ? <><Avatar member={assignee} size={22} /><span style={{ fontSize: 12 }}>{assignee.name}</span></>
                          : <span className="muted mono" style={{ fontSize: 11 }}>미배정</span>}
                      </span>
                    </>
                  )}

                  {/* PM 전용 액션 버튼 */}
                  {isPM && (
                    <span style={{ display: 'flex', alignItems: 'center', gap: 3, marginLeft: 4 }}>
                      {isEditing ? (
                        <>
                          <button className="btn btn-ghost" style={{ padding: '1px 6px', fontSize: 11 }} onClick={saveEditTask}>저장</button>
                          <button className="btn btn-ghost" style={{ padding: '1px 6px', fontSize: 11 }} onClick={() => setEditingTask(null)}>취소</button>
                        </>
                      ) : (
                        <>
                          {editingBranchId === t.id ? (
                            <span style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                              <input className="mono" style={{ fontSize: 11, padding: '2px 5px', border: '1px solid var(--line)', borderRadius: 4, width: 130, background: 'var(--surface)' }}
                                placeholder="feat/branch-name" value={branchInput} autoFocus
                                onChange={e => setBranchInput(e.target.value)}
                                onKeyDown={e => { if (e.key === 'Enter') saveBranch(t); if (e.key === 'Escape') setEditingBranchId(null) }} />
                              <button className="btn btn-ghost" style={{ padding: '1px 5px', fontSize: 11 }} onClick={() => saveBranch(t)}>저장</button>
                              <button className="btn btn-ghost" style={{ padding: '1px 5px', fontSize: 11 }} onClick={() => setEditingBranchId(null)}>취소</button>
                            </span>
                          ) : (
                            <>
                              {t.gitBranch && <span className="mono" style={{ fontSize: 10, color: 'var(--muted)' }}>{t.gitBranch}</span>}
                              <button className="btn btn-ghost" style={{ padding: '1px 5px', fontSize: 10, opacity: 0.55 }} title={t.gitBranch ? '브랜치 수정' : 'GitHub 브랜치 연결'} onClick={() => startEditBranch(t)}>
                                {t.gitBranch ? '✎' : '⎇'}
                              </button>
                              <button className="btn btn-ghost" style={{ padding: '1px 5px', fontSize: 10, opacity: 0.55 }} title="작업 수정"
                                onClick={() => setEditingTask({ id: t.id, title: t.title, phase: t.phase, startDate: t.startDate || '', endDate: t.endDate || '', estimatedHours: t.hours > 0 ? String(t.hours) : '', difficulty: t.difficulty?.toUpperCase() ?? 'MEDIUM', assigneeId: t.assigneeId })}>
                                ✎
                              </button>
                              <button className="btn btn-ghost" style={{ padding: '1px 5px', fontSize: 10, opacity: 0.55, color: 'var(--bad)' }} title="작업 삭제" onClick={() => deleteTask(t)}>×</button>
                            </>
                          )}
                        </>
                      )}
                    </span>
                  )}
                </div>
              )
            })}

            {!loading && view === 'phase' && Object.entries(tasksByPhase).map(([ph, items]) => (
              <div key={ph} style={{ padding: '14px 18px', borderBottom: '1px solid var(--line)' }}>
                <div className="row" style={{ marginBottom: 8 }}>
                  <span className="badge" style={{ fontSize: 11 }}>{ph}</span>
                  <span className="tiny">{items.length}개 · {items.reduce((a, t) => a + t.hours, 0)}h</span>
                </div>
                <div className="col gap-sm">
                  {items.map(t => {
                    const a = t.assigneeId ? memberById.get(t.assigneeId) : null
                    return (
                      <div key={t.id} className="row" style={{ fontSize: 13 }}>
                        <span>{t.title}</span>
                        <span style={{ marginLeft: 'auto', display: 'flex', gap: 6, alignItems: 'center' }}>
                          {a && <Avatar member={a} size={18} />}
                          <span className="mono muted" style={{ fontSize: 11 }}>
                            {t.endDate ? `~${t.endDate.slice(5).replace('-', '/')}` : t.hours > 0 ? `${t.hours}h` : '—'}
                          </span>
                        </span>
                      </div>
                    )
                  })}
                </div>
              </div>
            ))}

            {!loading && visibleTasks.length === 0 && (
              <div className="placeholder" style={{ border: 0, padding: 24 }}>
                <div className="mono">EMPTY</div>
                <div>{isMember ? '담당 작업이 없습니다.' : '작업이 없습니다.'}</div>
              </div>
            )}
          </div>
        </div>

        {!isMember && (
          <div>
            <div className="row" style={{ marginBottom: 12 }}>
              <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>팀 부하</h2>
              <span className="tiny">프로젝트 기준</span>
            </div>
            <div className="col gap-lg">
              {projectMembers.map(m => {
                const load = loadByMember[m.id] ?? { hours: 0, tasks: 0 }
                const pct = Math.min(1, load.hours / (m.hours || 1))
                return (
                  <div key={m.id} className="member-card">
                    <div className="member-head">
                      <Avatar member={m} size={36} />
                      <div>
                        <div className="member-name">{m.name}</div>
                        <div className="member-role">{m.role}</div>
                      </div>
                    </div>
                    <div className="member-load">
                      <span>{load.hours}h / {m.hours}h</span>
                      <ProgressBar value={pct} kind={pct > 0.9 ? 'warn' : ''} />
                      <span style={{ minWidth: 30, textAlign: 'right' }}>{Math.round(pct * 100)}%</span>
                    </div>
                    <div className="member-tasks">
                      {load.tasks > 0
                        ? <span className="muted" style={{ fontSize: 12 }}>{load.tasks}개 작업 담당</span>
                        : <span className="muted mono" style={{ fontSize: 11 }}>EMPTY</span>}
                    </div>
                  </div>
                )
              })}
              {projectMembers.length === 0 && <div className="placeholder" style={{ border: 0 }}><div className="mono">EMPTY</div><div>팀원이 없습니다.</div></div>}
            </div>
          </div>
        )}
      </div>

      {/* ─── 프로젝트 수정 모달 ─── */}
      {editOpen && (
        <div className="modal-bg" onClick={() => setEditOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-head"><h2>프로젝트 수정</h2></div>
            <div className="modal-body">
              <div className="field">
                <label>프로젝트명</label>
                <input value={editName} onChange={e => setEditName(e.target.value)} />
              </div>
              <div className="field">
                <label>마감일</label>
                <input type="date" value={editDeadline} min={todayStr()} max={maxDateStr()} onChange={e => setEditDeadline(e.target.value)} />
              </div>
              <div className="field">
                <label>팀원</label>
                <div className="chips" style={{ marginTop: 4 }}>
                  {members.map(m => (
                    <button key={m.id} className={'chip' + (editMembers.includes(String(m.id)) ? ' active' : '')} onClick={() => toggleEditMember(String(m.id))}>
                      <Avatar member={m} size={18} />
                      {m.name}
                    </button>
                  ))}
                </div>
              </div>
              {editError && <div className="tiny" style={{ color: 'var(--bad)' }}>⚠ {editError}</div>}
            </div>
            <div className="modal-foot">
              <button className="btn btn-ghost" onClick={() => setEditOpen(false)} disabled={editSaving}>취소</button>
              <button className="btn btn-primary" onClick={saveEdit} disabled={editSaving || !editName || !editDeadline}>
                {editSaving ? '저장 중…' : '저장'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── 삭제 확인 모달 ─── */}
      {deleteConfirm && (
        <div className="modal-bg" onClick={() => setDeleteConfirm(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-head">
              <h2>프로젝트를 삭제하시겠습니까?</h2>
              <p>삭제하면 프로젝트와 관련 작업이 보관됩니다. 되돌릴 수 없습니다.</p>
            </div>
            <div className="modal-foot">
              <button className="btn btn-ghost" onClick={() => setDeleteConfirm(false)}>취소</button>
              <button className="btn btn-primary" style={{ background: 'var(--bad)' }} onClick={confirmDelete}>삭제 확인</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
