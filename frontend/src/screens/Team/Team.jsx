import './Team.css'
import Avatar from '../../ui/Avatar/Avatar'
import ProgressBar from '../../ui/ProgressBar/ProgressBar'

export default function Team({ members, workloads, role }) {
  const isMember = role === 'member'
  // memberId(string) → workload
  const wl = new Map((workloads || []).map(w => [String(w.memberId), w]))

  const highest = [...(workloads || [])].sort((a, b) => (b.loadRate ?? 0) - (a.loadRate ?? 0))[0]
  const lowest  = [...(workloads || [])].sort((a, b) => (a.loadRate ?? 0) - (b.loadRate ?? 0))[0]

  return (
    <div className="page" data-screen-label="Team">
      <div className="page-head">
        <div>
          <h1>팀</h1>
          <p className="lede">{members.length}명 · {isMember ? '읽기 전용' : 'AI가 부하를 모니터링합니다'}</p>
        </div>
      </div>

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
