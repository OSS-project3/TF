import { useState, useMemo } from 'react'
import Avatar from '../../ui/Avatar/Avatar'
import { daysFromNow } from '../../utils/date'
import { aiApi } from '../../api'

const PHASES = ['기획', '디자인', '개발', '테스트', '배포']
const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD']

const todayStr = () => new Date().toISOString().slice(0, 10)
const maxDateStr = () => new Date(new Date().setFullYear(new Date().getFullYear() + 5)).toISOString().slice(0, 10)

function emptyTask(members) {
  return { _key: Math.random(), title: '', phase: '개발', startDate: '', endDate: '', difficulty: 'MEDIUM', assigneeId: members[0]?.id ?? null, estimatedHours: '' }
}

const inputSt = { fontSize: 11, padding: '4px 4px', border: '1px solid var(--line)', borderRadius: 4, background: 'var(--surface)' }

function TaskEditor({ tasks, setTasks, members }) {
  function update(key, field, value) {
    setTasks(prev => prev.map(t => t._key === key ? { ...t, [field]: value } : t))
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 68px 96px 96px 84px 96px 48px 24px', gap: 4 }}>
        <span className="tiny muted" style={{ fontSize: 10 }}>제목</span>
        <span className="tiny muted" style={{ fontSize: 10 }}>단계</span>
        <span className="tiny muted" style={{ fontSize: 10 }}>시작일</span>
        <span className="tiny muted" style={{ fontSize: 10 }}>마감일</span>
        <span className="tiny muted" style={{ fontSize: 10 }}>난이도</span>
        <span className="tiny muted" style={{ fontSize: 10 }}>담당자</span>
        <span className="tiny muted" style={{ fontSize: 10 }}>시간(선택)</span>
        <span />
      </div>
      {tasks.map(t => (
        <div key={t._key} style={{ display: 'grid', gridTemplateColumns: '1fr 68px 96px 96px 84px 96px 48px 24px', gap: 4, alignItems: 'center' }}>
          <input value={t.title} onChange={e => update(t._key, 'title', e.target.value)} placeholder="작업 제목" style={{ ...inputSt, fontSize: 12 }} />
          <select value={t.phase} onChange={e => update(t._key, 'phase', e.target.value)} style={inputSt}>
            {PHASES.map(p => <option key={p}>{p}</option>)}
          </select>
          <input type="date" value={t.startDate} onChange={e => update(t._key, 'startDate', e.target.value)} style={inputSt} />
          <input type="date" value={t.endDate} onChange={e => update(t._key, 'endDate', e.target.value)} style={inputSt} />
          <select value={t.difficulty} onChange={e => update(t._key, 'difficulty', e.target.value)} style={inputSt}>
            {DIFFICULTIES.map(d => <option key={d}>{d}</option>)}
          </select>
          <select value={t.assigneeId ?? ''} onChange={e => update(t._key, 'assigneeId', e.target.value ? Number(e.target.value) : null)} style={inputSt}>
            <option value="">미배정</option>
            {members.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
          </select>
          <input type="number" min={1} max={999} value={t.estimatedHours} onChange={e => update(t._key, 'estimatedHours', e.target.value)} placeholder="-" style={{ ...inputSt, textAlign: 'right' }} />
          <button onClick={() => setTasks(prev => prev.filter(x => x._key !== t._key))} style={{ border: 0, background: 'none', color: 'var(--muted)', cursor: 'pointer', fontSize: 14, padding: 0, lineHeight: 1 }}>×</button>
        </div>
      ))}
      <button className="btn" style={{ width: 'fit-content', fontSize: 12, padding: '4px 10px' }} onClick={() => setTasks(prev => [...prev, emptyTask(members)])}>+ 작업 추가</button>
    </div>
  )
}

export default function CreateProjectModal({ members, currentUser, onClose, onCreate }) {
  const [mode, setMode] = useState(null) // null | 'ai' | 'manual'
  const [step, setStep] = useState(1)

  // 공통 필드
  const [name, setName] = useState('')
  const [goal, setGoal] = useState('')
  const [deadline, setDeadline] = useState('')
  const [selected, setSelected] = useState(currentUser ? [currentUser.id] : [])

  // 태스크 목록 (AI 미리보기 or 직접 입력)
  const [tasks, setTasks] = useState([])

  const [aiLoading, setAiLoading] = useState(false)
  const [aiError, setAiError] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const goalSuggestion = useMemo(() => {
    if (!goal || goal.length < 5) return null
    if (goal.includes('측정') || /\d+%/.test(goal)) return null
    return '더 측정 가능하게: 예) "신규 사용자 7일 잔존율 +5%p" — 숫자로 만들면 AI가 더 정확하게 분해해요.'
  }, [goal])

  const today = todayStr()
  const maxDate = maxDateStr()
  const toggle = (id) => setSelected(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id])

  // 선택된 멤버 객체만 (task assignee picker에 사용)
  const selectedMembers = members.filter(m => selected.includes(m.id))

  async function runAi() {
    setAiLoading(true); setAiError('')
    try {
      const ids = Array.from(new Set([...selected.map(Number), Number(currentUser?.id)]))
      const res = await aiApi.decompose({ goal, deadline, memberIds: ids })
      const aiTasks = (res.tasks || []).map((t, i) => ({
        _key: Math.random(),
        title: t.title || '',
        phase: t.phase || '개발',
        startDate: '',
        endDate: '',
        difficulty: String(t.difficulty || 'MEDIUM').toUpperCase(),
        assigneeId: ids[i % ids.length] ?? null,
        estimatedHours: t.estimatedHours > 0 ? String(t.estimatedHours) : '',
      }))
      setTasks(aiTasks.length > 0 ? aiTasks : [emptyTask(selectedMembers)])
      setStep(3)
    } catch {
      setAiError('AI 분석에 실패했습니다. 수동으로 작업을 추가하거나 다시 시도하세요.')
      setTasks([emptyTask(selectedMembers)])
      setStep(3)
    } finally {
      setAiLoading(false)
    }
  }

  async function save() {
    setSaving(true); setError('')
    try {
      const ids = Array.from(new Set([...selected.map(Number), Number(currentUser?.id)]))
      await onCreate({ name, goal: goal || name, deadline, members: ids, tasks })
    } catch (e) {
      setError(e?.code === 'FORBIDDEN' ? '프로젝트 생성은 PM 권한이 필요합니다.' : (e?.message || '생성에 실패했습니다.'))
      setSaving(false)
    }
  }

  // ─── 모드 선택 화면 ───
  if (!mode) {
    return (
      <div className="modal-bg" onClick={onClose}>
        <div className="modal" onClick={e => e.stopPropagation()}>
          <div className="modal-head">
            <h2>새 프로젝트</h2>
            <p>생성 방식을 선택하세요.</p>
          </div>
          <div className="modal-body" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <button
              className="card"
              style={{ textAlign: 'left', cursor: 'pointer', border: '2px solid var(--line)', padding: 16, background: 'none' }}
              onClick={() => { setMode('ai'); setStep(1) }}
            >
              <div style={{ fontSize: 20, marginBottom: 8 }}>✸</div>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>AI 자동 생성</div>
              <div className="muted" style={{ fontSize: 12 }}>목표를 입력하면 AI가 작업과 일정을 자동으로 생성합니다. 결과를 미리 보고 수정할 수 있습니다.</div>
            </button>
            <button
              className="card"
              style={{ textAlign: 'left', cursor: 'pointer', border: '2px solid var(--line)', padding: 16, background: 'none' }}
              onClick={() => { setMode('manual'); setStep(1) }}
            >
              <div style={{ fontSize: 20, marginBottom: 8 }}>✎</div>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>직접 생성</div>
              <div className="muted" style={{ fontSize: 12 }}>프로젝트명, 마감일, 작업을 직접 입력합니다. AI를 사용하지 않습니다.</div>
            </button>
          </div>
          <div className="modal-foot">
            <button className="btn btn-ghost" onClick={onClose}>취소</button>
          </div>
        </div>
      </div>
    )
  }

  // ─── AI 모드 ───
  if (mode === 'ai') {
    const totalSteps = 3
    return (
      <div className="modal-bg" onClick={onClose}>
        <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: step === 3 ? 640 : 480 }}>
          <div className="modal-head">
            <div className="row" style={{ marginBottom: 4 }}>
              <span className="tiny">Step {step} / {totalSteps}</span>
              <span className="tiny" style={{ marginLeft: 'auto', color: 'var(--ai)' }}>✸ AI 자동 생성</span>
            </div>
            <h2>
              {step === 1 ? '프로젝트 정보' : step === 2 ? '팀원 선택' : 'AI 작업 미리보기'}
            </h2>
            <p>
              {step === 1 ? '목표를 자세히 적을수록 AI가 정확하게 분해해요.' :
               step === 2 ? 'AI가 능력과 가용 시간을 보고 자동으로 배정합니다.' :
               'AI가 생성한 작업 및 일정입니다. 저장 전 직접 수정하거나 추가·삭제할 수 있습니다.'}
            </p>
          </div>

          <div className="modal-body">
            {step === 1 && (
              <>
                <div className="field">
                  <label>프로젝트명</label>
                  <input value={name} onChange={e => setName(e.target.value)} placeholder="예) 모바일 결제 리뉴얼" />
                </div>
                <div className="field">
                  <label>목표 (1~2문장)</label>
                  <textarea value={goal} onChange={e => setGoal(e.target.value)} placeholder="예) 결제 플로우를 단순화해 결제 성공률을 12% 향상" />
                  {goalSuggestion && <div className="ai-assist"><span className="glyph">AI</span>{goalSuggestion}</div>}
                </div>
                <div className="field">
                  <label>마감일</label>
                  <input type="date" value={deadline} min={today} max={maxDate} onChange={e => setDeadline(e.target.value)} />
                  {deadline && <div className="hint mono">총 {daysFromNow(deadline)}일 남음</div>}
                </div>
              </>
            )}

            {step === 2 && (
              <>
                <div className="field">
                  <label>팀원</label>
                  <div className="chips" style={{ marginTop: 4 }}>
                    {members.map(m => (
                      <button key={m.id} className={'chip' + (selected.includes(m.id) ? ' active' : '')} onClick={() => toggle(m.id)}>
                        <Avatar member={m} size={20} />
                        {m.name} <span className="muted mono" style={{ fontSize: 10 }}>· {m.role}</span>
                      </button>
                    ))}
                  </div>
                </div>
                <div className="ai-assist" style={{ marginTop: 8 }}>
                  <span className="glyph">AI</span>
                  <div><strong>{selected.length}명</strong> 선택됨. AI가 목표를 분석해 작업으로 분해합니다.</div>
                </div>
                {aiError && <div className="ai-assist" style={{ color: 'var(--bad)', marginTop: 4 }}><span className="glyph">!</span>{aiError}</div>}
              </>
            )}

            {step === 3 && (
              <>
                <div className="ai-assist" style={{ marginBottom: 12 }}>
                  <span className="glyph">AI</span>
                  <div>AI가 생성한 작업 및 일정입니다. 저장 전 직접 수정하거나 추가·삭제할 수 있습니다.</div>
                </div>
                <TaskEditor tasks={tasks} setTasks={setTasks} members={selectedMembers} />
                {error && <div className="ai-assist" style={{ color: 'var(--bad)', marginTop: 8 }}><span className="glyph">!</span>{error}</div>}
              </>
            )}
          </div>

          <div className="modal-foot">
            <button className="btn btn-ghost" onClick={step === 1 ? () => setMode(null) : () => setStep(s => s - 1)} disabled={aiLoading || saving}>
              {step === 1 ? '← 방식 선택' : '이전'}
            </button>
            {step === 1 && (
              <button className="btn btn-primary" onClick={() => setStep(2)} disabled={!name || !goal || !deadline}>다음 →</button>
            )}
            {step === 2 && (
              <button className="btn btn-primary" onClick={runAi} disabled={aiLoading || selected.length === 0}>
                {aiLoading ? 'AI 분석 중…' : '✸ AI로 분석 →'}
              </button>
            )}
            {step === 3 && (
              <button className="btn btn-primary" onClick={save} disabled={saving || tasks.some(t => !t.title.trim())}>
                {saving ? '저장 중…' : '프로젝트 저장'}
              </button>
            )}
          </div>
        </div>
      </div>
    )
  }

  // ─── 직접 생성 모드 ───
  const totalManualSteps = 2
  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: step === 2 ? 640 : 480 }}>
        <div className="modal-head">
          <div className="row" style={{ marginBottom: 4 }}>
            <span className="tiny">Step {step} / {totalManualSteps}</span>
            <span className="tiny" style={{ marginLeft: 'auto', color: 'var(--muted)' }}>✎ 직접 생성</span>
          </div>
          <h2>{step === 1 ? '프로젝트 정보' : '작업 목록'}</h2>
          <p>{step === 1 ? '프로젝트 기본 정보를 입력하세요.' : '프로젝트에 포함할 작업을 추가하세요. (선택 사항)'}</p>
        </div>

        <div className="modal-body">
          {step === 1 && (
            <>
              <div className="field">
                <label>프로젝트명</label>
                <input value={name} onChange={e => setName(e.target.value)} placeholder="예) 모바일 결제 리뉴얼" />
              </div>
              <div className="field">
                <label>마감일</label>
                <input type="date" value={deadline} min={today} max={maxDate} onChange={e => setDeadline(e.target.value)} />
                {deadline && <div className="hint mono">총 {daysFromNow(deadline)}일 남음</div>}
              </div>
              <div className="field">
                <label>팀원</label>
                <div className="chips" style={{ marginTop: 4 }}>
                  {members.map(m => (
                    <button key={m.id} className={'chip' + (selected.includes(m.id) ? ' active' : '')} onClick={() => toggle(m.id)}>
                      <Avatar member={m} size={20} />
                      {m.name} <span className="muted mono" style={{ fontSize: 10 }}>· {m.role}</span>
                    </button>
                  ))}
                </div>
              </div>
            </>
          )}

          {step === 2 && (
            <>
              <TaskEditor tasks={tasks} setTasks={setTasks} members={selectedMembers} />
              {error && <div className="ai-assist" style={{ color: 'var(--bad)', marginTop: 8 }}><span className="glyph">!</span>{error}</div>}
            </>
          )}
        </div>

        <div className="modal-foot">
          <button className="btn btn-ghost" onClick={step === 1 ? () => setMode(null) : () => setStep(1)} disabled={saving}>
            {step === 1 ? '← 방식 선택' : '이전'}
          </button>
          {step === 1 && (
            <button className="btn btn-primary" onClick={() => { setTasks([emptyTask(selectedMembers)]); setStep(2) }} disabled={!name || !deadline}>
              다음 →
            </button>
          )}
          {step === 2 && (
            <button className="btn btn-primary" onClick={save} disabled={saving || tasks.some(t => !t.title.trim())}>
              {saving ? '저장 중…' : '프로젝트 저장'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
