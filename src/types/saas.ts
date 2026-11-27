import type { ContentNode } from '@/types/node'

export type UserRole = 'USER' | 'SUPER_ADMIN'
export type UserStatus = 'ACTIVE' | 'BANNED'
export type PageVisibility = 'DRAFT' | 'PUBLIC' | 'UNLISTED'
export type PageLanguageMode = 'bilingual' | 'vi' | 'en'
export type PageFontFamily = 'inter' | 'be-vietnam' | 'roboto' | 'merriweather' | 'lora' | 'times' | 'arial' | 'jetbrains'
export type PageFontScale = 'sm' | 'md' | 'lg'
export type PageLineHeight = 'tight' | 'normal' | 'relaxed'
export type PageSectionSpacing = 'compact' | 'normal' | 'spacious'
export type PageLayoutMode = 'a4' | 'web' | 'wide' | 'custom'
export type PageSheetMode = 'paper' | 'transparent'

export interface UserProfile {
  id: string
  username: string
  email: string
  displayName: string
  avatarUrl?: string | null
  hasPassword?: boolean
  googleLinked?: boolean
  role: UserRole | string
  status: UserStatus | string
  maxPages: number
  maxStorageBytes: number
  usedPages: number
  usedStorageBytes: number
}

export interface AuthResponse {
  token: string
  username: string
  expiresInMs: number
  user: UserProfile
  isNewUser?: boolean
}

export interface GoogleConfigResponse {
  enabled: boolean
  clientId: string
}

export interface UpdateAccountPayload {
  username?: string
  displayName?: string
  avatarUrl?: string | null
  clearAvatar?: boolean
}

export interface ChangePasswordPayload {
  currentPassword?: string
  newPassword: string
}

export interface ForgotPasswordResponse {
  message: string
  smtpConfigured: boolean
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
  displayName?: string
}

export interface PageOwner {
  username: string
  displayName: string
  role: UserRole | string
  avatarUrl?: string | null
}

export interface PageTheme {
  bgColor?: string
  primaryColor?: string
  fontFamily?: PageFontFamily
  fontScale?: PageFontScale
  fontScalePct?: number
  lineHeight?: PageLineHeight
  lineHeightValue?: number
  paragraphGapPx?: number
  sectionSpacing?: PageSectionSpacing
  pageLayout?: PageLayoutMode
  customWidthPx?: number
  sheetMode?: PageSheetMode
  paperBgColor?: string
  textColor?: string
  marginTop?: number
  marginBottom?: number
  marginLeft?: number
  marginRight?: number
  stackColumnsOnMobile?: boolean
  showPageBreaks?: boolean
  hideNavbar?: boolean
  languageMode?: PageLanguageMode
  ogImageUrl?: string
  category?: string
  previewLayout?: string
}

export interface PageSummary {
  id: string
  ownerId: string
  slug: string
  title: string
  description: string
  visibility: PageVisibility | string
  isPrimary: boolean
  isTemplate: boolean
  isHomeFeatured: boolean
  cvPdfUrl?: string | null
  themeJson: string
  viewCount: number
  unreadMessagesCount: number
  createdAt: string
  updatedAt: string
}

export interface PublicPageResponse {
  page: PageSummary
  owner: PageOwner
  nodes: ContentNode[]
}

export interface TemplateSummary {
  id: string
  slug: string
  title: string
  description: string
  ownerUsername: string
  themeJson?: string
  updatedAt: string
}

export interface CreatePagePayload {
  title: string
  slug: string
  description?: string
  visibility?: PageVisibility
  fromTemplateId?: string
}

export interface UpdatePagePayload {
  title: string
  slug: string
  description?: string
  visibility?: PageVisibility
  isPrimary?: boolean
  cvPdfUrl?: string | null
  themeJson?: string
}

export interface UploadedFileItem {
  id: string
  url: string
  filename: string
  contentType: string
  sizeBytes: number
  usageCount?: number
  createdAt: string
}

export interface MyFilesResponse {
  files: UploadedFileItem[]
  usedStorageBytes: number
  maxStorageBytes: number
}

export interface AdminOverview {
  totalUsers: number
  activeUsers: number
  bannedUsers: number
  totalPages: number
  publicPages: number
  templatePages: number
  totalStorageBytes: number
}

export interface AdminUser {
  id: string
  username: string
  email: string
  displayName: string
  role: UserRole | string
  status: UserStatus | string
  maxPages: number
  maxStorageBytes: number
  usedPages: number
  usedStorageBytes: number
  createdAt: string
}

