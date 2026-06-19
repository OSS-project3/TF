import CheckMark from '../../ui/CheckMark/CheckMark'

export default function TaskRow({ t, done, onToggle, projectName, highlight, muted }) {
  return (
    <div
      className={'task' + (done ? ' done' : '')}
      style={{
        margin: '0 -18px',
        padding: '12px 18px',
        opacity: muted ? 0.6 : 1,
        background: highlight && !done ? 'var(--ai-soft)' : 'transparent',
      }}
    >
      <button className={'task-check' + (done ? ' done' : '')} onClick={() => onToggle(t.id)}>
        <CheckMark />
      </button>
      <div className="task-title">
        <span className="badge" style={{ fontSize: 10 }}>{t.phase}</span>
        <span className="t">{t.title}</span>
      </div>
      <span className="task-meta">{t.hours}h</span>
      <span className="task-meta" style={{
        color: t.difficulty === 'hard' ? 'var(--bad)' : t.difficulty === 'medium' ? 'var(--warn)' : 'var(--ok)',
      }}>
        {t.difficulty}
      </span>
      <span className="task-assignee">
        {projectName && <span className="muted mono" style={{ fontSize: 10.5 }}>{projectName}</span>}
      </span>
    </div>
  )
}
