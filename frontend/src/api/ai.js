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
export const decompose = (payload) => track('AI 작업 분해', api.post('/ai/decompositions', payload))

/**
 * AI 회의 요약. POST /ai/meeting-summaries
 * @param {{ notes: string, projectId?: number }} payload
 * @returns {Promise<{ summary: string[],
 *   todos: Array<{ title: string, assignee: string, dueDate: string }> }>}
 */
export const summarizeMeeting = (payload) => track('AI 회의 요약', api.post('/ai/meeting-summaries', payload))
