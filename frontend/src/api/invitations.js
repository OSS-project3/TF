import { api, setToken } from './client.js'

export const createInvitation = () => api.post('/invitations')

export const acceptInvitation = async (token, remember = true) => {
  const data = await api.post('/invitations/accept', { token })
  if (data?.accessToken) setToken(data.accessToken, remember)
  return data
}
