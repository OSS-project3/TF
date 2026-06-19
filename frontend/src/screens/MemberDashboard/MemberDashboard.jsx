import { useState } from 'react'
import './MemberDashboard.css'
import TaskRow from './TaskRow'
import Avatar from '../../ui/Avatar/Avatar'
import ProgressBar from '../../ui/ProgressBar/ProgressBar'
import HealthBadge from '../../ui/HealthBadge/HealthBadge'
import { daysFromNow } from '../../utils/date'
import { adaptTask } from '../../api/adapt'

export default function MemberDashboard({ currentUser, projects, members, openProject, dash }) {
  const today = (dash?.todayTasks ?? []).map(adaptTask)
  const week  = (dash?.thisWeekTasks ?? []).map(adaptTask)
  const later = (dash?.laterTasks ?? []).map(adaptTask)

  const [done, setDone] = useState(new Set())
  const toggle = (id) => setDone(s => {
    const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n
  })

  const myProjects = projects.filter(p => p.members.includes(currentUser.id))
  const loadPct   = dash?.loadRate ?? 0
  const loadHours = dash?.assignedHours ?? 0
  const capacity  = dash?.capacityHours ?? currentUser.hours

  return (
    <div className="page" data-screen-label="My Dashboard">
      <div className="page-head">
        <div>
          <h1>안녕하세요, {currentUser.name} 님</h1>
          <p className="lede">오늘 {today.length}건 · 이번 주 {week.length}건 · 부하 {Math.round(loadPct * 100)}%</p>
        </div>
      </div>

      <div className="card ai-callout" style={{ marginBottom: 24 }}>
        <div className="row">
          <span className="badge ai">✸ 오늘의 추천</span>
          <div style={{ fontSize: 13.5 }}>
            {loadPct > 0.85 ? (
              <>이번 주 부하가 <strong>{Math.round(loadPct * 100)}%</strong>로 높아요. <span className="muted">일부 작업을 다음 주로 미루는 것을 고려하세요.</span></>
            ) : today[0] ? (
              <>오늘 가장 중요한 작업은 <strong>"{today[0].title}"</strong>이에요. <span className="muted">집중 시간 2~3h 권장.</span></>
            ) : (
              <>오늘 마감인 작업이 없어요. <span className="muted">이번 주 작업을 미리 살펴보세요.</span></>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-4" style={{ marginBottom: 28 }}>
        <div className="card stat">
          <div className="stat-label">이번 주 부하</div>
          <div className="stat-value">{Math.round(loadPct * 100)}%</div>
          <div className={'stat-delta ' + (loadPct > 0.85 ? 'down' : '')}>{loadHours}h / {capacity}h</div>
        </div>
        <div className="card stat">
          <div className="stat-label">참여 프로젝트</div>
          <div className="stat-value">{dash?.projectCount ?? myProjects.length}</div>
          <div className="stat-delta">활성</div>
        </div>
        <div className="card stat">
          <div className="stat-label">담당 작업</div>
          <div className="stat-value">
            {dash?.doneTaskCount ?? 0}<span style={{ color: 'var(--muted-2)', fontSize: 18 }}> / {dash?.taskCount ?? 0}</span>
          </div>
          <div className="stat-delta">완료 / 전체</div>
        </div>
        <div className="card stat">
          <div className="stat-label">다음 마감</div>
          <div className="stat-value" style={{ fontSize: 22 }}>{dash?.nextDueDate ?? '없음'}</div>
          <div className="stat-delta">{dash?.nextDueDate ? `D-${daysFromNow(dash.nextDueDate)}` : '—'}</div>
        </div>
      </div>

      <div className="row" style={{ marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>오늘</h2>
        <span className="tiny">{today.length}</span>
      </div>
      <div className="card" style={{ padding: '0 18px', borderTop: '1px solid var(--line)', borderBottom: '1px solid var(--line)', marginBottom: 28 }}>
        {today.map(t => <TaskRow key={t.id} t={t} done={done.has(t.id)} onToggle={toggle} highlight />)}
        {today.length === 0 && (
          <div className="placeholder" style={{ border: 0, padding: 24 }}>
            <div className="mono">EMPTY</div>
            <div>오늘 마감인 작업이 없어요. 한가한 하루 되세요.</div>
          </div>
        )}
      </div>

      <div className="row" style={{ marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>이번 주</h2>
        <span className="tiny">{week.length}</span>
      </div>
      <div className="card" style={{ padding: '0 18px', borderTop: '1px solid var(--line)', borderBottom: '1px solid var(--line)', marginBottom: 28 }}>
        {week.map(t => <TaskRow key={t.id} t={t} done={done.has(t.id)} onToggle={toggle} />)}
        {week.length === 0 && <div className="placeholder" style={{ border: 0, padding: 24 }}><div className="mono">EMPTY</div><div>이번 주 마감 작업이 없습니다.</div></div>}
      </div>

      {later.length > 0 && (
        <>
          <div className="row" style={{ marginBottom: 12 }}>
            <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>다음 주 이후</h2>
            <span className="tiny">{later.length}</span>
          </div>
          <div className="card" style={{ padding: '0 18px', borderTop: '1px solid var(--line)', borderBottom: '1px solid var(--line)', marginBottom: 28 }}>
            {later.map(t => <TaskRow key={t.id} t={t} done={done.has(t.id)} onToggle={toggle} muted />)}
          </div>
        </>
      )}

      <div className="row" style={{ marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>참여 프로젝트</h2>
        <span className="tiny">{myProjects.length}</span>
      </div>
      <div className="grid grid-3">
        {myProjects.map(p => {
          const m = members.filter(mb => p.members.includes(mb.id))
          return (
            <button key={p.id} className="card" style={{ textAlign: 'left', cursor: 'pointer', background: 'transparent', border: 0 }} onClick={() => openProject(p.id)}>
              <div className="row" style={{ marginBottom: 10 }}>
                <HealthBadge health={p.health} />
                <span className="tiny" style={{ marginLeft: 'auto' }}>D-{daysFromNow(p.deadline)}</span>
              </div>
              <h3 style={{ margin: '0 0 4px', fontSize: 15, fontWeight: 600 }}>{p.name}</h3>
              <p className="muted" style={{ margin: '0 0 14px', fontSize: 12.5, lineHeight: 1.5, minHeight: 38 }}>{p.goal}</p>
              <div className="row gap-sm" style={{ marginBottom: 8 }}>
                <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>{p.done}/{p.tasks}</span>
                <span className="mono" style={{ fontSize: 11, marginLeft: 'auto' }}>{Math.round(p.progress * 100)}%</span>
              </div>
              <ProgressBar value={p.progress} kind={p.health === 'warn' ? 'warn' : ''} />
              <div className="row" style={{ marginTop: 14 }}>
                <div className="row gap-sm">
                  {m.slice(0, 4).map(mm => <Avatar key={mm.id} member={mm} size={22} />)}
                  {m.length > 4 && <span className="tiny">+{m.length - 4}</span>}
                </div>
              </div>
            </button>
          )
        })}
        {myProjects.length === 0 && <div className="placeholder" style={{ border: 0, padding: 32 }}><div className="mono">EMPTY</div><div>참여 중인 프로젝트가 없습니다.</div></div>}
      </div>
    </div>
  )
}
