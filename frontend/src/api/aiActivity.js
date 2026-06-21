// AI 실행 내역 스토어
// 앱에서 실제로 호출되는 AI 작업(작업 분해·회의 요약 등)을 추적한다.
// 호출 시작 → 'running', 완료 → 'done', 실패 → 'error' 로 상태가 바뀐다.
// AIThread가 useSyncExternalStore로 구독해 "실행 중/완료" 내역만 표시한다.
// 서버의 자동 모니터링 이력은 syncFromServer()로 60초마다 병합한다.

import { api } from './client.js'

const KEY = 'tf_ai_activity'
const MAX = 20

let items = load()
const subs = new Set()
let seq = 0

function load() {
  try {
    const all = JSON.parse(localStorage.getItem(KEY) || '[]')
    // 작업 분해는 자동 모니터링 대상이 아니므로 기존 기록에서 제거
    return all.filter(i => i.label !== 'AI 작업 분해')
  } catch { return [] }
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

/** 서버의 자동 모니터링·회의 요약 이력을 가져와 로컬 항목과 병합한다. */
export async function syncFromServer() {
  try {
    const data = await api.get('/ai/activities')
    if (!Array.isArray(data) || data.length === 0) return
    const serverItems = data.map(item => ({
      id: `srv_${item.id}`,
      label: item.projectName ? `[${item.projectName}] ${item.label}` : item.label,
      status: 'done',
      finishedAt: new Date(item.createdAt).getTime(),
      startedAt:  new Date(item.createdAt).getTime(),
      source: 'server',
    }))
    const serverIds = new Set(serverItems.map(i => i.id))
    const localItems = items.filter(i => !serverIds.has(i.id))
    items = [...localItems, ...serverItems]
      .sort((a, b) => (b.finishedAt || b.startedAt || 0) - (a.finishedAt || a.startedAt || 0))
      .slice(0, MAX)
    emit()
  } catch { /* 서버 조회 실패 무시 — 로컬 항목 유지 */ }
}
