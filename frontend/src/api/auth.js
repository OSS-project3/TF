// 인증 API — /api/v1/auth/**
import { api, setToken, clearToken } from './client.js'

/**
 * 회원가입 + JWT 즉시 발급. 성공 시 토큰을 localStorage에 저장한다.
 * POST /auth/register
 * @param {{ name: string, email: string, password: string, role: 'PM'|'FRONTEND'|'BACKEND'|'DESIGNER'|'QA',
 *           initial: string, weeklyCapacityHours: number, skills?: string[] }} payload
 * @returns {Promise<{ accessToken: string }>}
 */
export async function register(payload, remember = true) {
  const data = await api.post('/auth/register', payload, { auth: false })
  if (data?.accessToken) setToken(data.accessToken, remember)
  return data
}

/**
 * 로그인. 성공 시 토큰을 localStorage에 저장한다.
 * POST /auth/login
 * @param {{ email: string, password: string }} payload
 * @returns {Promise<{ accessToken: string }>}
 */
export async function login(payload, remember = true) {
  const data = await api.post('/auth/login', payload, { auth: false })
  if (data?.accessToken) setToken(data.accessToken, remember)
  return data
}

/**
 * 로그아웃. 서버 토큰 블랙리스트 등록 후 로컬 토큰 제거.
 * POST /auth/logout
 */
/**
 * Google ID 토큰으로 로그인 / 자동 회원가입.
 * POST /auth/google
 */
export async function googleLogin(idToken, inviteToken, remember = true) {
  const data = await api.post('/auth/google',
    { idToken, ...(inviteToken ? { inviteToken } : {}) },
    { auth: false })
  if (data?.accessToken) setToken(data.accessToken, remember)
  return data
}

export async function logout() {
  try {
    await api.post('/auth/logout')
  } finally {
    clearToken()
  }
}
