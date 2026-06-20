import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import '../auth.css'

const ROLES = ['PM', 'Frontend', 'Backend', 'Designer', 'QA']

export default function Signup() {
  const navigate = useNavigate()
  const { register } = useAuth()
  const [searchParams] = useSearchParams()
  const inviteToken = searchParams.get('token') || undefined

  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '', role: 'Frontend' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const set = (k, v) => setForm(p => ({ ...p, [k]: v }))

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.name.trim()) { setError('이름을 입력해주세요.'); return }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) { setError('올바른 이메일 형식이 아닙니다.'); return }
    if (form.password.length < 8) { setError('비밀번호는 8자 이상이어야 합니다.'); return }
    if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) { setError('비밀번호는 영문과 숫자를 모두 포함해야 합니다.'); return }
    if (form.password !== form.confirm) { setError('비밀번호가 일치하지 않습니다.'); return }
    setError(''); setLoading(true)
    const result = await register(form.name.trim(), form.email.trim(), form.password, form.role, inviteToken)
    setLoading(false)
    if (result.ok) navigate('/', { replace: true })
    else setError(result.message)
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-brand-row">
          <div className="auth-brand-mark"></div>
          <div className="brand-name" style={{ fontSize: 16, fontWeight: 600 }}>TeamFlow<span className="ai">AI</span></div>
        </div>
        <h1 className="auth-title">회원가입</h1>
        {inviteToken && (
          <div className="auth-info" style={{ background: 'var(--ai-bg, #eff6ff)', border: '1px solid var(--ai)', borderRadius: 8, padding: '8px 12px', marginBottom: 8, fontSize: 13 }}>
            초대 링크로 접속했습니다. 가입 후 팀 워크스페이스에 합류합니다.
          </div>
        )}
        <p className="auth-sub">이미 계정이 있으신가요? <Link to={inviteToken ? `/login?token=${inviteToken}` : '/login'} style={{ color: 'var(--ai)', fontWeight: 500 }}>로그인</Link></p>

        <form onSubmit={handleSubmit} noValidate>
          <div className="field"><label>이름</label>
            <input value={form.name} onChange={e => set('name', e.target.value)} placeholder="홍길동" autoComplete="name" /></div>
          <div className="field"><label>이메일</label>
            <input type="email" value={form.email} onChange={e => set('email', e.target.value)} placeholder="example@email.com" autoComplete="email" /></div>
          <div className="field">
            <label>역할</label>
            <div className="chips auth-role-grid">
              {ROLES.map(r => (
                <button key={r} type="button" className={'chip' + (form.role === r ? ' active' : '')} onClick={() => set('role', r)}>{r}</button>
              ))}
            </div>
          </div>
          <div className="field"><label>비밀번호 (영문+숫자, 8자 이상)</label>
            <input type="password" value={form.password} onChange={e => set('password', e.target.value)} autoComplete="new-password" /></div>
          <div className="field"><label>비밀번호 확인</label>
            <input type="password" value={form.confirm} onChange={e => set('confirm', e.target.value)} autoComplete="new-password" /></div>

          {error && <div className="auth-error">⚠ {error}</div>}

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
            {loading ? '가입 중…' : '가입하기'}
          </button>
        </form>
      </div>
    </div>
  )
}
