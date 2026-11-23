import { computed, ref } from 'vue'
import { isSampleProfile, profileData as fallbackProfileData } from '@/data/profile'
import { getStoredToken, portfolioApi, setStoredToken } from '@/services/api'
import type {
  ContactMessage,
  EducationItem,
  ExperienceItem,
  ProfileData,
  ProjectItem,
  SkillGroup,
} from '@/types/profile'

const portfolio = ref<ProfileData>({
  ...structuredClone(fallbackProfileData),
  isDraft: isSampleProfile,
})

const apiConnected = ref(false)
const isLoading = ref(false)
const isAuthenticated = ref(false)
const adminUsername = ref('')
const editMode = ref(false)
const loginModalOpen = ref(false)
const inboxModalOpen = ref(false)
const messages = ref<ContactMessage[]>([])

let initialized = false

export function usePortfolio() {
  const isDraft = computed(() => Boolean(portfolio.value.isDraft))
  const unreadMessagesCount = computed(() => messages.value.filter((m) => !m.read).length)

  async function loadPortfolio() {
    isLoading.value = true
    try {
      const remote = await portfolioApi.getPortfolio()
      portfolio.value = remote
      apiConnected.value = true
    } catch {
      apiConnected.value = false
    } finally {
      isLoading.value = false
    }
  }

  async function loadMessages() {
    if (!isAuthenticated.value) return
    try {
      messages.value = await portfolioApi.getMessages()
    } catch {
      // Ignore if session expired
    }
  }

  async function restoreSession() {
    const token = getStoredToken()
    if (!token) return
    try {
      const me = await portfolioApi.me()
      isAuthenticated.value = true
      adminUsername.value = me.username
      apiConnected.value = true
      await loadMessages()
    } catch {
      setStoredToken(null)
      isAuthenticated.value = false
      adminUsername.value = ''
      editMode.value = false
    }
  }

  async function initPortfolio() {
    if (initialized) return
    initialized = true
    await loadPortfolio()
    await restoreSession()
  }

  async function login(username: string, password: string) {
    const res = await portfolioApi.login(username, password)
    setStoredToken(res.token)
    isAuthenticated.value = true
    adminUsername.value = res.username
    apiConnected.value = true
    editMode.value = true
    loginModalOpen.value = false
    await loadPortfolio()
    await loadMessages()
  }

  function logout() {
    setStoredToken(null)
    isAuthenticated.value = false
    adminUsername.value = ''
    editMode.value = false
    inboxModalOpen.value = false
    messages.value = []
  }

  async function updateProfile(
    payload: Omit<ProfileData, 'skills' | 'experiences' | 'education' | 'projects'> & { isDraft: boolean },
  ) {
    const updated = await portfolioApi.updateProfile(payload)
    portfolio.value = updated
  }

  async function saveSkill(item: Omit<SkillGroup, 'id'> & { id?: string }) {
    if (item.id) {
      await portfolioApi.updateSkill(item.id, item as SkillGroup)
    } else {
      await portfolioApi.createSkill(item)
    }
    await loadPortfolio()
  }

  async function deleteSkill(id: string) {
    await portfolioApi.deleteSkill(id)
    await loadPortfolio()
  }

  async function saveEducation(item: Omit<EducationItem, 'id'> & { id?: string }) {
    const normalized: Omit<EducationItem, 'id'> & { id?: string } = {
      ...item,
      period:
        typeof item.period === 'string'
          ? { vi: item.period, en: item.period }
          : item.period,
    }
    if (normalized.id) {
      await portfolioApi.updateEducation(normalized.id, normalized as EducationItem)
    } else {
      await portfolioApi.createEducation(normalized)
    }
    await loadPortfolio()
  }

  async function deleteEducation(id: string) {
    await portfolioApi.deleteEducation(id)
    await loadPortfolio()
  }

  async function saveExperience(item: Omit<ExperienceItem, 'id'> & { id?: string }) {
    if (item.id) {
      await portfolioApi.updateExperience(item.id, item as ExperienceItem)
    } else {
      await portfolioApi.createExperience(item)
    }
    await loadPortfolio()
  }

  async function deleteExperience(id: string) {
    await portfolioApi.deleteExperience(id)
    await loadPortfolio()
  }

  async function saveProject(item: Omit<ProjectItem, 'id'> & { id?: string }) {
    if (item.id) {
      await portfolioApi.updateProject(item.id, item as ProjectItem)
    } else {
      await portfolioApi.createProject(item)
    }
    await loadPortfolio()
  }

  async function deleteProject(id: string) {
    await portfolioApi.deleteProject(id)
    await loadPortfolio()
  }

  async function sendContactMessage(payload: {
    name: string
    email: string
    subject: string
    message: string
  }): Promise<boolean> {
    try {
      await portfolioApi.submitContact(payload)
      apiConnected.value = true
      if (isAuthenticated.value) {
        await loadMessages()
      }
      return true
    } catch {
      return false
    }
  }

  async function markMessageRead(id: number, read: boolean) {
    await portfolioApi.markMessageRead(id, read)
    await loadMessages()
  }

  async function deleteMessage(id: number) {
    await portfolioApi.deleteMessage(id)
    await loadMessages()
  }

  return {
    portfolio,
    isDraft,
    apiConnected,
    isLoading,
    isAuthenticated,
    adminUsername,
    editMode,
    loginModalOpen,
    inboxModalOpen,
    messages,
    unreadMessagesCount,
    initPortfolio,
    loadPortfolio,
    loadMessages,
    login,
    logout,
    updateProfile,
    saveSkill,
    deleteSkill,
    saveEducation,
    deleteEducation,
    saveExperience,
    deleteExperience,
    saveProject,
    deleteProject,
    sendContactMessage,
    markMessageRead,
    deleteMessage,
  }
}
