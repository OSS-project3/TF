// AI 실행 내역 스토어
// 앱에서 실제로 호출되는 AI 작업(작업 분해·회의 요약 등)을 추적한다.
// 호출 시작 → 'running', 완료 → 'done', 실패 → 'error' 로 상태가 바뀐다.
// AIThread가 useSyncExternalStore로 구독해 "실행 중/완료" 내역만 표시한다.

const KEY = 'tf_ai_activity'
const MAX = 20

let items = load()
const subs = new Set()
let seq = 0

function load() {
  try { return JSON.parse(localStorage.getItem(KEY) || '[]') } catch { return [] }
}
function persist() {
  // 완료/실패만 저장 (running은 휘발성 — 새로고침 시 사라짐)
  try {
    localStorage.setItem(KEY, JSON.stringify(items.filter(i => i.status !== 'running').slice(0, MAX)))
  } catch { /* 저장 실패 무시 */ }
}
function emit() { subs.forEach(fn => fn()) }

export function subscribe(fn) { subs.add(fn); return () => subs.delete(fn) }
export function getSnapshot() { return items }

/**
 * AI 호출을 추적한다. promise를 그대로 통과시키므로 호출부는 영향 없음.
 * @param {string} label 표시할 작업명 (예: 'AI 작업 분해')
 * @param {Promise} promise 실제 AI API 호출 promise
 */
export function track(label, promise) {
  const id = `${Date.now()}_${seq++}`
  items = [{ id, label, status: 'running', startedAt: Date.now() }, ...items].slice(0, MAX)
  emit()
  return Promise.resolve(promise).then(
    (res) => { settle(id, 'done'); return res },
    (err) => { settle(id, 'error'); throw err },
  )
}

function settle(id, status) {
  items = items.map(i => i.id === id ? { ...i, status, finishedAt: Date.now() } : i)
  persist(); emit()
}
