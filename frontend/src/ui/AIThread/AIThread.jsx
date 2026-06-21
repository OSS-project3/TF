import { useSyncExternalStore, useEffect, useState } from 'react'
import './AIThread.css'
import { subscribe, getSnapshot, syncFromServer } from '../../api/aiActivity.js'

// 실제 AI 실행 내역만 표시한다 (목업 팁 제거).
// running = 실행 중, done = 완료, error = 실패
const STATUS = {
  running: { label: '실행 중', cls: 'running' },
  done:    { label: '완료',   cls: 'done' },
  error:   { label: '실패',   cls: 'error' },
}

function timeAgo(ts) {
  if (!ts) return ''
  const s = Math.floor((Date.now() - ts) / 1000)
  if (s < 60) return '방금'
  if (s < 3600) return `${Math.floor(s / 60)}분 전`
  if (s < 86400) return `${Math.floor(s / 3600)}시간 전`
  return `${Math.floor(s / 86400)}일 전`
}

export default function AIThread() {
  const items = useSyncExternalStore(subscribe, getSnapshot)
  const [, setTick] = useState(0)

  // 서버 이력 초기 로드 + 60초 폴링
  useEffect(() => {
    syncFromServer()
    const id = setInterval(syncFromServer, 60000)
    return () => clearInterval(id)
  }, [])

  // 상대 시간 주기적 갱신 (항목이 있을 때만)
  useEffect(() => {
    if (items.length === 0) return
    const id = setInterval(() => setTick(t => t + 1), 30000)
    return () => clearInterval(id)
  }, [items.length])

  const hasRunning = items.some(i => i.status === 'running')

  return (
    <aside className="ai-thread" data-screen-label="AI Thread">
      <div className="ai-thread-head">
        <span className="ai-dot"></span>
        AI 실행 내역
      </div>

      {items.length === 0 && (
        <div className="ai-msg" style={{ color: 'var(--muted)', borderBottom: 0 }}>
          아직 실행된 AI 작업이 없습니다.
          <div className="tiny" style={{ marginTop: 6, color: 'var(--muted-2)' }}>
            작업 분해·회의 요약 등 AI 기능을 실행하면 여기에 표시됩니다.
          </div>
        </div>
      )}

      {items.map(it => {
        const st = STATUS[it.status] ?? STATUS.done
        return (
          <div className="ai-msg fade-in" key={it.id}>
            <time>
              <span className={'ai-act-tag ' + st.cls}>
                {it.status === 'running' && <span className="spin" style={{ marginRight: 5, verticalAlign: 'middle' }} />}
                {st.label}
              </span>
              {it.status !== 'running' && ` · ${timeAgo(it.finishedAt || it.startedAt)}`}
            </time>
            {it.label}
          </div>
        )
      })}

      {hasRunning && (
        <div style={{ marginTop: 'auto', paddingTop: 14, borderTop: '1px solid var(--line)' }}>
          <div className="thinking-line"></div>
          <div className="tiny" style={{ marginTop: 8 }}>AI가 실행 중입니다…</div>
        </div>
      )}
    </aside>
  )
}
