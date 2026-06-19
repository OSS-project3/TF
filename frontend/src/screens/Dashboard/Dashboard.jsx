import './Dashboard.css'
import Avatar from '../../ui/Avatar/Avatar'
import ProgressBar from '../../ui/ProgressBar/ProgressBar'
import HealthBadge from '../../ui/HealthBadge/HealthBadge'
import Segmented from '../../ui/Segmented/Segmented'
import { daysFromNow } from '../../utils/date'

export default function Dashboard({ projects, members, openProject, openCreate, pm }) {
  const totalTasks  = pm ? pm.totalTaskCount : projects.reduce((a, p) => a + p.tasks, 0)
  const doneTasks   = pm ? pm.doneTaskCount  : projects.reduce((a, p) => a + p.done, 0)
  const lateTasks   = pm ? pm.lateTaskCount  : projects.reduce((a, p) => a + p.late, 0)
  const avgProgress = pm ? pm.averageProgress
    : (projects.length ? projects.reduce((a, p) => a + p.progress, 0) / projects.length : 0)
  const memberCount = pm ? pm.memberCount : members.length
  const avgLoad = pm ? pm.averageLoadRate : 0

  return (
    <div className="page" data-screen-label="Dashboard">
      <div className="page-head">
        <div>
          <h1>오늘의 워크스페이스</h1>
          <p className="lede">활성 프로젝트 {projects.length}개</p>
        </div>
        <div className="page-head-actions">
          <button className="btn btn-primary" onClick={openCreate}>+ 새 프로젝트</button>
        </div>
      </div>

      <div className="grid grid-4" style={{ marginBottom: 24 }}>
        <div className="card stat">
          <div className="stat-label">전체 진행률</div>
          <div className="stat-value">{Math.round(avgProgress * 100)}%</div>
          <div className="stat-delta">전체 프로젝트 평균</div>
        </div>
        <div className="card stat">
          <div className="stat-label">완료 / 전체</div>
          <div className="stat-value">
            {doneTasks}<span style={{ color: 'var(--muted-2)', fontSize: 18 }}> / {totalTasks}</span>
          </div>
          <div className="stat-delta">작업 기준</div>
        </div>
        <div className="card stat">
          <div className="stat-label">지연 작업</div>
          <div className="stat-value">{lateTasks}</div>
          <div className={'stat-delta' + (lateTasks > 0 ? ' down' : '')}>{lateTasks > 0 ? '▼ 검토 필요' : '없음'}</div>
        </div>
        <div className="card stat">
          <div className="stat-label">팀 멤버</div>
          <div className="stat-value">{memberCount}</div>
          <div className="stat-delta">평균 부하 {Math.round(avgLoad * 100)}%</div>
        </div>
      </div>

      <div className="card ai-callout" style={{ marginBottom: 24 }}>
        <div className="row">
          <span className="badge ai">✸ AI 인사이트</span>
          <div style={{ fontSize: 13.5, color: 'var(--ink)' }}>
            {lateTasks > 0
              ? <>지연 작업 <strong>{lateTasks}건</strong>이 있어요. <span style={{ color: 'var(--muted)' }}>부하가 높은 팀원의 작업을 재배정하면 일정을 앞당길 수 있어요.</span></>
              : <>현재 지연 작업이 없습니다. <span style={{ color: 'var(--muted)' }}>전체 평균 진행률은 {Math.round(avgProgress * 100)}%입니다.</span></>}
          </div>
        </div>
      </div>

      <div className="row" style={{ marginBottom: 14 }}>
        <h2 style={{ fontSize: 16, fontWeight: 600, margin: 0 }}>진행 중인 프로젝트</h2>
        <span className="tiny">{projects.length}</span>
        <div style={{ marginLeft: 'auto' }}>
          <Segmented value="all" onChange={() => {}} options={[{ value: 'all', label: '전체' }]} />
        </div>
      </div>

      <div className="grid grid-3">
        {projects.map(p => {
          const m = members.filter(mb => p.members.includes(mb.id))
          const days = daysFromNow(p.deadline)
          return (
            <button
              key={p.id}
              className="card"
              style={{ textAlign: 'left', cursor: 'pointer', background: 'transparent', border: 0 }}
              onClick={() => openProject(p.id)}
            >
              <div className="row" style={{ marginBottom: 10 }}>
                <HealthBadge health={p.health} />
                <span className="tiny" style={{ marginLeft: 'auto' }}>D-{days}</span>
              </div>
              <h3 style={{ margin: '0 0 4px', fontSize: 15, fontWeight: 600, letterSpacing: '-0.01em' }}>{p.name}</h3>
              <p className="muted" style={{ margin: '0 0 14px', fontSize: 12.5, lineHeight: 1.5, minHeight: 38 }}>{p.goal}</p>
              <div className="row gap-sm" style={{ marginBottom: 8 }}>
                <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>{p.done} / {p.tasks}</span>
                <span className="mono" style={{ fontSize: 11, color: 'var(--ink)', marginLeft: 'auto' }}>{Math.round(p.progress * 100)}%</span>
              </div>
              <ProgressBar value={p.progress} kind={p.health === 'warn' ? 'warn' : ''} />
              <div className="row" style={{ marginTop: 14 }}>
                <div className="row gap-sm">
                  {m.slice(0, 4).map(mm => <Avatar key={mm.id} member={mm} size={22} />)}
                  {m.length > 4 && <span className="tiny">+{m.length - 4}</span>}
                </div>
                {p.late > 0 && (
                  <span className="badge bad" style={{ marginLeft: 'auto' }}>{p.late} 지연</span>
                )}
              </div>
            </button>
          )
        })}
        {projects.length === 0 && (
          <div className="placeholder" style={{ border: 0, padding: 32 }}>
            <div className="mono">EMPTY</div>
            <div>활성 프로젝트가 없습니다.</div>
          </div>
        )}
      </div>
    </div>
  )
}
