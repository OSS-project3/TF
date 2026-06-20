import { api, setToken } from './client.js'

export const createInvitation = () => api.post('/invitations')

export const acceptInvitation = async (token, remember = true) => {
  const data = await api.post('/invitations/accept', { token })
  if (data?.accessToken) setToken(data.accessToken, remember)
  return data
}

// ── 이메일 기반 팀원 초대(참가 요청) ──────────────────────────────────

/** PM이 이메일로 팀원을 초대한다. POST /invitations/email */
export const inviteByEmail = (email) => api.post('/invitations/email', { email })

/** 내가 받은 미처리 참가 요청 목록. GET /invitations/received */
export const getReceivedInvitations = () => api.get('/invitations/received')

/**
 * 참가 요청 수락. POST /invitations/received/{id}/accept
 * 워크스페이스가 전환되며 새 JWT를 반환·저장한다.
 */
export const acceptReceivedInvitation = async (id, remember = true) => {
  const data = await api.post(`/invitations/received/${id}/accept`)
  if (data?.accessToken) setToken(data.accessToken, remember)
  return data
}

/** 참가 요청 거절. POST /invitations/received/{id}/reject */
export const rejectReceivedInvitation = (id) => api.post(`/invitations/received/${id}/reject`)
