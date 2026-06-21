import { useState, useEffect, useMemo } from 'react'
import './Meetings.css'
import Avatar from '../../ui/Avatar/Avatar'
import CheckMark from '../../ui/CheckMark/CheckMark'
import { aiApi, meetingApi } from '../../api'

const today = () => new Date().toISOString().slice(0, 10)
const fmt = (s) => { const d = new Date(s); return `${d.getMonth() + 1}월 ${d.getDate()}일` }

export default function Meetings({ members, projects }) {
  const [view, setView] = useState('compose') // compose | records | manual
  const [projectId, setProjectId] = useState(projects[0]?.id ?? '')

  // compose
  const [notes, setNotes] = useState('')
  const [phase, setPhase] = useState('input') // input | processing | result
  const [summary, setSummary] = useState([])
  const [todos, setTodos] = useState([])
  const [saved, setSaved] = useState(false)
  const [err, setErr] = useState('')

  // records
  const [meetings, setMeetings] = useState([])
  const [expandedId, setExpandedId] = useState(null)

  // manual
  const [manualForm, setManualForm] = useState({ title: '', date: today(), attendees: '', notes: '' })

  const memberById = useMemo(() => new Map(members.map(m => [m.id, m])), [members])
  const memberByName = useMemo(() => new Map(members.map(m => [m.name, m])), [members])
  const project = projects.find(p => p.id === projectId)
  const projectMembers = members.filter(m => project?.members.includes(m.id))

  const loadMeetings = () => {
    meetingApi.getMeetings()
      .then(setMeetings)
      .catch(() => setMeetings([]))
  }
  useEffect(() => { loadMeetings() }, [])

  async function process() {
    setPhase('processing'); setErr(''); setSaved(false); setSummary([]); setTodos([])
    try {
      const res = await aiApi.summarizeMeeting({ notes, projectId: projectId ? Number(projectId) : undefined })
      setSummary(res.summary || [])
      setTodos((res.todos || []).map(t => ({
        what: t.title,
        who: t.assignee || (projectMembers[0]?.name ?? ''),
        due: t.dueDate || '',
      })))
      setPhase('result')
    } catch (e) {
      setErr(e?.code === 'AI_DISABLED' ? 'AI 기능이 비활성화되어 있습니다 (OPENAI_API_KEY 설정 필요).' : (e?.message || 'AI 요약에 실패했습니다.'))
      setPhase('input')
    }
  }

  function resetCompose() { setPhase('input'); setNotes(''); setSummary([]); setTodos([]); setSaved(false); setErr('') }

  async function saveToRecords() {
    if (!project) { setErr('프로젝트를 선택하세요.'); return }
    try {
      await meetingApi.createMeeting({
        title: `${project.name} 회의록`,
        date: today(),
        attendeeMemberIds: projectMembers.map(m => Number(m.id)),
        notes,
        summary,
        todos: todos.map(t => ({
          assigneeId: Number((memberByName.get(t.who) ?? projectMembers[0])?.id),
          projectId: Number(projectId),
          title: t.what,
          dueDate: t.due || undefined,
        })),
        manual: false,
      })
      setSaved(true)
      loadMeetings()
    } catch (e) {
      setErr(e?.message || '저장에 실패했습니다.')
    }
  }

  async function manualSave() {
    if (!manualForm.title.trim() || !manualForm.notes.trim() || !project) return
    try {
      await meetingApi.createMeeting({
        title: manualForm.title.trim(),
        date: manualForm.date,
        attendeeMemberIds: projectMembers.map(m => Number(m.id)),
        notes: manualForm.notes,
        summary: [],
        todos: [],
        manual: true,
      })
      setManualForm({ title: '', date: today(), attendees: '', notes: '' })
      loadMeetings()
      setView('records')
    } catch (e) {
      setErr(e?.message || '저장에 실패했습니다.')
    }
  }

  return (
    <div className="page" data-screen-label="Meetings">
      <div className="page-head">
        <div>
          <h1>회의록</h1>
          <p className="lede">
            {view === 'records' ? `총 ${meetings.length}개의 회의 기록`
              : view === 'manual' ? '회의록 수기 등록'
              : '회의 내용을 붙여넣으면 요약과 액션 아이템을 자동 생성합니다'}
          </p>
        </div>
        <div className="page-head-actions">
          <select className="schedule-project-select" value={projectId} onChange={e => setProjectId(e.target.value)}>
            {projects.length === 0 && <option value="">프로젝트 없음</option>}
            {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
          {view === 'compose' && phase === 'result' && <button className="btn" onClick={resetCompose}>다시 작성</button>}
          {view !== 'manual' && <button className="btn" onClick={() => setView('manual')}>수기 등록</button>}
          {view === 'manual' && <button className="btn btn-ghost" onClick={() => setView('compose')}>취소</button>}
          <button className="btn" onClick={() => setView(v => v === 'records' ? 'compose' : 'records')}>
            {view === 'records' ? '닫기' : `기록 보기 (${meetings.length})`}
          </button>
        </div>
      </div>

      {err && <div className="ai-assist" style={{ marginBottom: 14, color: 'var(--bad)' }}><span className="glyph">!</span>{err}</div>}

      {/* RECORDS */}
      {view === 'records' && (
        <div className="meeting-records">
          {meetings.length === 0 && <div className="placeholder" style={{ padding: '40px 0' }}>저장된 회의록이 없습니다</div>}
          {meetings.map(m => {
            const isOpen = expandedId === m.id
            return (
              <div key={m.id} className={'meeting-record-item' + (isOpen ? ' expanded' : '')}>
                <button className="meeting-record-header" onClick={() => setExpandedId(isOpen ? null : m.id)}>
                  <div className="row gap-sm" style={{ alignItems: 'center', flex: 1 }}>
                    <span className="badge" style={{ fontSize: 11 }}>{fmt(m.date)}</span>
                    {m.manual && <span className="badge" style={{ fontSize: 11 }}>수기</span>}
                    <span style={{ fontWeight: 600, fontSize: 14 }}>{m.title}</span>
                    <span className="muted" style={{ fontSize: 12, marginLeft: 'auto' }}>{m.attendeeMemberIds?.length ?? 0}명 참석</span>
                  </div>
                  <span className="meeting-record-chevron">{isOpen ? '▲' : '▼'}</span>
                </button>
                {isOpen && (
                  <div className="meeting-record-body">
                    <div className="row gap-sm" style={{ flexWrap: 'wrap', marginBottom: 12 }}>
                      {(m.attendeeMemberIds ?? []).map(id => {
                        const mem = memberById.get(String(id))
                        return (
                          <div key={id} className="row gap-sm" style={{ alignItems: 'center' }}>
                            {mem && <Avatar member={mem} size={20} />}
                            <span style={{ fontSize: 12 }}>{mem?.name ?? `#${id}`}</span>
                          </div>
                        )
                      })}
                    </div>
                    {m.summary?.length > 0 && (
                      <div style={{ marginBottom: 14 }}>
                        <div className="tiny" style={{ marginBottom: 8 }}>요약</div>
                        {m.summary.map((s, i) => <div key={i} className="summary-bullet">{s}</div>)}
                      </div>
                    )}
                    {m.todos?.length > 0 && (
                      <div style={{ marginBottom: 14 }}>
                        <div className="tiny" style={{ marginBottom: 8 }}>액션 아이템</div>
                        {m.todos.map((t, i) => {
                          const mem = memberById.get(String(t.assigneeId))
                          return (
                            <div key={i} className="row" style={{ padding: '6px 0', borderBottom: i < m.todos.length - 1 ? '1px solid var(--line)' : 0, alignItems: 'flex-start' }}>
                              <div style={{ minWidth: 0, flex: 1 }}>
                                <div style={{ fontSize: 13, marginBottom: 4 }}>{t.title}</div>
                                <div className="row gap-sm">
                                  {mem && <Avatar member={mem} size={16} />}
                                  <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>{mem?.name ?? `#${t.assigneeId}`}</span>
                                  {t.dueDate && <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>· {t.dueDate}</span>}
                                </div>
                              </div>
                            </div>
                          )
                        })}
                      </div>
                    )}
                    {m.notes && (
                      <div className="meeting-record-notes">
                        <div className="tiny" style={{ marginBottom: 6 }}>회의 노트</div>
                        <pre className="meeting-notes-pre">{m.notes}</pre>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* MANUAL */}
      {view === 'manual' && (
        <div className="meeting-manual">
          <div className="field">
            <label>회의 제목</label>
            <input type="text" placeholder="예: 스프린트 체크인" value={manualForm.title} onChange={e => setManualForm(f => ({ ...f, title: e.target.value }))} />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="field">
              <label>날짜</label>
              <input type="date" value={manualForm.date} onChange={e => setManualForm(f => ({ ...f, date: e.target.value }))} />
            </div>
            <div className="field">
              <label>참석자</label>
              <input type="text" value={projectMembers.map(m => m.name).join(', ')} disabled />
            </div>
          </div>
          <div className="field" style={{ flex: 1 }}>
            <label>회의 노트</label>
            <textarea placeholder="회의 내용, 결정사항 등을 기록하세요" value={manualForm.notes} onChange={e => setManualForm(f => ({ ...f, notes: e.target.value }))} style={{ minHeight: 200 }} />
          </div>
          <div className="row" style={{ marginTop: 8 }}>
            <span className="tiny" style={{ color: 'var(--muted-2)' }}>{manualForm.notes.length} 자</span>
            <button className="btn btn-primary" style={{ marginLeft: 'auto' }} onClick={manualSave} disabled={!manualForm.title.trim() || !manualForm.notes.trim()}>저장</button>
          </div>
        </div>
      )}

      {/* COMPOSE */}
      {view === 'compose' && (
        <>
          <div className="meeting-input">
            <div className="row">
              <span className="tiny">회의 노트</span>
              <span className="tiny" style={{ marginLeft: 'auto', color: notes.length >= 180 ? 'var(--bad, #e55)' : 'var(--muted-2)' }}>{notes.length}/200 자</span>
            </div>
            <textarea value={notes} onChange={e => setNotes(e.target.value.slice(0, 200))} maxLength={200} placeholder="회의 노트를 붙여넣으세요. 발언자, 결정사항, 다음 액션…" disabled={phase !== 'input'} />
            <div className="row">
              {phase === 'input' && (
                <button className="btn btn-primary" style={{ marginLeft: 'auto' }} onClick={process} disabled={notes.length < 20}>✸ AI로 요약하기</button>
              )}
              {phase === 'processing' && (
                <div className="row" style={{ width: '100%' }}>
                  <span className="spin"></span>
                  <span className="mono" style={{ fontSize: 12, color: 'var(--ai)' }}>AI가 분석 중…</span>
                </div>
              )}
            </div>
          </div>

          {phase === 'result' && (
            <div className="meeting-result">
              <div className="card">
                <div className="card-head">
                  <span className="badge ai">✸ 요약</span>
                  <h3 style={{ marginLeft: 8 }}>핵심 내용</h3>
                </div>
                <div>{summary.map((s, i) => <div key={i} className="summary-bullet fade-in">{s}</div>)}</div>
              </div>
              <div className="card">
                <div className="card-head">
                  <span className="badge ai">✸ TODO</span>
                  <h3 style={{ marginLeft: 8 }}>액션 아이템</h3>
                </div>
                <div className="col gap-sm">
                  {todos.map((t, i) => {
                    const mem = memberByName.get(t.who)
                    return (
                      <div key={i} className="row fade-in" style={{ padding: '8px 0', borderBottom: i < todos.length - 1 ? '1px solid var(--line)' : 0, alignItems: 'flex-start' }}>
                        <button className="task-check" style={{ marginTop: 2 }}><CheckMark /></button>
                        <div style={{ minWidth: 0, flex: 1 }}>
                          <div style={{ fontSize: 13, marginBottom: 4 }}>{t.what}</div>
                          <div className="row gap-sm">
                            {mem && <Avatar member={mem} size={18} />}
                            <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>{t.who || '미배정'}</span>
                            {t.due && <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>· {t.due}</span>}
                          </div>
                        </div>
                      </div>
                    )
                  })}
                  {todos.length === 0 && <span className="muted" style={{ fontSize: 12 }}>추출된 액션 아이템이 없습니다.</span>}
                </div>
                <div className="row gap-sm" style={{ marginTop: 14 }}>
                  {!saved ? (
                    <button className="btn btn-primary" onClick={saveToRecords}>기록에 저장</button>
                  ) : (
                    <span style={{ fontSize: 13, color: 'var(--ok)', display: 'flex', alignItems: 'center', gap: 4 }}>✓ 저장됨</span>
                  )}
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
