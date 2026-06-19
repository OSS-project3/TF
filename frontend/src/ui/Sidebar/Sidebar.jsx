import { useState } from 'react'
import './Sidebar.css'
import Avatar from '../Avatar/Avatar'

export default function Sidebar({ tab, setTab, counts, role, currentUser, onSettings, onLogout }) {
  const [menuOpen, setMenuOpen] = useState(false)

  const main = role === 'pm' ? [
    { id: 'dashboard', label: '대시보드', g: '◉' },
    { id: 'projects',  label: '프로젝트', g: '▣', count: counts.projects },
    { id: 'schedule',  label: '일정',     g: '▤' },
    { id: 'team',      label: '팀',       g: '◐', count: counts.team },
    { id: 'meetings',  label: '회의록',   g: '≡' },
  ] : [
    { id: 'dashboard', label: '내 할 일', g: '◉' },
    { id: 'projects',  label: '프로젝트', g: '▣', count: counts.projects },
    { id: 'schedule',  label: '내 일정',  g: '▤' },
    { id: 'team',      label: '팀',       g: '◐', count: counts.team },
    { id: 'meetings',  label: '회의록',   g: '≡' },
  ]

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark"></div>
        <div className="brand-name">TeamFlow<span className="ai">AI</span></div>
      </div>

      <nav className="nav">
        <div className="nav-section">{role === 'pm' ? 'Workspace' : '내 영역'}</div>
        {main.map(item => (
          <button
            key={item.id}
            className={'nav-item' + (tab === item.id ? ' active' : '')}
            onClick={() => setTab(item.id)}
          >
            <span className="nav-glyph">{item.g}</span>
            {item.label}
            {item.count != null && <span className="nav-count">{item.count}</span>}
          </button>
        ))}

        <div className="nav-section">AI</div>
        {role === 'pm' ? (
          <>
            <button className="nav-item" onClick={() => setTab('projects')}>
              <span className="nav-glyph" style={{ color: 'var(--ai)' }}>✸</span>
              새 프로젝트 분해
            </button>
            <button className="nav-item" onClick={() => setTab('meetings')}>
              <span className="nav-glyph" style={{ color: 'var(--ai)' }}>✸</span>
              회의록 요약
            </button>
          </>
        ) : (
          <>
            <button className="nav-item" onClick={() => setTab('dashboard')}>
              <span className="nav-glyph" style={{ color: 'var(--ai)' }}>✸</span>
              오늘 무엇부터?
            </button>
            <button className="nav-item" onClick={() => setTab('meetings')}>
              <span className="nav-glyph" style={{ color: 'var(--ai)' }}>✸</span>
              회의록 요약
            </button>
          </>
        )}
      </nav>

      <div className="sidebar-footer" style={{ position: 'relative' }}>
        <button
          onClick={() => setMenuOpen(o => !o)}
          style={{
            display: 'flex', alignItems: 'center', gap: 10, width: '100%',
            background: 'none', border: 0, padding: 4, borderRadius: 6,
            cursor: 'pointer', textAlign: 'left',
          }}
        >
          <Avatar member={currentUser} />
          <div className="user-meta" style={{ flex: 1, minWidth: 0 }}>
            <strong>{currentUser?.name}</strong>
            <span>{currentUser?.role} · {role === 'pm' ? 'PM 뷰' : '팀원 뷰'}</span>
          </div>
          <span style={{ fontSize: 11, color: 'var(--muted)' }}>⇅</span>
        </button>

        {menuOpen && (
          <div style={{
            position: 'absolute',
            bottom: 'calc(100% + 6px)', left: 12, right: 12,
            background: 'var(--bg)',
            border: '1px solid var(--line)',
            borderRadius: 8,
            padding: 6,
            zIndex: 20,
            boxShadow: '0 6px 24px rgba(0,0,0,0.06)',
          }}>
            <button className="nav-item" onClick={() => { setMenuOpen(false); onSettings?.() }}>
              <span className="nav-glyph">⚙</span> 설정
            </button>
            <button className="nav-item" onClick={() => { setMenuOpen(false); onLogout?.() }}>
              <span className="nav-glyph">⎋</span> 로그아웃
            </button>
          </div>
        )}
      </div>
    </aside>
  )
}
