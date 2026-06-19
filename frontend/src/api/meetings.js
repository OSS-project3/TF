// 회의록 API — /api/v1/meetings/**
import { api } from './client.js'

/**
 * @typedef {Object} MeetingTodo
 * @property {number} id
 * @property {number} assigneeId
 * @property {number} projectId
 * @property {string} title
 * @property {string|null} dueDate       yyyy-MM-dd
 */

/**
 * @typedef {Object} Meeting
 * @property {number} id
 * @property {string} title
 * @property {string} date               yyyy-MM-dd
 * @property {number[]} attendeeMemberIds
 * @property {string|null} notes
 * @property {string[]} summary
 * @property {MeetingTodo[]} todos
 * @property {boolean} manual
 */

/**
 * 회의록 목록 조회. GET /meetings (날짜 내림차순)
 * @param {{ projectId?: number, from?: string, to?: string }} [params] from/to: yyyy-MM-dd
 * @returns {Promise<Meeting[]>}
 */
export const getMeetings = ({ projectId, from, to } = {}) =>
  api.get('/meetings', { params: { projectId, from, to } }).then((page) => page.items)

/**
 * 회의록 상세 조회. GET /meetings/{meetingId}
 * @returns {Promise<Meeting>}
 */
export const getMeeting = (meetingId) => api.get(`/meetings/${meetingId}`)

/**
 * 회의록 저장. POST /meetings
 * - manual=false: AI 요약 결과 저장 (summary·todos 포함)
 * - manual=true: 수기 등록 (summary 빈 배열 허용, notes 필수)
 * @param {{ title: string, date: string, attendeeMemberIds: number[], notes?: string,
 *           summary?: string[],
 *           todos?: Array<{ assigneeId: number, projectId: number, title: string, dueDate?: string }>,
 *           manual: boolean }} payload
 * @returns {Promise<{ id: number }>} 생성된 회의록 ID
 */
export const createMeeting = (payload) => api.post('/meetings', payload)
