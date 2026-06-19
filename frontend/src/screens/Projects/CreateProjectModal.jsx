import { useState, useMemo } from 'react'
import Avatar from '../../ui/Avatar/Avatar'
import { daysFromNow } from '../../utils/date'

export default function CreateProjectModal({ members, currentUser, onClose, onCreate }) {
  const [step, setStep] = useState(1)
  const [name, setName] = useState('')
  const [goal, setGoal] = useState('')
  const [deadline, setDeadline] = useState('')
  const [selected, setSelected] = useState(currentUser ? [currentUser.id] : [])
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')

  const goalSuggestion = useMemo(() => {
    if (!goal || goal.length < 5) return null
    if (goal.includes('측정') || /\d+%/.test(goal)) return null
    return '더 측정 가능하게: 예) "신규 사용자 7일 잔존율 +5%p" — 숫자로 만들면 AI가 더 정확하게 분해해요.'
  }, [goal])

  const toggle = (id) => setSelected(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id])

  async function submit() {
    setCreating(true); setError('')
    try {
      await onCreate({ name, goal, deadline, members: selected })
    } catch (e) {
      setError(e?.code === 'FORBIDDEN' ? '프로젝트 생성은 PM 권한이 필요합니다.' : (e?.message || '생성에 실패했습니다.'))
      setCreating(false)
    }
  }

  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-head">
          <div className="row" style={{ marginBottom: 4 }}>
            <span className="tiny">Step {step} / 2</span>
            <span className="tiny" style={{ marginLeft: 'auto', color: 'var(--ai)' }}>✸ AI 어시스트</span>
          </div>
          <h2>{step === 1 ? '프로젝트 만들기' : '팀원 선택'}</h2>
          <p>{step === 1 ? '목표를 자세히 적을수록 AI가 정확하게 분해해요.' : 'AI가 능력과 가용 시간을 보고 자동으로 배정합니다.'}</p>
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
                <input type="date" value={deadline} onChange={e => setDeadline(e.target.value)} />
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
              {error && <div className="ai-assist" style={{ color: 'var(--bad)' }}><span className="glyph">!</span>{error}</div>}
            </>
          )}
        </div>

        <div className="modal-foot">
          {step > 1 && <button className="btn" onClick={() => setStep(1)} disabled={creating}>이전</button>}
          <button className="btn btn-ghost" onClick={onClose} disabled={creating}>취소</button>
          {step === 1 && (
            <button className="btn btn-primary" onClick={() => setStep(2)} disabled={!name || !goal || !deadline}>다음 →</button>
          )}
          {step === 2 && (
            <button className="btn btn-primary" onClick={submit} disabled={creating || selected.length === 0}>
              {creating ? '생성 중…' : '✸ AI로 작업 분해'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
