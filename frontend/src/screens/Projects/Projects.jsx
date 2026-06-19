import './Projects.css'
import Avatar from '../../ui/Avatar/Avatar'
import ProgressBar from '../../ui/ProgressBar/ProgressBar'
import HealthBadge from '../../ui/HealthBadge/HealthBadge'
import { daysFromNow } from '../../utils/date'

export default function Projects({ projects, members, openProject, openCreate, role, currentUser }) {
  const isMember = role === 'member'
  const visible = isMember ? projects.filter(p => p.members.includes(currentUser.id)) : projects

  return (
    <div className="page" data-screen-label="Projects">
      <div className="page-head">
        <div>
          <h1>프로젝트</h1>
          <p className="lede">
            {isMember
              ? `${currentUser.name} 님이 참여 중인 ${visible.length}개`
              : `${visible.length}개 활성 · AI가 자동 분해하고 일정을 잡아줍니다`}
          </p>
        </div>
        <div className="page-head-actions">
          {!isMember && (
            <button className="btn btn-primary" onClick={openCreate}>+ 새 프로젝트</button>
          )}
        </div>
      </div>

      <div className="card" style={{ padding: 0, borderTop: '1px solid var(--line)' }}>
        <div className="row" style={{ padding: '12px 0', borderBottom: '1px solid var(--line)', fontFamily: 'var(--mono)', fontSize: 10.5, textTransform: 'uppercase', color: 'var(--muted)', letterSpacing: '0.06em' }}>
          <div style={{ flex: 2 }}>프로젝트</div>
          <div style={{ width: 140 }}>진행률</div>
          <div style={{ width: 120 }}>팀</div>
          <div style={{ width: 90 }}>마감</div>
          <div style={{ width: 80 }}>상태</div>
        </div>
        {visible.map(p => {
          const m = members.filter(mb => p.members.includes(mb.id))
          return (
            <button
              key={p.id}
              onClick={() => openProject(p.id)}
              style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '14px 0', border: 0, borderBottom: '1px solid var(--line)', background: 'none', width: '100%', textAlign: 'left', cursor: 'pointer' }}
            >
              <div style={{ flex: 2, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 2 }}>{p.name}</div>
                <div className="muted" style={{ fontSize: 12, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.goal}</div>
              </div>
              <div style={{ width: 140 }}>
                <div className="row gap-sm" style={{ marginBottom: 4 }}>
                  <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>{p.done}/{p.tasks}</span>
                  <span className="mono" style={{ fontSize: 11, marginLeft: 'auto' }}>{Math.round(p.progress * 100)}%</span>
                </div>
                <ProgressBar value={p.progress} kind={p.health === 'warn' ? 'warn' : ''} />
              </div>
              <div style={{ width: 120, display: 'flex', gap: 4 }}>
                {m.slice(0, 4).map(mm => <Avatar key={mm.id} member={mm} size={22} />)}
                {m.length > 4 && <span className="tiny" style={{ alignSelf: 'center' }}>+{m.length - 4}</span>}
              </div>
              <div style={{ width: 90 }} className="mono muted">D-{daysFromNow(p.deadline)}</div>
              <div style={{ width: 80 }}><HealthBadge health={p.health} /></div>
            </button>
          )
        })}
        {visible.length === 0 && (
          <div className="placeholder" style={{ border: 0, padding: 32 }}>
            <div className="mono">EMPTY</div>
            <div>{isMember ? '참여 중인 프로젝트가 없습니다.' : '프로젝트가 없습니다. 새 프로젝트를 만들어 보세요.'}</div>
          </div>
        )}
      </div>
    </div>
  )
}
