import type {
  ContactMessage,
  EducationItem,
  ExperienceItem,
  LoginResponse,
  ProfileData,
  ProjectItem,
  SkillGroup,
} from '@/types/profile'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const TOKEN_KEY = 'my-profile-admin-jwt'

export function getStoredToken(): string | null {
  if (typeof window === 'undefined') return null
  return window.localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string | null) {
  if (typeof window === 'undefined') return
  if (token) {
    window.localStorage.setItem(TOKEN_KEY, token)
  } else {
    window.localStorage.removeItem(TOKEN_KEY)
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken()
  const headers = new Headers(options.headers)

  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let errorMessage = `HTTP ${response.status}`
    try {
      const data = (await response.json()) as { error?: string; message?: string }
      errorMessage = data.error || data.message || errorMessage
    } catch {
      // Ignore non-JSON body
    }
    throw new Error(errorMessage)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const portfolioApi = {
  getPortfolio: () => request<ProfileData>('/api/portfolio'),

  submitContact: (payload: { name: string; email: string; subject: string; message: string }) =>
    request<ContactMessage>('/api/contact', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  login: (username: string, password: string) =>
    request<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  me: () => request<{ username: string; role: string }>('/api/auth/me'),

  updateProfile: (payload: Omit<ProfileData, 'skills' | 'experiences' | 'education' | 'projects'> & { isDraft: boolean }) =>
    request<ProfileData>('/api/admin/profile', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  uploadFile: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<{ id: string; url: string; filename: string; contentType: string }>('/api/admin/upload', {
      method: 'POST',
      body: formData,
    })
  },

  createSkill: (payload: Omit<SkillGroup, 'id'> & { id?: string }) =>
    request<SkillGroup>('/api/admin/skills', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateSkill: (id: string, payload: SkillGroup) =>
    request<SkillGroup>(`/api/admin/skills/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deleteSkill: (id: string) =>
    request<void>(`/api/admin/skills/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),

  createEducation: (payload: Omit<EducationItem, 'id'> & { id?: string }) =>
    request<EducationItem>('/api/admin/education', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateEducation: (id: string, payload: EducationItem) =>
    request<EducationItem>(`/api/admin/education/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deleteEducation: (id: string) =>
    request<void>(`/api/admin/education/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),

  createExperience: (payload: Omit<ExperienceItem, 'id'> & { id?: string }) =>
    request<ExperienceItem>('/api/admin/experiences', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateExperience: (id: string, payload: ExperienceItem) =>
    request<ExperienceItem>(`/api/admin/experiences/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deleteExperience: (id: string) =>
    request<void>(`/api/admin/experiences/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),

  createProject: (payload: Omit<ProjectItem, 'id'> & { id?: string }) =>
    request<ProjectItem>('/api/admin/projects', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateProject: (id: string, payload: ProjectItem) =>
    request<ProjectItem>(`/api/admin/projects/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deleteProject: (id: string) =>
    request<void>(`/api/admin/projects/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),

  getMessages: () => request<ContactMessage[]>('/api/admin/messages'),

  markMessageRead: (id: number, read: boolean) =>
    request<ContactMessage>(`/api/admin/messages/${id}/read`, {
      method: 'PATCH',
      body: JSON.stringify({ read }),
    }),

  deleteMessage: (id: number) =>
    request<void>(`/api/admin/messages/${id}`, {
      method: 'DELETE',
    }),
}
