import './AIThread.css'

// 탭/역할 기반 정적 컨텍스트 메시지 (목업 데이터 파일 미사용)
const PM = {
  dashboard: [
    { t: '09:02', tag: 'INSIGHT', body: '활성 프로젝트의 평균 진행률과 지연 작업을 모니터링하고 있습니다.' },
    { t: '09:01', tag: 'TIP', body: '부하가 높은 팀원의 작업을 재배정하면 일정을 단축할 수 있어요.' },
  ],
  projects: [
    { t: '09:00', tag: 'PLAN', body: '새 프로젝트를 만들면 목표를 분석해 작업으로 자동 분해합니다.' },
  ],
  detail: [
    { t: '09:03', tag: 'AUTO', body: '작업 목록과 팀 부하를 실시간으로 분석하고 있습니다.' },
  ],
  team: [
    { t: '09:00', tag: 'BALANCE', body: '팀원별 주간 부하를 비교해 균형 상태를 확인하세요.' },
  ],
  meetings: [
    { t: '09:00', tag: 'SUMMARY', body: '회의 노트를 붙여넣으면 요약과 액션 아이템을 추출합니다.' },
  ],
  schedule: [
    { t: '09:00', tag: 'PLAN', body: '마감일과 의존성을 반영해 일정을 배치합니다.' },
  ],
}
const MEMBER = {
  dashboard: [
    { t: '09:02', tag: 'FOCUS', body: '오늘 가장 중요한 작업부터 처리하도록 추천합니다.' },
    { t: '09:01', tag: 'NUDGE', body: '이번 주 부하를 고려해 작업 순서를 제안할 수 있어요.' },
  ],
  projects: [{ t: '09:00', tag: 'INFO', body: '참여 중인 프로젝트의 진행 상황을 확인하세요.' }],
  detail: [{ t: '09:03', tag: 'FOCUS', body: '본인 담당 작업을 강조해서 보여드립니다.' }],
  team: [{ t: '09:00', tag: 'INFO', body: '팀 구성과 역할을 확인할 수 있어요.' }],
  meetings: [{ t: '09:00', tag: 'SUMMARY', body: '회의록을 요약하고 내 할 일을 정리합니다.' }],
  schedule: [{ t: '09:00', tag: 'INFO', body: '내 일정을 타임라인으로 확인하세요.' }],
}

export default function AIThread({ tab, role }) {
  const src = role === 'member' ? MEMBER : PM
  const messages = src[tab] ?? src['dashboard'] ?? []

  return (
    <aside className="ai-thread" data-screen-label="AI Thread">
      <div className="ai-thread-head">
        <span className="ai-dot"></span>
        AI 스레드
      </div>
      {messages.map((m, i) => (
        <div className="ai-msg fade-in" key={i} style={{ animationDelay: `${i * 80}ms` }}>
          <time>{m.t} · <span className="tag">{m.tag}</span></time>
          {m.body}
        </div>
      ))}
      <div style={{ marginTop: 'auto', paddingTop: 14, borderTop: '1px solid var(--line)' }}>
        <div className="thinking-line"></div>
        <div className="tiny" style={{ marginTop: 8 }}>실시간으로 추론 중…</div>
      </div>
    </aside>
  )
}
