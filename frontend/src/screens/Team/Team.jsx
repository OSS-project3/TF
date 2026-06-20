import { useEffect, useState } from 'react'
import './Team.css'
import Avatar from '../../ui/Avatar/Avatar'
import ProgressBar from '../../ui/ProgressBar/ProgressBar'
import { invitationApi } from '../../api'
import { useAuth } from '../../context/AuthContext.jsx'

export default function Team({ members, workloads, role, onReload }) {
  const { refreshUser } = useAuth()
  const isMember = role === 'member'
  // memberId(string) → workload
  const wl = new Map((workloads || []).map(w => [String(w.memberId), w]))

  const highest = [...(workloads || [])].sort((a, b) => (b.loadRate ?? 0) - (a.loadRate ?? 0))[0]
  const lowest  = [...(workloads || [])].sort((a, b) => (a.loadRate ?? 0) - (b.loadRate ?? 0))[0]

  // ── 이메일 초대 / 받은 참가 요청 ──
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteMsg, setInviteMsg] = useState(null)
  const [inviting, setInviting] = useState(false)
  const [received, setReceived] = useState([])
  const [busyId, setBusyId] = useState(null)

  async function loadReceived() {
    try { setReceived(await invitationApi.getReceivedInvitations() ?? []) }
    catch { setReceived([]) }
  }
  useEffect(() => { loadReceived() }, [])

  async function sendInvite(e) {
    e.preventDefault()
    const email = inviteEmail.trim()
    if (!email) return
    setInviting(true); setInviteMsg(null)
    try {
      await invitationApi.inviteByEmail(email)
      setInviteEmail('')
      setInviteMsg({ ok: true, text: `${email} 님에게 참가 요청을 보냈습니다.` })
    } catch (e) {
      setInviteMsg({ ok: false, text: e?.message || '초대 발송에 실패했습니다.' })
    } finally { setInviting(false) }
  }

  async function acceptInvite(id) {
    setBusyId(id)
    try {
      await invitationApi.acceptReceivedInvitation(id) // 새 JWT 저장됨
      await refreshUser()      // 세션(워크스페이스) 갱신
      await loadReceived()     // 처리된 요청 제거
      await onReload?.()       // 멤버·프로젝트 등 워크스페이스 데이터 재로딩
    } catch (e) {
      alert(e?.message || '참가 요청 수락에 실패했습니다.')
    } finally { setBusyId(null) }
  }

  async function rejectInvite(id) {
    setBusyId(id)
    try {
      await invitationApi.rejectReceivedInvitation(id)
      await loadReceived()
    } catch (e) {
      alert(e?.message || '참가 요청 거절에 실패했습니다.')
    } finally { setBusyId(null) }
  }

  return (
    <div className="page" data-screen-label="Team">
      <div className="page-head">
        <div>
          <h1>팀</h1>
          <p className="lede">{members.length}명 · {isMember ? '읽기 전용' : 'AI가 부하를 모니터링합니다'}</p>
        </div>
      </div>

      {/* 받은 참가 요청 — 모든 사용자에게 표시 */}
      {received.length > 0 && (
        <div className="card" style={{ marginBottom: 20, borderColor: 'var(--ai)' }}>
          <div className="card-head"><h3>받은 참가 요청 <span className="badge ai">{received.length}</span></h3></div>
          <div className="col gap-sm">
            {received.map(inv => (
              <div key={inv.id} className="row" style={{ gap: 10, padding: '8px 0', borderTop: '1px solid var(--line)' }}>
                <div style={{ fontSize: 13 }}>
                  <strong>{inv.inviterName}</strong> 님이 <strong>{inv.workspaceName}</strong> 워크스페이스에 초대했습니다.
                </div>
                <div className="row gap-sm" style={{ marginLeft: 'auto' }}>
                  <button className="btn btn-primary" disabled={busyId === inv.id} onClick={() => acceptInvite(inv.id)}>
                    {busyId === inv.id ? '처리 중…' : '수락'}
                  </button>
                  <button className="btn" disabled={busyId === inv.id} onClick={() => rejectInvite(inv.id)}>거절</button>
                </div>
              </div>
            ))}
          </div>
          <p className="muted tiny" style={{ marginTop: 8 }}>
            수락하면 현재 워크스페이스에서 해당 워크스페이스로 이동합니다.
          </p>
        </div>
      )}

      {/* 이메일로 팀원 초대 — PM 전용 */}
      {!isMember && (
        <form className="card" onSubmit={sendInvite} style={{ marginBottom: 20 }}>
          <div className="card-head"><h3>팀원 초대</h3></div>
          <p className="muted" style={{ fontSize: 13, marginBottom: 12 }}>
            팀원의 계정 이메일을 입력하면, 해당 사용자가 본인 계정에서 참가 요청을 받습니다.
          </p>
          <div className="row gap-sm" style={{ flexWrap: 'wrap' }}>
            <input
              type="email"
              placeholder="member@example.com"
              value={inviteEmail}
              onChange={e => setInviteEmail(e.target.value)}
              style={{
                flex: 1, minWidth: 220,
                border: '1px solid var(--line-strong)',
                borderRadius: 'var(--radius)',
                padding: '9px 12px',
                fontSize: 14,
                outline: 'none',
                transition: 'border-color 0.12s ease',
              }}
              onFocus={e => { e.target.style.borderColor = 'var(--muted-2)' }}
              onBlur={e => { e.target.style.borderColor = 'var(--line-strong)' }}
            />
            <button type="submit" className="btn btn-primary" disabled={inviting || !inviteEmail.trim()}>
              {inviting ? '보내는 중…' : '참가 요청 보내기'}
            </button>
          </div>
          {inviteMsg && (
            <div className="tiny" style={{ color: inviteMsg.ok ? 'var(--ok)' : 'var(--bad)', marginTop: 8 }}>
              {inviteMsg.ok ? '✓' : '⚠'} {inviteMsg.text}
            </div>
          )}
        </form>
      )}

      {!isMember && highest && lowest && highest.memberId !== lowest.memberId && (
        <div className="card ai-callout" style={{ marginBottom: 20 }}>
          <div className="row">
            <span className="badge ai">✸ 균형 분석</span>
            <div style={{ fontSize: 13 }}>
              <strong>{highest.memberName}</strong> 님이 {Math.round((highest.loadRate ?? 0) * 100)}% 부하로 가장 높아요.
              {' '}<strong>{lowest.memberName}</strong> 님은 {Math.round((lowest.loadRate ?? 0) * 100)}%로 여유가 있습니다.
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-3">
        {members.map(m => {
          const w = wl.get(m.id)
          const pct = w?.loadRate ?? 0
          const assigned = w?.assignedHours ?? 0
          return (
            <div key={m.id} className="card">
              <div className="row" style={{ marginBottom: 14 }}>
                <Avatar member={m} size={40} />
                <div>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>{m.name}</div>
                  <div className="muted mono" style={{ fontSize: 11 }}>{m.role}</div>
                </div>
                <span className={'badge ' + (pct > 0.85 ? 'bad' : pct > 0.6 ? 'warn' : 'ok')} style={{ marginLeft: 'auto' }}>
                  {Math.round(pct * 100)}%
                </span>
              </div>

              <div className="col gap-sm">
                <div className="row" style={{ fontSize: 12 }}>
                  <span className="muted">주간 부하</span>
                  <span className="mono" style={{ marginLeft: 'auto' }}>{assigned}h / {m.hours}h</span>
                </div>
                <ProgressBar value={pct} kind={pct > 0.85 ? 'warn' : ''} />
              </div>

              <div className="divider"></div>

              <div className="row" style={{ fontSize: 12 }}>
                <span className="muted">담당 작업</span>
                <span className="mono" style={{ marginLeft: 'auto' }}>{w?.taskCount ?? 0}</span>
              </div>
              <div className="row" style={{ fontSize: 12, marginTop: 6 }}>
                <span className="muted">프로젝트</span>
                <span className="mono" style={{ marginLeft: 'auto' }}>{w?.projectCount ?? 0}</span>
              </div>

              <div className="row gap-sm" style={{ marginTop: 12, flexWrap: 'wrap' }}>
                {m.skills.map(s => <span key={s} className="badge" style={{ fontSize: 11 }}>{s}</span>)}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
