// AI API — /api/v1/ai/** (OpenAI 연동). 키 미설정 시 503 AI_DISABLED 에러.
import { api } from './client.js'

/**
 * AI 작업 분해. POST /ai/decompositions
 * @param {{ goal: string, deadline?: string, memberIds?: number[] }} payload
 * @returns {Promise<{ reasoningMessages: string[],
 *   tasks: Array<{ title: string, phase: string, estimatedHours: number,
 *                  difficulty: 'EASY'|'MEDIUM'|'HARD', dependencyTaskIds: number[] }> }>}
 */
export const decompose = (payload) => api.post('/ai/decompositions', payload)

/**
 * AI 회의 요약. POST /ai/meeting-summaries
 * @param {{ notes: string, projectId?: number }} payload
 * @returns {Promise<{ summary: string[],
 *   todos: Array<{ title: string, assignee: string, dueDate: string }> }>}
 */
export const summarizeMeeting = (payload) => api.post('/ai/meeting-summaries', payload)