export interface AdminUpdateUserPayload {
  status?: UserStatus
  role?: UserRole
  maxPages?: number
  maxStorageBytes?: number
}

export interface AdminPage {
  id: string
  ownerId: string
  ownerUsername: string
  slug: string
  title: string
  visibility: PageVisibility | string
  isPrimary: boolean
  isTemplate: boolean
  isHomeFeatured: boolean
  viewCount: number
  updatedAt: string
}

export interface AdminUpdatePagePayload {
  isTemplate?: boolean
  isHomeFeatured?: boolean
  visibility?: PageVisibility
}

export interface AdminAiConfig {
  enabled: boolean
  baseUrl: string
  model: string
  hasApiKey: boolean
  apiKeyMasked: string
}

export interface UpdateAiConfigPayload {
  enabled?: boolean
  baseUrl?: string
  apiKey?: string
  clearApiKey?: boolean
  model?: string
}

export interface TestAiConfigResponse {
  ok: boolean
  message: string
  models: string[]
}

export interface AiStatusResponse {
  available: boolean
  model: string
}

const VALID_FONT_FAMILIES = new Set<PageFontFamily>([
  'inter',
  'be-vietnam',
  'roboto',
  'merriweather',
  'lora',
  'times',
  'arial',
  'jetbrains',
])
const VALID_FONT_SCALES = new Set<PageFontScale>(['sm', 'md', 'lg'])
const VALID_LINE_HEIGHTS = new Set<PageLineHeight>(['tight', 'normal', 'relaxed'])
const VALID_SECTION_SPACINGS = new Set<PageSectionSpacing>(['compact', 'normal', 'spacious'])
const VALID_PAGE_LAYOUTS = new Set<PageLayoutMode>(['a4', 'web', 'wide', 'custom'])
const VALID_SHEET_MODES = new Set<PageSheetMode>(['paper', 'transparent'])

function parseNumberInRange(raw: unknown, min: number, max: number): number | undefined {
  if (typeof raw !== 'number' || !Number.isFinite(raw)) return undefined
  return Math.max(min, Math.min(max, Math.round(raw)))
}

function parseFloatInRange(raw: unknown, min: number, max: number, decimals = 2): number | undefined {
  if (typeof raw !== 'number' || !Number.isFinite(raw)) return undefined
  const factor = 10 ** decimals
  return Math.max(min, Math.min(max, Math.round(raw * factor) / factor))
}

export function parsePageTheme(themeJson?: string | null): PageTheme {
  if (!themeJson) return {}
  try {
    const parsed = JSON.parse(themeJson) as Record<string, unknown>
    const rawLang = parsed.languageMode
    const languageMode: PageLanguageMode =
      rawLang === 'vi' || rawLang === 'en' ? rawLang : 'bilingual'
    const rawFont = parsed.fontFamily
    const rawScale = parsed.fontScale
    const rawLineHeight = parsed.lineHeight
    const rawSpacing = parsed.sectionSpacing
    const rawPageLayout = parsed.pageLayout
    const rawSheetMode = parsed.sheetMode
    return {
      bgColor: typeof parsed.bgColor === 'string' && parsed.bgColor.trim() ? parsed.bgColor.trim() : undefined,
      primaryColor: typeof parsed.primaryColor === 'string' && parsed.primaryColor.trim() ? parsed.primaryColor.trim() : undefined,
      fontFamily: typeof rawFont === 'string' && VALID_FONT_FAMILIES.has(rawFont as PageFontFamily)
        ? (rawFont as PageFontFamily)
        : undefined,
      fontScale: typeof rawScale === 'string' && VALID_FONT_SCALES.has(rawScale as PageFontScale)
        ? (rawScale as PageFontScale)
        : undefined,
      fontScalePct: parseNumberInRange(parsed.fontScalePct, 80, 135),
      lineHeight: typeof rawLineHeight === 'string' && VALID_LINE_HEIGHTS.has(rawLineHeight as PageLineHeight)
        ? (rawLineHeight as PageLineHeight)
        : undefined,
      lineHeightValue: parseFloatInRange(parsed.lineHeightValue, 1.0, 2.2, 2),
      paragraphGapPx: parseNumberInRange(parsed.paragraphGapPx, 0, 24),
      sectionSpacing: typeof rawSpacing === 'string' && VALID_SECTION_SPACINGS.has(rawSpacing as PageSectionSpacing)
        ? (rawSpacing as PageSectionSpacing)
        : undefined,
      pageLayout: typeof rawPageLayout === 'string' && VALID_PAGE_LAYOUTS.has(rawPageLayout as PageLayoutMode)
        ? (rawPageLayout as PageLayoutMode)
        : undefined,
      customWidthPx: parseNumberInRange(parsed.customWidthPx, 480, 1600),
      sheetMode: typeof rawSheetMode === 'string' && VALID_SHEET_MODES.has(rawSheetMode as PageSheetMode)
        ? (rawSheetMode as PageSheetMode)
        : undefined,
      paperBgColor: typeof parsed.paperBgColor === 'string' && parsed.paperBgColor.trim() ? parsed.paperBgColor.trim() : undefined,
      textColor: typeof parsed.textColor === 'string' && parsed.textColor.trim() ? parsed.textColor.trim() : undefined,
      marginTop: parseNumberInRange(parsed.marginTop, 0, 120),
      marginBottom: parseNumberInRange(parsed.marginBottom, 0, 120),
      marginLeft: parseNumberInRange(parsed.marginLeft, 0, 120),
      marginRight: parseNumberInRange(parsed.marginRight, 0, 120),
      stackColumnsOnMobile: typeof parsed.stackColumnsOnMobile === 'boolean' ? parsed.stackColumnsOnMobile : undefined,
      showPageBreaks: typeof parsed.showPageBreaks === 'boolean' ? parsed.showPageBreaks : undefined,
      hideNavbar: Boolean(parsed.hideNavbar),
      languageMode,
      ogImageUrl: typeof parsed.ogImageUrl === 'string' && parsed.ogImageUrl.trim() ? parsed.ogImageUrl.trim() : undefined,
      category: typeof parsed.category === 'string' && parsed.category.trim() ? parsed.category.trim() : undefined,
      previewLayout: typeof parsed.previewLayout === 'string' && parsed.previewLayout.trim() ? parsed.previewLayout.trim() : undefined,
    }
  } catch {
    return {}
  }
}

