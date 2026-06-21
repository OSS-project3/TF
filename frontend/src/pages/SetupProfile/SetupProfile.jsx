import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { memberApi } from '../../api'
import { useAuth } from '../../context/AuthContext.jsx'
import '../auth.css'

const PRESET_HOURS = [10, 20, 30, 40]
const SKILL_SUGGESTIONS = [
  'React', 'Vue', 'TypeScript', 'JavaScript', 'HTML/CSS',
  'Spring Boot', 'Node.js', 'Django', 'FastAPI', 'Java', 'Python',
  'MySQL', 'PostgreSQL', 'MongoDB', 'Redis',
  'Docker', 'AWS', 'Git', 'Figma', 'UI/UX',
]

export default function SetupProfile() {
  const navigate = useNavigate()
  const { refreshUser } = useAuth()
  const [hours, setHours] = useState(40)
  const [customHours, setCustomHours] = useState('')
  const [useCustom, setUseCustom] = useState(false)
  const [skills, setSkills] = useState([])
  const [skillInput, setSkillInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const finalHours = useCustom ? Number(customHours) || 0 : hours

  function toggleSkill(s) {
    setSkills(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s])
  }

  function addCustomSkill(e) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      const s = skillInput.trim()
      if (s && !skills.includes(s)) setSkills(prev => [...prev, s])
      setSkillInput('')
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (finalHours < 1 || finalHours > 168) {
      setError('가용 시간은 1~168 사이로 입력해주세요.')
      return
    }
    setLoading(true); setError('')
    try {
      await memberApi.updateMe({ weeklyCapacityHours: finalHours, skills })
      await refreshUser()
      navigate('/', { replace: true })
    } catch {
      setError('저장에 실패했습니다. 다시 시도해주세요.')
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card" style={{ maxWidth: 480 }}>
        <div className="auth-brand-row">
          <div className="auth-brand-mark"></div>
          <div className="brand-name" style={{ fontSize: 16, fontWeight: 600 }}>TeamFlow<span className="ai">AI</span></div>
        </div>
        <h1 className="auth-title">프로필 설정</h1>
        <p className="auth-sub" style={{ marginBottom: 24 }}>AI가 작업을 배분할 때 아래 정보를 활용합니다.</p>

        <form onSubmit={handleSubmit}>
          {/* 가용 시간 */}
          <div className="field" style={{ marginBottom: 24 }}>
            <label style={{ fontWeight: 600, marginBottom: 6, display: 'block' }}>
              주간 가용 시간
            </label>
            <p style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 10 }}>
              일주일에 이 프로젝트에 투자할 수 있는 시간입니다. AI가 이 값을 기준으로 작업을 배분합니다.
            </p>
            <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
              {PRESET_HOURS.map(h => (
                <button
                  key={h}
                  type="button"
                  onClick={() => { setHours(h); setUseCustom(false) }}
                  style={{
                    flex: 1, padding: '10px 0', borderRadius: 8, fontSize: 13, fontWeight: 600,
                    border: `1.5px solid ${!useCustom && hours === h ? 'var(--ai)' : 'var(--border)'}`,
                    background: !useCustom && hours === h ? 'var(--ai-bg, #eff6ff)' : 'var(--surface)',
                    color: !useCustom && hours === h ? 'var(--ai)' : 'inherit',
                    cursor: 'pointer', transition: 'all 0.15s',
                  }}
                >
                  {h}h
                </button>
              ))}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <input
                type="number"
                min={1}
                max={168}
                placeholder="직접 입력 (h)"
                value={customHours}
                onFocus={() => setUseCustom(true)}
                onChange={e => { setCustomHours(e.target.value); setUseCustom(true) }}
                style={{
                  flex: 1, padding: '9px 12px', borderRadius: 8, fontSize: 13,
                  border: `1.5px solid ${useCustom ? 'var(--ai)' : 'var(--border)'}`,
                  outline: 'none',
                }}
              />
              <span style={{ fontSize: 13, color: 'var(--muted)', whiteSpace: 'nowrap' }}>시간 / 주</span>
            </div>
          </div>

          {/* 기술 스택 */}
          <div className="field" style={{ marginBottom: 28 }}>
            <label style={{ fontWeight: 600, marginBottom: 6, display: 'block' }}>
              기술 스택 <span style={{ fontWeight: 400, color: 'var(--muted)', fontSize: 12 }}>(선택)</span>
            </label>
            <p style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 10 }}>
              보유한 기술을 선택하면 AI가 적합한 작업을 배정합니다.
            </p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 10 }}>
              {SKILL_SUGGESTIONS.map(s => (
                <button
                  key={s}
                  type="button"
                  onClick={() => toggleSkill(s)}
                  className={'chip' + (skills.includes(s) ? ' active' : '')}
                  style={{ fontSize: 12 }}
                >
                  {s}
                </button>
              ))}
            </div>
            <input
              type="text"
              placeholder="직접 입력 후 Enter (예: Kotlin, Swift)"
              value={skillInput}
              onChange={e => setSkillInput(e.target.value)}
              onKeyDown={addCustomSkill}
              style={{
                width: '100%', padding: '9px 12px', borderRadius: 8, fontSize: 13,
                border: '1.5px solid var(--border)', outline: 'none', boxSizing: 'border-box',
              }}
            />
            {skills.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
                {skills.map(s => (
                  <span key={s} className="badge" style={{ fontSize: 12, cursor: 'pointer' }}
                    onClick={() => toggleSkill(s)}>
                    {s} ×
                  </span>
                ))}
              </div>
            )}
          </div>

          {error && <div className="auth-error">⚠ {error}</div>}

          <div style={{ display: 'flex', gap: 8 }}>
            <button
              type="button"
              className="btn"
              style={{ flex: 1, justifyContent: 'center' }}
              onClick={async () => {
                await memberApi.updateMe({ weeklyCapacityHours: 40, skills: [] })
                await refreshUser()
                navigate('/', { replace: true })
              }}
            >
              나중에 설정
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ flex: 2, justifyContent: 'center' }}
              disabled={loading}
            >
              {loading ? '저장 중…' : '시작하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
