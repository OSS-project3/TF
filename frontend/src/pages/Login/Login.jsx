import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import '../auth.css'

const DEMO = [
  { email: 'admin@teamflow.ai', password: 'admin1234', label: '관리자(PM)' },
  { email: 'demo@teamflow.ai', password: 'demo1234', label: '일반 사용자' },
]

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()

  const [form, setForm] = useState({ email: '', password: '' })
  const [remember, setRemember] = useState(true)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const from = location.state?.from?.pathname || '/'

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.email || !form.password) { setError('이메일과 비밀번호를 입력해주세요.'); return }
    setError(''); setLoading(true)
    const result = await login(form.email, form.password, remember)
    setLoading(false)
    if (result.ok) navigate(from, { replace: true })
    else setError(result.message)
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-brand-row">
          <div className="auth-brand-mark"></div>
          <div className="brand-name" style={{ fontSize: 16, fontWeight: 600 }}>TeamFlow<span className="ai">AI</span></div>
        </div>
        <h1 className="auth-title">로그인</h1>
        <p className="auth-sub">계정이 없으신가요? <Link to="/signup" style={{ color: 'var(--ai)', fontWeight: 500 }}>회원가입</Link></p>

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label>이메일</label>
            <input type="email" placeholder="example@email.com" value={form.email}
              onChange={e => setForm(p => ({ ...p, email: e.target.value }))} autoComplete="email" />
          </div>
          <div className="field">
            <label>비밀번호</label>
            <input type="password" placeholder="비밀번호 입력" value={form.password}
              onChange={e => setForm(p => ({ ...p, password: e.target.value }))} autoComplete="current-password" />
          </div>

          <label className="auth-remember">
            <input type="checkbox" checked={remember} onChange={e => setRemember(e.target.checked)} />
            <span>자동 로그인</span>
          </label>

          {error && <div className="auth-error">⚠ {error}</div>}

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
            {loading ? '로그인 중…' : '로그인'}
          </button>
        </form>

        <div className="auth-divider">데모 계정으로 빠르게 체험</div>
        <div className="demo-row">
          {DEMO.map(acc => (
            <button key={acc.email} type="button" className="btn"
              onClick={() => { setForm({ email: acc.email, password: acc.password }); setError('') }}>
              <span>{acc.label}</span>
              <small>{acc.email}</small>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