export function serializePageTheme(theme: PageTheme): string {
  const payload: Record<string, unknown> = {}
  if (theme.category) payload.category = theme.category
  if (theme.previewLayout) payload.previewLayout = theme.previewLayout
  if (theme.bgColor) payload.bgColor = theme.bgColor
  if (theme.primaryColor) payload.primaryColor = theme.primaryColor
  if (theme.fontFamily && theme.fontFamily !== 'inter') payload.fontFamily = theme.fontFamily
  if (theme.fontScale && theme.fontScale !== 'md') payload.fontScale = theme.fontScale
  if (typeof theme.fontScalePct === 'number') payload.fontScalePct = theme.fontScalePct
  if (theme.lineHeight && theme.lineHeight !== 'normal') payload.lineHeight = theme.lineHeight
  if (typeof theme.lineHeightValue === 'number') payload.lineHeightValue = theme.lineHeightValue
  if (typeof theme.paragraphGapPx === 'number') payload.paragraphGapPx = theme.paragraphGapPx
  if (theme.sectionSpacing && theme.sectionSpacing !== 'normal') payload.sectionSpacing = theme.sectionSpacing
  if (theme.pageLayout && theme.pageLayout !== 'a4') payload.pageLayout = theme.pageLayout
  if (theme.pageLayout === 'custom' && typeof theme.customWidthPx === 'number') payload.customWidthPx = theme.customWidthPx
  if (theme.sheetMode && theme.sheetMode !== 'paper') payload.sheetMode = theme.sheetMode
  if (theme.paperBgColor) payload.paperBgColor = theme.paperBgColor
  if (theme.textColor) payload.textColor = theme.textColor
  if (typeof theme.marginTop === 'number') payload.marginTop = theme.marginTop
  if (typeof theme.marginBottom === 'number') payload.marginBottom = theme.marginBottom
  if (typeof theme.marginLeft === 'number') payload.marginLeft = theme.marginLeft
  if (typeof theme.marginRight === 'number') payload.marginRight = theme.marginRight
  if (typeof theme.stackColumnsOnMobile === 'boolean') payload.stackColumnsOnMobile = theme.stackColumnsOnMobile
  if (theme.showPageBreaks === false) payload.showPageBreaks = false
  if (theme.hideNavbar) payload.hideNavbar = true
  if (theme.languageMode && theme.languageMode !== 'bilingual') payload.languageMode = theme.languageMode
  if (theme.ogImageUrl) payload.ogImageUrl = theme.ogImageUrl
  return JSON.stringify(payload)
}
