// 프로젝트 API — /api/v1/projects/**
import { api } from './client.js'

/**
 * @typedef {Object} Project
 * @property {number} id
 * @property {string} name
 * @property {string} goal
 * @property {string} deadline           yyyy-MM-dd
 * @property {string} status             'ACTIVE' | 'ARCHIVED'
 * @property {number} progress           0.0 ~ 1.0 (Task 집계 실시간 계산)
 * @property {number[]} memberIds
 * @property {number} taskCount
 * @property {number} doneTaskCount
 * @property {number} lateTaskCount
 * @property {string} health             'OK' | 'WARN' | 'BAD' | 'IDLE'
 */

/**
 * 프로젝트 목록 조회. GET /projects
 * @param {{ memberId?: number, status?: 'ACTIVE'|'ARCHIVED' }} [params]
 * @returns {Promise<Project[]>}
 */
export const getProjects = ({ memberId, status } = {}) =>
  api.get('/projects', { params: { memberId, status } }).then((page) => page.items)

/**
 * 프로젝트 생성 [PM 전용]. POST /projects
 * @param {{ name: string, goal: string, deadline: string, memberIds?: number[] }} payload
 * @returns {Promise<{ id: number }>} 생성된 프로젝트 ID
 */
export const createProject = (payload) => api.post('/projects', payload)

/**
 * 프로젝트 상세 조회. GET /projects/{projectId}
 * @returns {Promise<Project>}
 */
export const getProject = (projectId) => api.get(`/projects/${projectId}`)

/**
 * 프로젝트 수정 [PM 전용]. PATCH /projects/{projectId} — null 필드는 변경하지 않음.
 * @param {number} projectId
 * @param {{ name?: string, goal?: string, deadline?: string }} payload
 * @returns {Promise<Project>}
 */
export const updateProject = (projectId, payload) => api.patch(`/projects/${projectId}`, payload)

/**
 * 프로젝트 아카이브 [PM 전용]. DELETE /projects/{projectId} (status → ARCHIVED)
 */
export const archiveProject = (projectId) => api.delete(`/projects/${projectId}`)

/**
 * 프로젝트 멤버 목록 조회. GET /projects/{projectId}/members
 * @returns {Promise<import('./members.js').Member[]>}
 */
export const getProjectMembers = (projectId) =>
  api.get(`/projects/${projectId}/members`).then((page) => page.items)

/**
 * 프로젝트 멤버 일괄 교체 [PM 전용]. PUT /projects/{projectId}/members (전체 덮어쓰기)
 * @param {number} projectId
 * @param {number[]} memberIds
 */
export const replaceProjectMembers = (projectId, memberIds) =>
  api.put(`/projects/${projectId}/members`, { memberIds })

/**
 * 프로젝트 멤버 추가 [PM 전용]. POST /projects/{projectId}/members/{memberId}
 */
export const addProjectMember = (projectId, memberId) =>
  api.post(`/projects/${projectId}/members/${memberId}`)

/**
 * 프로젝트 멤버 제거 [PM 전용]. DELETE /projects/{projectId}/members/{memberId}
 */
export const removeProjectMember = (projectId, memberId) =>
  api.delete(`/projects/${projectId}/members/${memberId}`)
