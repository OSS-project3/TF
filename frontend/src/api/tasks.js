// 태스크 API — /api/v1/projects/{id}/tasks, /api/v1/tasks/**, /api/v1/me/tasks
import { api } from './client.js'

/**
 * @typedef {Object} Task
 * @property {number} id
 * @property {number} projectId
 * @property {string} title
 * @property {string} phase
 * @property {number} estimatedHours
 * @property {string} difficulty         'EASY' | 'MEDIUM' | 'HARD'
 * @property {string} status             'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED'
 * @property {number|null} assigneeId
 * @property {number[]} dependencyTaskIds
 * @property {string|null} startDate     yyyy-MM-dd
 * @property {string|null} endDate       yyyy-MM-dd
 * @property {boolean} isCriticalPath
 * @property {boolean} isLateRisk
 */

/**
 * 프로젝트 태스크 목록 조회. GET /projects/{projectId}/tasks
 * @param {number} projectId
 * @param {{ assigneeId?: number, status?: string, phase?: string }} [params]
 * @returns {Promise<Task[]>}
 */
export const getProjectTasks = (projectId, { assigneeId, status, phase } = {}) =>
  api.get(`/projects/${projectId}/tasks`, { params: { assigneeId, status, phase } })
    .then((page) => page.items)

/**
 * 태스크 생성. POST /projects/{projectId}/tasks
 * @param {number} projectId
 * @param {{ title: string, phase: string, estimatedHours: number,
 *           difficulty: 'EASY'|'MEDIUM'|'HARD', assigneeId?: number,
 *           startDate?: string, endDate?: string, dependencyTaskIds?: number[] }} payload
 * @returns {Promise<{ id: number }>} 생성된 태스크 ID
 */
export const createTask = (projectId, payload) =>
  api.post(`/projects/${projectId}/tasks`, payload)

/**
 * 태스크 수정. PATCH /tasks/{taskId} — null 필드는 변경하지 않음.
 * (상태 변경은 changeTaskStatus, 담당자 변경은 changeTaskAssignee 사용)
 * @param {number} taskId
 * @param {{ title?: string, phase?: string, estimatedHours?: number,
 *           difficulty?: 'EASY'|'MEDIUM'|'HARD', startDate?: string, endDate?: string,
 *           isCriticalPath?: boolean, isLateRisk?: boolean }} payload
 * @returns {Promise<Task>}
 */
export const updateTask = (taskId, payload) => api.patch(`/tasks/${taskId}`, payload)

/**
 * 태스크 상태 변경. PATCH /tasks/{taskId}/status
 * @param {number} taskId
 * @param {'TODO'|'IN_PROGRESS'|'DONE'|'BLOCKED'} status
 */
export const changeTaskStatus = (taskId, status) =>
  api.patch(`/tasks/${taskId}/status`, { status })

/**
 * 태스크 담당자 변경. PATCH /tasks/{taskId}/assignee
 * @param {number} taskId
 * @param {number} assigneeId
 */
export const changeTaskAssignee = (taskId, assigneeId) =>
  api.patch(`/tasks/${taskId}/assignee`, { assigneeId })

/**
 * 태스크 삭제. DELETE /tasks/{taskId}
 * @param {number} taskId
 */
export const deleteTask = (taskId) => api.delete(`/tasks/${taskId}`)

/**
 * 내 태스크 조회 (오늘/이번주/이후 그룹화). GET /me/tasks
 * @param {('TODO'|'IN_PROGRESS'|'DONE'|'BLOCKED')} [status] 상태 필터
 * @returns {Promise<{ today: Task[], thisWeek: Task[], later: Task[] }>}
 */
export const getMyTasks = (status) => api.get('/me/tasks', { params: { status } })
