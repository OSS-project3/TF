import { createContext, useContext, useState, useEffect } from 'react'
import { authApi, memberApi, getToken, clearToken, ApiError } from '../api'
import { toDisplayRole, toBackendRole, mapMember } from '../api/mappers.js'

const AuthContext = createContext(null)

// MemberResponse -> 세션 사용자 형태. role === 'PM' 을 관리자(isAdmin)로 취급한다.
// (백엔드에 별도 관리자 플래그가 없으므로 PM 역할을 관리자로 본다)
const buildSession = (me) => ({
  id: me.id,
  name: me.name,
  email: me.email ?? '',
  role: toDisplayRole(me.role),
  isAdmin: me.role === 'PM',
  weeklyCapacityHours: me.weeklyCapacityHours ?? 40,
})

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true) // 세션 복원 중
  const [users, setUsers] = useState([])       // Admin 페이지용 멤버 목록 (읽기)

  // 토큰이 있으면 새로고침 시 세션 복원
  useEffect(() => {
    let cancelled = false
    const restore = async () => {
      if (!getToken()) {
        setLoading(false)
        return
      }
      try {
        const me = await memberApi.getMe()
        if (!cancelled) setUser(buildSession(me))
      } catch {
        clearToken() // 만료/무효 토큰 정리
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    restore()
    return () => { cancelled = true }
  }, [])

  const login = async (email, password, remember = true) => {
    try {
      await authApi.login({ email, password }, remember) // 성공 시 토큰 자동 저장
      const me = await memberApi.getMe()
      setUser(buildSession(me))
      return { ok: true }
    } catch (e) {
      const message =
        e instanceof ApiError && e.code === 'INVALID_CREDENTIALS'
          ? '이메일 또는 비밀번호가 올바르지 않습니다.'
          : e?.message || '로그인에 실패했습니다.'
      return { ok: false, message }
    }
  }

  const googleLogin = async (idToken, inviteToken) => {
    try {
      const data = await authApi.googleLogin(idToken, inviteToken)
      const me = await memberApi.getMe()
      setUser(buildSession(me))
      return { ok: true, needsRoleSetup: !!data?.needsRoleSetup }
    } catch (e) {
      return { ok: false, message: e?.message || 'Google 로그인에 실패했습니다.' }
    }
  }

  const register = async (name, email, password, role, inviteToken) => {
    try {
      await authApi.register({
        name,
        email,
        password,
        role: toBackendRole(role),
        initial: name?.[0] ?? '?',
        weeklyCapacityHours: 40,
        skills: [],
        ...(inviteToken ? { inviteToken } : {}),
      })
      const me = await memberApi.getMe()
      setUser(buildSession(me))
      return { ok: true, needsProfileSetup: true }
    } catch (e) {
      const message =
        e instanceof ApiError && e.code === 'DUPLICATE_EMAIL'
          ? '이미 사용 중인 이메일입니다.'
          : e?.message || '회원가입에 실패했습니다.'
      return { ok: false, message }
    }
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } finally {
      setUser(null)
    }
  }

  // 프로필 수정 후 사이드바 등에 반영되도록 세션 정보 갱신
  const refreshUser = async () => {
    try {
      const me = await memberApi.getMe()
      setUser(buildSession(me))
    } catch { /* 무시 */ }
  }

  // 회원 탈퇴 등으로 서버 토큰이 이미 무효화된 경우, 서버 호출 없이 세션만 정리
  const clearSession = () => {
    clearToken()
    setUser(null)
  }

  // ── Admin 페이지용 ──
  // 백엔드에 멤버 관리(역할 변경·활성화·삭제) 엔드포인트가 없어, 목록은 API에서 읽고
  // 변경 동작은 로컬 상태에만 반영한다(새로고침 시 초기화).
  const loadUsers = async () => {
    try {
      const members = await memberApi.getMembers()
      setUsers(members.map((m) => ({
        ...mapMember(m),
        email: '',          // MemberResponse에 없는 필드 — 화면용 placeholder
        isActive: true,
        joinedAt: '-',
        lastLogin: '-',
      })))
    } catch {
      setUsers([])
    }
  }

  const updateUserRole = (id, role) =>
    setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, role } : u)))
  const toggleUserActive = (id) =>
    setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, isActive: !u.isActive } : u)))
  const deleteUser = (id) =>
    setUsers((prev) => prev.filter((u) => u.id !== id))

  return (
    <AuthContext.Provider value={{
      user, loading, users,
      login, googleLogin, register, logout, refreshUser, clearSession,
      loadUsers, updateUserRole, toggleUserActive, deleteUser,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
