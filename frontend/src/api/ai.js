// AI API — /api/v1/ai/** (OpenAI 연동). 키 미설정 시 503 AI_DISABLED 에러.
import { api } from './client.js'
import { track } from './aiActivity.js'

/**
 * AI 작업 분해. POST /ai/decompositions
 * @param {{ goal: string, deadline?: string, memberIds?: number[] }} payload
 * @returns {Promise<{ reasoningMessages: string[],
 *   tasks: Array<{ title: string, phase: string, estimatedHours: number,
 *                  difficulty: 'EASY'|'MEDIUM'|'HARD',
 *                  startDate: string, endDate: string }> }>}
 *   startDate/endDate: AI가 현재 날짜와 마감일 기준으로 산정한 일정 (yyyy-MM-dd, 빈 값 가능)
 */
// track() 미사용 — 작업 분해는 프로젝트 생성 시 사용자가 직접 트리거하는 액션이므로
// AI 실행내역(자동 모니터링 전용)에 표시하지 않음
export const decompose = (payload) => api.post('/ai/decompositions', payload)

/**
 * AI 회의 요약. POST /ai/meeting-summaries
 * @param {{ notes: string, projectId?: number }} payload
 * @returns {Promise<{ summary: string[],
 *   todos: Array<{ title: string, assignee: string, dueDate: string }> }>}
 */
export const summarizeMeeting = (payload) => track('AI 회의 요약', api.post('/ai/meeting-summaries', payload))
