import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { memberApi } from '../../api'
import { useAuth } from '../../context/AuthContext.jsx'
import '../auth.css'

const ROLES = [
  { value: 'PM',       label: 'PM',       desc: '프로젝트 매니저 — 프로젝트 생성·관리 권한' },
  { value: 'Frontend', label: 'Frontend',  desc: '프론트엔드 개발자' },
  { value: 'Backend',  label: 'Backend',   desc: '백엔드 개발자' },
  { value: 'Designer', label: 'Designer',  desc: 'UI/UX 디자이너' },
  { value: 'QA',       label: 'QA',        desc: '품질 보증 엔지니어' },
]

const ROLE_MAP = { PM: 'PM', Frontend: 'FRONTEND', Backend: 'BACKEND', Designer: 'DESIGNER', QA: 'QA' }

export default function SetupRole() {
  const navigate = useNavigate()
  const { refreshUser } = useAuth()
  const [selected, setSelected] = useState('Frontend')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await memberApi.updateMe({ role: ROLE_MAP[selected] })
      await refreshUser()
      navigate('/setup-profile', { replace: true })
    } catch {
      setError('역할 설정에 실패했습니다. 다시 시도해주세요.')
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-brand-row">
          <div className="auth-brand-mark"></div>
          <div className="brand-name" style={{ fontSize: 16, fontWeight: 600 }}>TeamFlow<span className="ai">AI</span></div>
        </div>
        <h1 className="auth-title">역할 선택</h1>
        <p className="auth-sub" style={{ marginBottom: 20 }}>팀에서 맡은 역할을 선택해주세요. 나중에 설정에서 변경할 수 있습니다.</p>

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 20 }}>
            {ROLES.map(r => (
              <label
                key={r.value}
                style={{
                  display: 'flex', alignItems: 'center', gap: 12,
                  padding: '12px 14px', borderRadius: 8, cursor: 'pointer',
                  border: `1.5px solid ${selected === r.value ? 'var(--ai)' : 'var(--border)'}`,
                  background: selected === r.value ? 'var(--ai-bg, #eff6ff)' : 'var(--surface)',
                  transition: 'border-color 0.15s, background 0.15s',
                }}
              >
                <input
                  type="radio" name="role" value={r.value}
                  checked={selected === r.value}
                  onChange={() => setSelected(r.value)}
                  style={{ accentColor: 'var(--ai)', width: 16, height: 16, flexShrink: 0 }}
                />
                <div>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>{r.label}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>{r.desc}</div>
                </div>
              </label>
            ))}
          </div>

          {error && <div className="auth-error">⚠ {error}</div>}

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', justifyContent: 'center' }}
            disabled={loading}
          >
            {loading ? '저장 중…' : '시작하기'}
          </button>
        </form>
      </div>
    </div>
  )
}
