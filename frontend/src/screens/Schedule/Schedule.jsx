import { useState, useEffect, useMemo } from 'react'
import './Schedule.css'
import Avatar from '../../ui/Avatar/Avatar'
import Segmented from '../../ui/Segmented/Segmented'
import { taskApi } from '../../api'
import { adaptTask } from '../../api/adapt'

const PX_PER_DAY = 28
const DAY = 86400000

export default function Schedule({ projects, members, role, currentUser }) {
  const isMember = role === 'member'
  const [viewMode, setViewMode] = useState('gantt')
  const [selectedProjectId, setSelectedProjectId] = useState(projects[0]?.id ?? '')
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [calMonth, setCalMonth] = useState(() => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1) })

  const project = projects.find(p => p.id === selectedProjectId) ?? projects[0]
  const memberById = useMemo(() => new Map(members.map(m => [m.id, m])), [members])

  useEffect(() => {
    if (!selectedProjectId) { setTasks([]); setLoading(false); return }
    let cancelled = false
    setLoading(true)
    taskApi.getProjectTasks(Number(selectedProjectId))
      .then(list => { if (!cancelled) setTasks(list.map(adaptTask)) })
      .catch(() => { if (!cancelled) setTasks([]) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [selectedProjectId])

  // 기간 범위 산정
  const range = useMemo(() => {
    const dates = []
    tasks.forEach(t => { if (t.startDate) dates.push(new Date(t.startDate)); if (t.endDate) dates.push(new Date(t.endDate)) })
    if (project?.deadline) dates.push(new Date(project.deadline))
    const now = new Date(); dates.push(now)
    const start = dates.length ? new Date(Math.min(...dates.map(d => d.getTime()))) : now
    const end = dates.length ? new Date(Math.max(...dates.map(d => d.getTime()))) : now
    const totalDays = Math.max(7, Math.round((end - start) / DAY) + 1)
    return { start, totalDays }
  }, [tasks, project])

  const dayOf = (dateStr) => Math.round((new Date(dateStr) - range.start) / DAY)
  const dayToLabel = (day) => { const d = new Date(range.start.getTime() + day * DAY); return `${d.getMonth() + 1}/${d.getDate()}` }
  const TOTAL_W = range.totalDays * PX_PER_DAY
  const todayDay = Math.round((new Date() - range.start) / DAY)

  // 스케줄 아이템 (날짜 있는 태스크)
  const items = useMemo(() => tasks.map(t => {
    const startDay = t.startDate ? dayOf(t.startDate) : 0
    const endDay = t.endDate ? dayOf(t.endDate) : startDay + Math.max(2, Math.round(t.hours / 4))
    return {
      id: t.id, title: t.title, owner: t.assigneeId ?? '', startDay, endDay: Math.max(endDay, startDay + 1),
      kind: t.critical ? 'ai' : t.lateRisk ? 'late' : '', done: t.status === 'DONE', phase: t.phase,
    }
  }), [tasks, range])

  const grouped = useMemo(() => {
    const m = {}
    items.forEach(it => { (m[it.owner] = m[it.owner] ?? []).push(it) })
    return m
  }, [items])

  const weekMarkers = useMemo(() => {
    const out = []
    for (let d = 0; d < range.totalDays; d += 7) out.push({ day: d, label: dayToLabel(d) })
    return out
  }, [range])

  const DAY_NAMES = ['일', '월', '화', '수', '목', '금', '토']
  const calYear = calMonth.getFullYear()
  const calMon = calMonth.getMonth()
  const calFirstWkday = new Date(calYear, calMon, 1).getDay()
  const calDaysInMonth = new Date(calYear, calMon + 1, 0).getDate()
  const tasksOnDay = (day) => {
    const off = Math.round((new Date(calYear, calMon, day) - range.start) / DAY)
    return items.filter(t => t.startDay <= off && off < t.endDay)
  }

  return (
    <div className="page" data-screen-label="Schedule">
      <div className="page-head">
        <div>
          <h1>일정</h1>
          <p className="lede">{project?.name} · 마감일과 의존성을 반영해 자동 배치</p>
        </div>
        <div className="page-head-actions">
          <select className="schedule-project-select" value={selectedProjectId} onChange={e => setSelectedProjectId(e.target.value)}>
            {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
          <Segmented value={viewMode} onChange={setViewMode} options={[
            { value: 'gantt', label: '간트' },
            { value: 'cal', label: '캘린더' },
            { value: 'list', label: '리스트' },
          ]} />
        </div>
      </div>

      {loading && <div className="placeholder" style={{ padding: 32 }}><div className="mono">LOADING</div></div>}
      {!loading && items.length === 0 && <div className="placeholder" style={{ padding: 32 }}><div className="mono">EMPTY</div><div>일정이 없습니다.</div></div>}

      {!loading && items.length > 0 && viewMode === 'gantt' && (
        <div className="schedule-wrap">
          <div className="schedule-row schedule-week-row">
            <div className="schedule-label schedule-label-header" style={{ fontSize: 11, color: 'var(--muted-2)' }}>담당자 / 작업</div>
            <div className="schedule-track" style={{ width: TOTAL_W, height: 24 }}>
              {weekMarkers.map(w => <div key={w.day} className="schedule-week-tick" style={{ left: w.day * PX_PER_DAY }}>{w.label}</div>)}
            </div>
          </div>
          {Object.entries(grouped).map(([mid, rows]) => {
            const member = memberById.get(mid)
            const isMe = isMember && mid === currentUser.id
            return (
              <div key={mid || 'none'}>
                <div className="schedule-row" style={{ background: isMe ? 'var(--ai-soft)' : 'var(--surface)' }}>
                  <div className="schedule-label" style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                    <Avatar member={member} size={24} />
                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                      <span style={{ fontSize: 13, fontWeight: 600 }}>{member?.name ?? '미배정'}</span>
                      <span className="who">{member?.role ?? ''} · {rows.length}개 작업</span>
                    </div>
                  </div>
                  <div className="schedule-track" style={{ width: TOTAL_W }}>
                    {todayDay >= 0 && <div className="schedule-today-line" style={{ left: todayDay * PX_PER_DAY }}></div>}
                  </div>
                </div>
                {rows.map(r => (
                  <div key={r.id} className="schedule-row">
                    <div className="schedule-label" style={{ paddingLeft: 36 }}>
                      <div className="schedule-task-inline">
                        <span className="schedule-task-title">{r.title}</span>
                        <span className="schedule-task-date">{dayToLabel(r.startDay)}~{dayToLabel(r.endDay)}</span>
                      </div>
                    </div>
                    <div className="schedule-track" style={{ width: TOTAL_W }}>
                      <div className={'tl-bar' + (r.kind ? ' ' + r.kind : '') + (r.done ? ' done' : '')}
                        style={{ left: r.startDay * PX_PER_DAY, width: (r.endDay - r.startDay) * PX_PER_DAY }}>
                        {r.kind === 'ai' && <span style={{ marginRight: 6, opacity: 0.8 }}>✸</span>}
                        {r.title}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )
          })}
        </div>
      )}

      {!loading && items.length > 0 && viewMode === 'cal' && (
        <div className="schedule-cal">
          <div className="schedule-cal-nav">
            <button className="btn btn-ghost" onClick={() => setCalMonth(new Date(calYear, calMon - 1, 1))}>‹</button>
            <span style={{ fontWeight: 600, fontSize: 15 }}>{calYear}년 {calMon + 1}월</span>
            <button className="btn btn-ghost" onClick={() => setCalMonth(new Date(calYear, calMon + 1, 1))}>›</button>
          </div>
          <div className="schedule-cal-grid">
            {DAY_NAMES.map(d => <div key={d} className="schedule-cal-head">{d}</div>)}
            {Array.from({ length: calFirstWkday }).map((_, i) => <div key={'e' + i} className="schedule-cal-cell schedule-cal-empty" />)}
            {Array.from({ length: calDaysInMonth }).map((_, i) => {
              const day = i + 1
              const dayTasks = tasksOnDay(day)
              return (
                <div key={day} className="schedule-cal-cell">
                  <span className="schedule-cal-day">{day}</span>
                  <div className="schedule-cal-tasks">
                    {dayTasks.slice(0, 3).map(t => (
                      <div key={t.id} className={'schedule-cal-chip' + (t.kind ? ' ' + t.kind : '') + (t.done ? ' done' : '')} title={t.title}>{t.title}</div>
                    ))}
                    {dayTasks.length > 3 && <div className="schedule-cal-more">+{dayTasks.length - 3}개</div>}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {!loading && items.length > 0 && viewMode === 'list' && (
        <div className="schedule-list">
          <table className="schedule-list-table">
            <thead><tr><th>작업명</th><th>담당자</th><th>기간</th><th>단계</th><th>상태</th></tr></thead>
            <tbody>
              {[...items].sort((a, b) => a.startDay - b.startDay).map(r => {
                const member = memberById.get(r.owner)
                const badgeKind = r.done ? 'ok' : r.kind === 'late' ? 'bad' : r.kind === 'ai' ? 'ai' : ''
                const statusText = r.done ? '완료' : r.kind === 'late' ? '지연 위험' : r.kind === 'ai' ? '임계 경로' : '진행 중'
                return (
                  <tr key={r.id}>
                    <td className="schedule-list-title">{r.title}</td>
                    <td><div className="row gap-sm" style={{ alignItems: 'center' }}><Avatar member={member} size={18} /><span style={{ fontSize: 12 }}>{member?.name ?? '—'}</span></div></td>
                    <td className="mono" style={{ fontSize: 12, whiteSpace: 'nowrap', color: 'var(--muted)' }}>{dayToLabel(r.startDay)}~{dayToLabel(r.endDay)}</td>
                    <td style={{ fontSize: 12, color: 'var(--muted)' }}>{r.phase}</td>
                    <td><span className={'badge' + (badgeKind ? ' ' + badgeKind : '')} style={{ fontSize: 11 }}>{statusText}</span></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
