import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import Sidebar from './ui/Sidebar/Sidebar'
import Topbar from './ui/Topbar/Topbar'
import AIThread from './ui/AIThread/AIThread'
import Projects from './screens/Projects/Projects'
import CreateProjectModal from './screens/Projects/CreateProjectModal'
import ProjectDetail from './screens/ProjectDetail/ProjectDetail'
import Dashboard from './screens/Dashboard/Dashboard'
import MemberDashboard from './screens/MemberDashboard/MemberDashboard'
import Team from './screens/Team/Team'
import Meetings from './screens/Meetings/Meetings'
import Schedule from './screens/Schedule/Schedule'
import Settings from './screens/Settings/Settings'
import { useAuth } from './context/AuthContext.jsx'
import { projectApi, memberApi, dashboardApi, taskApi } from './api'
import { adaptMember, adaptProject } from './api/adapt'

export default function Workspace() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const role = user?.isAdmin ? 'pm' : 'member'

  const [tab, setTab] = useState('dashboard')
  const [openProjectId, setOpenProjectId] = useState(null)
  const [createOpen, setCreateOpen] = useState(false)

  const [members, setMembers] = useState([])
  const [projects, setProjects] = useState([])
  const [pmDash, setPmDash] = useState(null)
  const [memberDash, setMemberDash] = useState(null)
  const [workloads, setWorkloads] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      const [rawMembers, rawProjects, wl] = await Promise.all([
        memberApi.getMembers(),
        projectApi.getProjects(),
        memberApi.getTeamWorkloads().catch(() => []),
      ])
      setMembers(rawMembers.map(adaptMember))
      setProjects(rawProjects.map(adaptProject))
      setWorkloads(wl)
      if (role === 'pm') {
        setPmDash(await dashboardApi.getPmDashboard().catch(() => null))
      } else {
        setMemberDash(await dashboardApi.getMemberDashboard().catch(() => null))
      }
    } catch (e) {
      setError(e?.message || '데이터를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [role])

  useEffect(() => { load() }, [load])

  const currentMember = members.find(m => m.id === String(user?.id))
  const currentUser = currentMember || {
    id: String(user?.id ?? ''),
    name: user?.name ?? '사용자',
    role: user?.role ?? '',
    init: user?.name?.[0] ?? '?',
    hours: user?.weeklyCapacityHours ?? 40,
    skills: [],
  }

  function openProject(id) { setOpenProjectId(id); setTab('projects') }
  function goTab(t) { setTab(t); setOpenProjectId(null) }

  async function handleLogout() { await logout(); navigate('/login', { replace: true }) }

  async function createProject({ name, goal, deadline, members: memberIds, tasks }) {
    const ids = Array.from(new Set([...memberIds.map(Number), Number(user.id)]))
    const { id } = await projectApi.createProject({ name, goal: goal || name, deadline, memberIds: ids })
    await Promise.all((tasks || []).map(t =>
      taskApi.createTask(id, {
        title: t.title,
        phase: t.phase || '개발',
        estimatedHours: Number(t.estimatedHours) > 0 ? Number(t.estimatedHours) : null,
        difficulty: String(t.difficulty || 'MEDIUM').toUpperCase(),
        assigneeIds: (t.assigneeIds ?? []).map(Number),
        startDate: t.startDate || null,
        endDate: t.endDate || null,
      }).catch(() => {})
    ))
    setCreateOpen(false)
    await load()
    openProject(String(id))
  }

  async function updateProject(projectId, { name, goal, deadline, memberIds }) {
    await projectApi.updateProject(Number(projectId), { name, goal, deadline })
    if (memberIds) await projectApi.replaceProjectMembers(Number(projectId), memberIds.map(Number))
    await load()
  }

  async function archiveProject(projectId) {
    await projectApi.archiveProject(Number(projectId))
    setOpenProjectId(null)
    await load()
  }

  if (loading) {
    return <div style={{ display: 'grid', placeItems: 'center', minHeight: '100vh', color: 'var(--muted)' }}>불러오는 중…</div>
  }

  const threadKey = openProjectId ? 'detail' : (tab === 'settings' ? 'dashboard' : tab)
  const currentProject = projects.find(p => p.id === openProjectId)
  const myProjectCount = role === 'pm' ? projects.length : projects.filter(p => p.members.includes(currentUser.id)).length

  let main = null
  let crumbs = ['TeamFlow']

  if (error) {
    main = <div className="page"><div className="placeholder" style={{ padding: 40, color: 'var(--bad)' }}>{error}</div></div>
  } else if (openProjectId && currentProject) {
    crumbs = ['TeamFlow', '프로젝트', currentProject.name]
    main = <ProjectDetail project={currentProject} members={members} back={() => setOpenProjectId(null)} role={role} currentUser={currentUser} onUpdate={updateProject} onArchive={archiveProject} />
  } else if (tab === 'settings') {
    crumbs = ['TeamFlow', '설정']
    main = <Settings onDeleted={() => navigate('/login', { replace: true })} />
  } else if (tab === 'dashboard') {
    crumbs = ['TeamFlow', role === 'pm' ? '대시보드' : '내 할 일']
    main = role === 'pm'
      ? <Dashboard projects={projects} members={members} openProject={openProject} openCreate={() => setCreateOpen(true)} pm={pmDash} />
      : <MemberDashboard currentUser={currentUser} projects={projects} members={members} openProject={openProject} dash={memberDash} />
  } else if (tab === 'projects') {
    crumbs = ['TeamFlow', '프로젝트']
    main = <Projects projects={projects} members={members} openProject={openProject} openCreate={() => setCreateOpen(true)} role={role} currentUser={currentUser} />
  } else if (tab === 'team') {
    crumbs = ['TeamFlow', '팀']
    main = <Team members={members} workloads={workloads} role={role} onReload={load} />
  } else if (tab === 'meetings') {
    crumbs = ['TeamFlow', '회의록']
    main = <Meetings members={members} projects={projects} />
  } else if (tab === 'schedule') {
    crumbs = ['TeamFlow', role === 'pm' ? '일정' : '내 일정']
    main = <Schedule projects={projects} members={members} role={role} currentUser={currentUser} />
  }

  return (
    <div className="app">
      <Sidebar
        tab={openProjectId ? 'projects' : tab}
        setTab={goTab}
        counts={{ projects: myProjectCount, team: members.length }}
        role={role}
        currentUser={currentUser}
        onSettings={() => goTab('settings')}
        onLogout={handleLogout}
      />
      <main className="main">
        <Topbar
          crumbs={crumbs}
          actions={role === 'member' ? <span className="badge ai">팀원</span> : <span className="badge ai">PM</span>}
        />
        {main}
      </main>
      <AIThread tab={threadKey} role={role} />

      {createOpen && role === 'pm' && (
        <CreateProjectModal
          members={members}
          currentUser={currentUser}
          onClose={() => setCreateOpen(false)}
          onCreate={createProject}
        />
      )}
    </div>
  )
}
