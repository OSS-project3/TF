// 대시보드 API — /api/v1/dashboard/**
import { api } from './client.js'

/**
 * PM 대시보드 조회 [PM 전용]. GET /dashboard/pm
 * @returns {Promise<{
 *   activeProjectCount: number, averageProgress: number, totalTaskCount: number,
 *   doneTaskCount: number, lateTaskCount: number, memberCount: number,
 *   averageLoadRate: number, projects: import('./projects.js').Project[]
 * }>}
 */
export const getPmDashboard = () => api.get('/dashboard/pm')

/**
 * 멤버 대시보드 조회. GET /dashboard/member
 * @returns {Promise<{
 *   loadRate: number, assignedHours: number, capacityHours: number,
 *   projectCount: number, taskCount: number, doneTaskCount: number,
 *   nextDueDate: string|null,
 *   todayTasks: import('./tasks.js').Task[],
 *   thisWeekTasks: import('./tasks.js').Task[],
 *   laterTasks: import('./tasks.js').Task[],
 *   projects: Array<{ id: number, name: string, progress: number, health: string }>
 * }>}
 */
export const getMemberDashboard = () => api.get('/dashboard/member')
