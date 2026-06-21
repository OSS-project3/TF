import { useEffect, useState } from 'react'
import './Team.css'
import Avatar from '../../ui/Avatar/Avatar'
import { invitationApi } from '../../api'
import { useAuth } from '../../context/AuthContext.jsx'

function loadColor(pct) {
  if (pct > 1.2) return 'var(--bad)'
  if (pct > 0.85) return 'var(--warn)'
  return 'var(--ok)'
}

function LoadBar({ pct }) {
  const capped = Math.min(pct, 2)
  const color = loadColor(pct)
  return (
    <div style={{ position: 'relative', height: 8, borderRadius: 4, background: 'var(--line)', flex: 1 }}>
      <div style={{
        position: 'absolute', left: 0, top: 0, height: '100%',
        width: `${Math.min(capped / 2, 1) * 100}%`,
        borderRadius: 4, background: color, transition: 'width 0.3s',
      }} />
      {/* 100% 기준선 */}
      <div style={{
        position: 'absolute', left: '50%', top: -3, bottom: -3,
        width: 1.5, background: 'var(--muted)', opacity: 0.4,
      }} />
    </div>
  )
}

export default function Team({ members, workloads, role, onReload }) {
  const { refreshUser } = useAuth()
  const isMember = role === 'member'
  const wl = new Map((workloads || []).map(w => [String(w.memberId), w]))

  const ranked = [...members]
    .map(m => ({ m, w: wl.get(String(m.id)) }))
    .sort((a, b) => ((b.w?.loadRate ?? 0) - (a.w?.loadRate ?? 0)))

  const highest = ranked[0]
  const lowest  = ranked[ranked.length - 1]

  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteMsg, setInviteMsg]     = useState(null)
  const [inviting, setInviting]       = useState(false)
  const [received, setReceived]       = useState([])
  const [busyId, setBusyId]           = useState(null)

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
      await invitationApi.acceptReceivedInvitation(id)
      await refreshUser()
      await loadReceived()
      await onReload?.()
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

      {/* 받은 참가 요청 */}
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
          <p className="muted tiny" style={{ marginTop: 8 }}>수락하면 현재 워크스페이스에서 해당 워크스페이스로 이동합니다.</p>
        </div>
      )}

      {/* 팀원 초대 — PM 전용 */}
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
                padding: '9px 12px', fontSize: 14, outline: 'none',
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

      {/* 균형 분석 callout */}
      {!isMember && highest && lowest && highest.m.id !== lowest.m.id && (
        <div className="card ai-callout" style={{ marginBottom: 20 }}>
          <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
            <span className="badge ai">✸ 균형 분석</span>
            <span style={{ fontSize: 13 }}>
              <strong>{highest.m.name}</strong>
              <span style={{ color: loadColor(highest.w?.loadRate ?? 0), fontWeight: 600 }}> {Math.round((highest.w?.loadRate ?? 0) * 100)}%</span>
              {' '}부하로 가장 높고,{' '}
              <strong>{lowest.m.name}</strong>
              <span style={{ color: loadColor(lowest.w?.loadRate ?? 0), fontWeight: 600 }}> {Math.round((lowest.w?.loadRate ?? 0) * 100)}%</span>
              {' '}로 여유가 있어요.
            </span>
          </div>
        </div>
      )}

      {/* 부하 랭킹 리스트 */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {/* 헤더 */}
        <div className="team-list-header">
          <span style={{ flex: '0 0 200px' }}>팀원</span>
          <span style={{ flex: 1 }}>주간 부하 <span className="muted" style={{ fontWeight: 400, fontSize: 11 }}>(기준선 = 100%)</span></span>
          <span style={{ flex: '0 0 52px', textAlign: 'right' }}>부하율</span>
          <span style={{ flex: '0 0 80px', textAlign: 'right' }}>시간</span>
          <span style={{ flex: '0 0 60px', textAlign: 'right' }}>작업</span>
          <span style={{ flex: '0 0 60px', textAlign: 'right' }}>프로젝트</span>
        </div>

        {ranked.map(({ m, w }, i) => {
          const pct      = w?.loadRate ?? 0
          const assigned = w?.assignedHours ?? 0
          const capacity = w?.capacityHours ?? m.hours ?? 40
          const color   = loadColor(pct)
          return (
            <div key={m.id} className="team-list-row" style={{ borderTop: i === 0 ? 'none' : '1px solid var(--line)' }}>
              {/* 순위 + 아바타 + 이름 */}
              <div style={{ flex: '0 0 200px', display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ fontSize: 11, color: 'var(--muted)', width: 16, textAlign: 'center', flexShrink: 0 }}>
                  {i + 1}
                </span>
                <Avatar member={m} size={32} />
                <div>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{m.name}</div>
                  <div style={{ fontSize: 11, color: 'var(--muted)' }}>{m.role}</div>
                </div>
              </div>

              {/* 프로그레스 바 */}
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8 }}>
                <LoadBar pct={pct} />
              </div>

              {/* 부하율 */}
              <div style={{ flex: '0 0 52px', textAlign: 'right', fontWeight: 700, fontSize: 13, color }}>
                {Math.round(pct * 100)}%
              </div>

              {/* 시간 */}
              <div style={{ flex: '0 0 80px', textAlign: 'right', fontSize: 12, color: 'var(--muted)' }}>
                {assigned}h / {capacity}h
              </div>

              {/* 작업 수 */}
              <div style={{ flex: '0 0 60px', textAlign: 'right', fontSize: 12 }}>
                {w?.taskCount ?? 0}개
              </div>

              {/* 프로젝트 수 */}
              <div style={{ flex: '0 0 60px', textAlign: 'right', fontSize: 12 }}>
                {w?.projectCount ?? 0}개
              </div>
            </div>
          )
        })}
      </div>

      {/* 스킬 태그 섹션 */}
      {members.some(m => m.skills?.length > 0) && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-head"><h3>팀 스킬</h3></div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {members.map(m => m.skills?.length > 0 && (
              <div key={m.id} className="row" style={{ gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                <span style={{ fontSize: 12, fontWeight: 600, width: 80, flexShrink: 0 }}>{m.name}</span>
                {m.skills.map(s => <span key={s} className="badge" style={{ fontSize: 11 }}>{s}</span>)}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
