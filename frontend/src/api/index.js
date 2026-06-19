// 프론트엔드 API 레이어 통합 진입점
// 백엔드(Spring Boot, base URL: /api/v1)의 모든 엔드포인트와 1:1 대응한다.
//
// 사용 예:
//   import { authApi, projectApi, ApiError } from '../api'
//
//   await authApi.login({ email, password })          // 토큰 자동 저장
//   const projects = await projectApi.getProjects()   // Project[]
//   try { ... } catch (e) { if (e instanceof ApiError) console.log(e.code) }

export * as authApi from './auth.js'
export * as memberApi from './members.js'
export * as projectApi from './projects.js'
export * as taskApi from './tasks.js'
export * as meetingApi from './meetings.js'
export * as dashboardApi from './dashboard.js'
export * as aiApi from './ai.js'

export { api, ApiError, getToken, setToken, clearToken } from './client.js'
