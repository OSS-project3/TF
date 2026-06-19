// 공통 HTTP 클라이언트
// 백엔드 응답 형식: { data, message } 또는 { error: { code, message } }
// 모든 도메인 API 모듈(auth/members/projects/tasks/meetings/dashboard)이 이 모듈을 사용한다.

// 개발 시 vite.config.js의 proxy가 /api → http://localhost:8080 으로 전달한다.
// 배포 환경에서는 VITE_API_BASE_URL 환경변수로 절대 URL을 지정할 수 있다.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

const TOKEN_KEY = 'tf_token'

// remember=true → localStorage(브라우저 종료 후에도 유지, 자동 로그인)
// remember=false → sessionStorage(탭/브라우저 종료 시 만료)
export const getToken = () =>
  localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)

export const setToken = (token, remember = true) => {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  if (token) {
    (remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token)
  }
}

export const clearToken = () => {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

/**
 * 백엔드 에러 응답({ error: { code, message } })을 표현하는 에러 객체.
 * code 예: PROJECT_NOT_FOUND, INVALID_CREDENTIALS, FORBIDDEN, DUPLICATE_EMAIL ...
 */
export class ApiError extends Error {
  constructor(status, code, message) {
    super(message || code || `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

const buildQuery = (params) => {
  if (!params) return ''
  const usp = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      usp.append(key, value)
    }
  })
  const qs = usp.toString()
  return qs ? `?${qs}` : ''
}

/**
 * @param {string} method
 * @param {string} path  BASE_URL 뒤에 붙는 경로 (예: '/projects')
 * @param {{ body?: any, params?: Record<string, any>, auth?: boolean }} [options]
 * @returns {Promise<any>} 성공 시 응답의 data 필드 (없으면 null)
 */
async function request(method, path, { body, params, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }

  let res
  try {
    res = await fetch(`${BASE_URL}${path}${buildQuery(params)}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch (e) {
    throw new ApiError(0, 'NETWORK_ERROR', '서버에 연결할 수 없습니다.')
  }

  const text = await res.text()
  const json = text ? JSON.parse(text) : null

  if (!res.ok) {
    const error = json?.error
    throw new ApiError(res.status, error?.code, error?.message)
  }

  // 성공 응답은 항상 { data, message } 형태 (data 없는 경우 null)
  return json ? json.data : null
}

export const api = {
  get: (path, options) => request('GET', path, options),
  post: (path, body, options) => request('POST', path, { ...options, body }),
  patch: (path, body, options) => request('PATCH', path, { ...options, body }),
  put: (path, body, options) => request('PUT', path, { ...options, body }),
  delete: (path, options) => request('DELETE', path, options),
}
