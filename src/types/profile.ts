export type Locale = 'vi' | 'en'

export interface LocalizedText {
  vi: string
  en: string
}

export interface LocalizedList {
  vi: string[]
  en: string[]
}

export interface SocialLink {
  id: 'github' | 'linkedin' | 'facebook' | 'x' | 'website'
  label: string
  handle: string
  url: string
}

export interface HighlightMetric {
  value: string
  label: LocalizedText
}

export interface SkillGroup {
  id: string
  title: LocalizedText
  description: LocalizedText
  accent: 'emerald' | 'indigo' | 'amber' | 'sky'
  items: string[]
  sortOrder?: number
}

export interface ExperienceItem {
  id: string
  company: string
  role: LocalizedText
  period: LocalizedText
  location: LocalizedText
  type: LocalizedText
  summary: LocalizedText
  achievements: LocalizedList
  stack: string[]
  sortOrder?: number
}

export interface EducationItem {
  id: string
  school: string
  degree: LocalizedText
  period: string | LocalizedText
  gpa?: string
  details: LocalizedText
  sortOrder?: number
}

export interface ProjectItem {
  id: string
  title: string
  category: 'fullstack' | 'frontend' | 'tooling' | 'work'
  featured?: boolean
  year: string
  tagline: LocalizedText
  description: LocalizedText
  highlights: LocalizedList
  stack: string[]
  liveUrl?: string
  repoUrl?: string
  secondaryRepoUrl?: string
  sortOrder?: number
}

export interface ProfileData {
  name: string
  initials: string
  avatarUrl?: string
  handle: string
  role: LocalizedText
  availability: LocalizedText
  location: LocalizedText
  timezone: string
  email: string
  phone?: string
  cvPdfUrl?: string
  headline: LocalizedText
  bio: LocalizedText
  currentFocus: LocalizedText
  metrics: HighlightMetric[]
  socials: SocialLink[]
  skills: SkillGroup[]
  experiences: ExperienceItem[]
  education: EducationItem[]
  languages: LocalizedList
  projects: ProjectItem[]
  isDraft?: boolean
}

export interface ContactMessage {
  id: number
  name: string
  email: string
  subject: string
  message: string
  read: boolean
  createdAt: string
}

export interface LoginResponse {
  token: string
  username: string
  expiresInMs: number
}
