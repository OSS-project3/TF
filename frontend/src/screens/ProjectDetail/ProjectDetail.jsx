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

export default function ProjectDetail({ project, members, back, role, currentUser }) {
  const isMember = role === 'member'
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [view, setView] = useState('tasks')

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

  async function toggleDone(t) {
    const next = isDone(t) ? 'TODO' : 'DONE'
    setTasks(prev => prev.map(x => x.id === t.id ? { ...x, status: next } : x))
    try {
      await taskApi.changeTaskStatus(Number(t.id), toBackendStatus(next === 'DONE' ? '완료' : '대기중'))
    } catch {
      setTasks(prev => prev.map(x => x.id === t.id ? { ...x, status: t.status } : x))
    }
  }

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

  return (
    <div className="page" data-screen-label="Project Detail">
      <div className="page-head">
        <div>
          <div className="row" style={{ marginBottom: 6 }}>
            <button className="btn btn-ghost" onClick={back} style={{ padding: '2px 8px', fontSize: 12 }}>← 프로젝트</button>
            <HealthBadge health={project.health} />
            <span className="tiny">D-{daysFromNow(project.deadline)}</span>
          </div>
          <h1>{project.name}</h1>
          <p className="lede">{project.goal}</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: isMember ? '1fr' : '1.5fr 1fr', gap: 20 }}>
        <div>
          <div className="row" style={{ marginBottom: 12 }}>
            <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>
              {isMember ? '내 작업' : `작업 (${visibleTasks.length})`}
            </h2>
            <span className="tiny">
              {isMember ? `${visibleTasks.length}건 담당` : `완료 ${tasks.filter(isDone).length}/${tasks.length}`}
            </span>
            <div style={{ marginLeft: 'auto' }}>
              <Segmented value={view} onChange={setView} options={[
                { value: 'tasks', label: '리스트' },
                { value: 'phase', label: '단계' },
              ]} />
            </div>
          </div>

          <div className="card" style={{ padding: 0, borderTop: '1px solid var(--line)', borderBottom: '1px solid var(--line)' }}>
            {loading && <div className="placeholder" style={{ border: 0, padding: 24 }}><div className="mono">LOADING</div><div>작업을 불러오는 중…</div></div>}

            {!loading && view === 'tasks' && visibleTasks.map((t, i) => {
              const assignee = t.assigneeId ? memberById.get(t.assigneeId) : null
              const done = isDone(t)
              const mine = isMember && t.assigneeId === currentUser.id
              return (
                <div key={t.id} className={'task' + (done ? ' done' : '')} style={{ margin: '0 18px', padding: '12px 0', background: mine && !done ? 'oklch(0.62 0.18 264 / 0.04)' : 'transparent' }}>
                  <button className={'task-check' + (done ? ' done' : '')} disabled={isMember && !mine} onClick={() => toggleDone(t)}>
                    <CheckMark />
                  </button>
                  <div className="task-title">
                    <span className="badge" style={{ fontSize: 10 }}>{t.phase}</span>
                    <span className="t">{t.title}</span>
                  </div>
                  <span className="task-meta">{t.hours}h</span>
                  <span className="task-meta" style={{ color: t.difficulty === 'hard' ? 'var(--bad)' : t.difficulty === 'medium' ? 'var(--warn)' : 'var(--ok)' }}>{t.difficulty}</span>
                  <span className="task-assignee">
                    {assignee ? <><Avatar member={assignee} size={22} /><span style={{ fontSize: 12 }}>{assignee.name}</span></>
                      : <span className="muted mono" style={{ fontSize: 11 }}>미배정</span>}
                  </span>
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
                          <span className="mono muted" style={{ fontSize: 11 }}>{t.hours}h</span>
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
    </div>
  )
}
