import { useEffect, useState } from 'react'
import { memberApi, invitationApi, ApiError } from '../../api'
import { useAuth } from '../../context/AuthContext.jsx'

export default function Settings({ onDeleted }) {
  const { refreshUser, clearSession } = useAuth()
  const [loading, setLoading] = useState(true)
  const [profile, setProfile] = useState({ name: '', initial: '', weeklyCapacityHours: 40, skills: '' })
  const [profileMsg, setProfileMsg] = useState(null)
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [pwMsg, setPwMsg] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [inviteLink, setInviteLink] = useState(null)
  const [inviteLoading, setInviteLoading] = useState(false)
  const [inviteCopied, setInviteCopied] = useState(false)

  useEffect(() => {
    memberApi.getMe().then(me => {
      setProfile({ name: me.name ?? '', initial: me.initial ?? '', weeklyCapacityHours: me.weeklyCapacityHours ?? 40, skills: (me.skills ?? []).join(', ') })
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  async function saveProfile(e) {
    e.preventDefault(); setProfileMsg(null)
    try {
      await memberApi.updateMe({
        name: profile.name.trim(), initial: profile.initial.trim(),
        weeklyCapacityHours: Number(profile.weeklyCapacityHours),
        skills: profile.skills.split(',').map(s => s.trim()).filter(Boolean),
      })
      await refreshUser()
      setProfileMsg({ ok: true, text: '프로필이 저장되었습니다.' })
    } catch (e) { setProfileMsg({ ok: false, text: e?.message || '저장 실패' }) }
  }

  async function savePassword(e) {
    e.preventDefault(); setPwMsg(null)
    if (pw.newPassword.length < 8) { setPwMsg({ ok: false, text: '새 비밀번호는 8자 이상이어야 합니다.' }); return }
    if (pw.newPassword !== pw.confirm) { setPwMsg({ ok: false, text: '새 비밀번호가 일치하지 않습니다.' }); return }
    try {
      await memberApi.changePassword({ currentPassword: pw.currentPassword, newPassword: pw.newPassword })
      setPw({ currentPassword: '', newPassword: '', confirm: '' })
      setPwMsg({ ok: true, text: '비밀번호가 변경되었습니다.' })
    } catch (e) {
      const text = e instanceof ApiError && e.code === 'WRONG_PASSWORD' ? '현재 비밀번호가 올바르지 않습니다.' : (e?.message || '변경 실패')
      setPwMsg({ ok: false, text })
    }
  }

  async function generateInvite() {
    setInviteLoading(true); setInviteLink(null); setInviteCopied(false)
    try {
      const data = await invitationApi.createInvitation()
      setInviteLink(`${window.location.origin}/signup?token=${data.token}`)
    } catch (e) { alert(e?.message || '초대 링크 생성 실패') }
    finally { setInviteLoading(false) }
  }

  async function copyInvite() {
    if (!inviteLink) return
    try { await navigator.clipboard.writeText(inviteLink); setInviteCopied(true); setTimeout(() => setInviteCopied(false), 2000) }
    catch { alert('클립보드 복사에 실패했습니다.') }
  }

  async function deleteAccount() {
    try { await memberApi.deleteMe(); clearSession(); onDeleted?.() }
    catch (e) { setConfirmDelete(false); alert(e?.message || '회원 탈퇴 실패') }
  }

  if (loading) return <div className="page"><div className="placeholder" style={{ padding: 32 }}><div className="mono">LOADING</div></div></div>

  return (
    <div className="page" data-screen-label="Settings">
      <div className="page-head"><div><h1>설정</h1><p className="lede">프로필과 계정 정보를 관리합니다</p></div></div>

      <div style={{ maxWidth: 560, display: 'flex', flexDirection: 'column', gap: 20 }}>
        <form className="card" onSubmit={saveProfile}>
          <div className="card-head"><h3>프로필</h3></div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="field"><label>이름</label><input value={profile.name} onChange={e => setProfile(p => ({ ...p, name: e.target.value }))} /></div>
            <div className="field"><label>이니셜 (최대 2자)</label><input maxLength={2} value={profile.initial} onChange={e => setProfile(p => ({ ...p, initial: e.target.value }))} /></div>
          </div>
          <div className="field"><label>주간 가용 시간 (시간)</label><input type="number" min={1} value={profile.weeklyCapacityHours} onChange={e => setProfile(p => ({ ...p, weeklyCapacityHours: e.target.value }))} /></div>
          <div className="field"><label>보유 스킬 (쉼표로 구분)</label><input placeholder="React, TypeScript" value={profile.skills} onChange={e => setProfile(p => ({ ...p, skills: e.target.value }))} /></div>
          {profileMsg && <div className="tiny" style={{ color: profileMsg.ok ? 'var(--ok)' : 'var(--bad)', marginBottom: 8 }}>{profileMsg.ok ? '✓' : '⚠'} {profileMsg.text}</div>}
          <div className="row"><button type="submit" className="btn btn-primary" style={{ marginLeft: 'auto' }}>프로필 저장</button></div>
        </form>

        <form className="card" onSubmit={savePassword}>
          <div className="card-head"><h3>비밀번호 변경</h3></div>
          <div className="field"><label>현재 비밀번호</label><input type="password" value={pw.currentPassword} onChange={e => setPw(p => ({ ...p, currentPassword: e.target.value }))} /></div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="field"><label>새 비밀번호 (8자+)</label><input type="password" value={pw.newPassword} onChange={e => setPw(p => ({ ...p, newPassword: e.target.value }))} /></div>
            <div className="field"><label>새 비밀번호 확인</label><input type="password" value={pw.confirm} onChange={e => setPw(p => ({ ...p, confirm: e.target.value }))} /></div>
          </div>
          {pwMsg && <div className="tiny" style={{ color: pwMsg.ok ? 'var(--ok)' : 'var(--bad)', marginBottom: 8 }}>{pwMsg.ok ? '✓' : '⚠'} {pwMsg.text}</div>}
          <div className="row"><button type="submit" className="btn btn-primary" style={{ marginLeft: 'auto' }} disabled={!pw.currentPassword}>비밀번호 변경</button></div>
        </form>

        <div className="card">
          <div className="card-head"><h3>팀원 초대</h3></div>
          <p className="muted" style={{ fontSize: 13, marginBottom: 12 }}>초대 링크를 생성하여 팀원에게 공유하세요. 링크는 7일간 유효합니다.</p>
          <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
            <button className="btn btn-primary" onClick={generateInvite} disabled={inviteLoading}>
              {inviteLoading ? '생성 중…' : '초대 링크 생성'}
            </button>
            {inviteLink && (
              <button className="btn" onClick={copyInvite}>
                {inviteCopied ? '복사됨!' : '링크 복사'}
              </button>
            )}
          </div>
          {inviteLink && (
            <div style={{ marginTop: 10, padding: '8px 10px', background: 'var(--surface-2, #f5f5f5)', borderRadius: 6, fontSize: 12, wordBreak: 'break-all', color: 'var(--muted)' }}>
              {inviteLink}
            </div>
          )}
        </div>

        <div className="card" style={{ borderColor: 'var(--bad)' }}>
          <div className="card-head"><h3 style={{ color: 'var(--bad)' }}>위험 구역</h3></div>
          <p className="muted" style={{ fontSize: 13, marginBottom: 12 }}>계정을 삭제하면 모든 프로젝트에서 제외되며 되돌릴 수 없습니다.</p>
          <button className="btn" style={{ color: 'var(--bad)', borderColor: 'var(--bad)' }} onClick={() => setConfirmDelete(true)}>회원 탈퇴</button>
        </div>
      </div>

      {confirmDelete && (
        <div className="modal-bg" onClick={() => setConfirmDelete(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-head"><h2>정말 탈퇴하시겠습니까?</h2><p>되돌릴 수 없으며 계정 접근이 즉시 차단됩니다.</p></div>
            <div className="modal-foot">
              <button className="btn btn-ghost" onClick={() => setConfirmDelete(false)}>취소</button>
              <button className="btn btn-primary" style={{ background: 'var(--bad)' }} onClick={deleteAccount}>탈퇴 확인</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
