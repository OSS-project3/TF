// 멤버 API — /api/v1/me, /api/v1/members, /api/v1/team/workloads
import { api } from './client.js'

/**
 * @typedef {Object} Member
 * @property {number} id
 * @property {string} name
 * @property {string} role               'PM' | 'FRONTEND' | 'BACKEND' | 'DESIGNER' | 'QA'
 * @property {string} initial
 * @property {number} weeklyCapacityHours
 * @property {string[]} skills
 */

/**
 * 내 정보 조회. GET /me
 * @returns {Promise<Member>}
 */
export const getMe = () => api.get('/me')

/**
 * 멤버 목록 조회. GET /members
 * @param {('PM'|'FRONTEND'|'BACKEND'|'DESIGNER'|'QA')} [role] 역할 필터
 * @returns {Promise<Member[]>}
 */
export const getMembers = (role) =>
  api.get('/members', { params: { role } }).then((page) => page.items)

/**
 * 특정 멤버 조회. GET /members/{memberId}
 * @returns {Promise<Member>}
 */
export const getMember = (memberId) => api.get(`/members/${memberId}`)

/**
 * 특정 멤버 워크로드 조회. GET /members/{memberId}/workload
 * @param {number} memberId
 * @param {{ from?: string, to?: string }} [range] yyyy-MM-dd
 */
export const getMemberWorkload = (memberId, { from, to } = {}) =>
  api.get(`/members/${memberId}/workload`, { params: { from, to } })

/**
 * 내 프로필 수정. PATCH /me — null/미지정 필드는 변경하지 않음.
 * @param {{ name?: string, initial?: string, weeklyCapacityHours?: number, skills?: string[] }} payload
 * @returns {Promise<Member>}
 */
export const updateMe = (payload) => api.patch('/me', payload)

/**
 * 비밀번호 변경. PATCH /me/password
 * @param {{ currentPassword: string, newPassword: string }} payload
 */
export const changePassword = (payload) => api.patch('/me/password', payload)

/**
 * 회원 탈퇴. DELETE /me — 계정 삭제 후 토큰 무효화.
 */
export const deleteMe = () => api.delete('/me')

/**
 * 팀 워크로드 조회. GET /team/workloads
 * @param {{ projectId?: number, from?: string, to?: string }} [params]
 */
export const getTeamWorkloads = ({ projectId, from, to } = {}) =>
  api.get('/team/workloads', { params: { projectId, from, to } }).then((page) => page.items)
