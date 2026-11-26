import type { ContentNode, NodeAttrs } from '@/types/node'
import type {
  PageFontFamily,
  PageFontScale,
  PageLayoutMode,
  PageLineHeight,
  PageSectionSpacing,
  PageTheme,
} from '@/types/saas'

export interface ColorPreset {
  hex: string
  vi: string
  en: string
}

export interface FontOption {
  id: PageFontFamily
  label: string
  sample: string
  cssStack: string
}

export const PRIMARY_COLOR_PRESETS: ColorPreset[] = [
  { hex: '#00b14f', vi: 'Xanh TopCV', en: 'TopCV Green' },
  { hex: '#1d4ed8', vi: 'Xanh dương', en: 'Royal Blue' },
  { hex: '#0f766e', vi: 'Xanh ngọc', en: 'Teal' },
  { hex: '#4f46e5', vi: 'Chàm', en: 'Indigo' },
  { hex: '#be123c', vi: 'Đỏ đô', en: 'Crimson' },
  { hex: '#b45309', vi: 'Hổ phách', en: 'Amber' },
  { hex: '#7c3aed', vi: 'Tím', en: 'Violet' },
  { hex: '#18181b', vi: 'Than chì', en: 'Charcoal' },
]

export const BG_COLOR_PRESETS: ColorPreset[] = [
  { hex: '#f6f4ef', vi: 'Kem ấm', en: 'Warm Cream' },
  { hex: '#ffffff', vi: 'Trắng tinh', en: 'Pure White' },
  { hex: '#f1f5f9', vi: 'Xám băng', en: 'Cool Slate' },
  { hex: '#f0fdf4', vi: 'Xanh bạc hà', en: 'Soft Mint' },
  { hex: '#eff6ff', vi: 'Xanh băng', en: 'Ice Blue' },
  { hex: '#0f172a', vi: 'Tối (Slate)', en: 'Dark Slate' },
  { hex: '#18181b', vi: 'Đen tuyền', en: 'Dark Zinc' },
]

export const PAGE_LAYOUT_OPTIONS: Array<{ id: PageLayoutMode; vi: string; en: string; width: number }> = [
  { id: 'a4', vi: 'Giấy A4', en: 'A4 Paper', width: 794 },
  { id: 'web', vi: 'Web chuẩn', en: 'Standard Web', width: 960 },
  { id: 'wide', vi: 'Web rộng', en: 'Wide Web', width: 1140 },
  { id: 'custom', vi: 'Tùy chỉnh', en: 'Custom', width: 900 },
]

export const MARGIN_PRESETS: Array<{ id: string; vi: string; en: string; px: number }> = [
  { id: 'none', vi: 'Không lề', en: '0 px', px: 0 },
  { id: 'narrow', vi: 'Hẹp', en: 'Narrow', px: 20 },
  { id: 'normal', vi: 'Chuẩn', en: 'Normal', px: 36 },
  { id: 'spacious', vi: 'Thoáng', en: 'Wide', px: 56 },
]

export interface SurfaceThemePreset {
  id: string
  vi: string
  en: string
  patch: Partial<PageTheme>
}

export const SURFACE_THEME_PRESETS: SurfaceThemePreset[] = [
  {
    id: 'a4-paper',
    vi: 'Giấy A4 trắng',
    en: 'White A4 Paper',
    patch: {
      pageLayout: 'a4',
      sheetMode: 'paper',
      bgColor: '#f1f5f9',
      paperBgColor: '#ffffff',
      textColor: '#18181b',
      marginTop: 36,
      marginBottom: 36,
      marginLeft: 36,
      marginRight: 36,
    },
  },
  {
    id: 'web-cream',
    vi: 'Web kem trong suốt',
    en: 'Warm Transparent Web',
    patch: {
      pageLayout: 'web',
      sheetMode: 'transparent',
      bgColor: '#f6f4ef',
      textColor: '#18181b',
      marginTop: 12,
      marginBottom: 24,
      marginLeft: 16,
      marginRight: 16,
    },
  },
  {
    id: 'web-dark',
    vi: 'Web Dark Mode',
    en: 'Dark Transparent Web',
    patch: {
      pageLayout: 'web',
      sheetMode: 'transparent',
      bgColor: '#0f172a',
      textColor: '#f1f5f9',
      primaryColor: '#38bdf8',
      marginTop: 12,
      marginBottom: 24,
      marginLeft: 16,
      marginRight: 16,
    },
  },
  {
    id: 'dark-paper',
    vi: 'Tờ giấy tối',
    en: 'Dark Paper Sheet',
    patch: {
      pageLayout: 'a4',
      sheetMode: 'paper',
      bgColor: '#09090b',
      paperBgColor: '#18181b',
      textColor: '#f4f4f5',
      primaryColor: '#34d399',
      marginTop: 36,
      marginBottom: 36,
      marginLeft: 36,
      marginRight: 36,
    },
  },
  {
    id: 'topcv-mint',
    vi: 'TopCV Bạc hà',
    en: 'TopCV Mint',
    patch: {
      pageLayout: 'a4',
      sheetMode: 'paper',
      bgColor: '#ecfdf5',
      paperBgColor: '#ffffff',
      textColor: '#18181b',
      primaryColor: '#00b14f',
      marginTop: 32,
      marginBottom: 32,
      marginLeft: 32,
      marginRight: 32,
    },
  },
]

export const FONT_OPTIONS: FontOption[] = [
  {
    id: 'inter',
    label: 'Inter',
    sample: 'Hiện đại, chuẩn CV quốc tế',
    cssStack: '"Inter", -apple-system, BlinkMacSystemFont, ui-sans-serif, system-ui, sans-serif',
  },
  {
    id: 'be-vietnam',
    label: 'Be Vietnam Pro',
    sample: 'Sắc nét, tối ưu tiếng Việt',
    cssStack: '"Be Vietnam Pro", "Inter", ui-sans-serif, system-ui, sans-serif',
  },
  {
    id: 'roboto',
    label: 'Roboto',
    sample: 'Gọn gàng, thân thiện ATS',
    cssStack: '"Roboto", "Inter", ui-sans-serif, system-ui, sans-serif',
  },
  {
    id: 'merriweather',
    label: 'Merriweather',
    sample: 'Cổ điển, trang trọng (Serif)',
    cssStack: '"Merriweather", Georgia, ui-serif, serif',
  },
  {
    id: 'lora',
    label: 'Lora',
    sample: 'Thanh lịch, biên tập (Serif)',
    cssStack: '"Lora", Georgia, ui-serif, serif',
  },
  {
    id: 'times',
    label: 'Times New Roman',
    sample: 'Cổ điển, chuẩn học thuật & ATS',
    cssStack: '"Times New Roman", Times, Georgia, serif',
  },
  {
    id: 'arial',
    label: 'Arial',
    sample: 'Phổ thông, tối giản dễ đọc',
    cssStack: 'Arial, Helvetica, ui-sans-serif, sans-serif',
  },
  {
    id: 'jetbrains',
    label: 'JetBrains Mono',
    sample: 'Kỹ thuật, lập trình viên',
    cssStack: '"JetBrains Mono", ui-monospace, SFMono-Regular, Menlo, monospace',
  },
]

export const FONT_SCALE_OPTIONS: { id: PageFontScale; vi: string; en: string }[] = [
  { id: 'sm', vi: 'Nhỏ', en: 'Small' },
  { id: 'md', vi: 'Vừa', en: 'Medium' },
  { id: 'lg', vi: 'Lớn', en: 'Large' },
]

export const LINE_HEIGHT_OPTIONS: { id: PageLineHeight; vi: string; en: string; value: string }[] = [
  { id: 'tight', vi: 'Chặt', en: 'Compact', value: '1.45' },
  { id: 'normal', vi: 'Vừa', en: 'Normal', value: '1.65' },
  { id: 'relaxed', vi: 'Thoáng', en: 'Relaxed', value: '1.85' },
]

export const SECTION_SPACING_OPTIONS: { id: PageSectionSpacing; vi: string; en: string; value: string }[] = [
  { id: 'compact', vi: 'Gọn', en: 'Compact', value: '0.75rem' },
  { id: 'normal', vi: 'Vừa', en: 'Normal', value: '1.5rem' },
  { id: 'spacious', vi: 'Rộng', en: 'Spacious', value: '2.25rem' },
]

function clampByte(n: number): number {
  return Math.max(0, Math.min(255, Math.round(n)))
}

function toHexByte(n: number): string {
  return clampByte(n).toString(16).padStart(2, '0')
}

export function normalizeHex(input: string | undefined | null): string | null {
  if (!input) return null
  const raw = input.trim().toLowerCase()
  if (!raw || raw === 'transparent' || raw === 'inherit' || raw === 'currentcolor') return null

  if (/^#[0-9a-f]{3}$/i.test(raw)) {
    const [, r, g, b] = raw.match(/^#(.)(.)(.)$/)!
    return `#${r}${r}${g}${g}${b}${b}`
  }
  if (/^#[0-9a-f]{6}$/i.test(raw)) {
    return raw
  }
  if (/^#[0-9a-f]{8}$/i.test(raw)) {
    const alpha = parseInt(raw.slice(7, 9), 16) / 255
    if (alpha <= 0.08) return null
    return `#${raw.slice(1, 7)}`
  }

  const rgbMatch = raw.match(/^rgba?\(\s*(\d+(?:\.\d+)?)\s*[,\s]\s*(\d+(?:\.\d+)?)\s*[,\s]\s*(\d+(?:\.\d+)?)(?:\s*[,/]\s*(\d+(?:\.\d+)?%?))?\s*\)$/)
  if (rgbMatch) {
    const r = Number(rgbMatch[1])
    const g = Number(rgbMatch[2])
    const b = Number(rgbMatch[3])
    if (rgbMatch[4] !== undefined) {
      const aRaw = rgbMatch[4]
      const alpha = aRaw.endsWith('%') ? Number(aRaw.slice(0, -1)) / 100 : Number(aRaw)
      if (!Number.isFinite(alpha) || alpha <= 0.08) return null
    }
    return `#${toHexByte(r)}${toHexByte(g)}${toHexByte(b)}`
  }
  return null
}

function hexToRgb(hex: string): [number, number, number] {
  const clean = normalizeHex(hex) ?? '#1d4ed8'
  return [
    parseInt(clean.slice(1, 3), 16),
    parseInt(clean.slice(3, 5), 16),
    parseInt(clean.slice(5, 7), 16),
  ]
}

export function mixHex(hexA: string, hexB: string, weightA: number): string {
  const w = Math.max(0, Math.min(1, weightA))
  const [r1, g1, b1] = hexToRgb(hexA)
  const [r2, g2, b2] = hexToRgb(hexB)
  return `#${toHexByte(r1 * w + r2 * (1 - w))}${toHexByte(g1 * w + g2 * (1 - w))}${toHexByte(b1 * w + b2 * (1 - w))}`
}

export function relativeLuminance(hex: string): number {
  const [r, g, b] = hexToRgb(hex).map((v) => {
    const c = v / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r! + 0.7152 * g! + 0.0722 * b!
}

export function isDarkColor(hex: string): boolean {
  return relativeLuminance(hex) < 0.42
}

export function isNeutralColor(hex: string): boolean {
  const norm = normalizeHex(hex)
  if (!norm) return true
  const [r, g, b] = hexToRgb(norm)
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const delta = max - min
  const lum = (max + min) / 510
  const sat = delta === 0 ? 0 : delta / (1 - Math.abs(2 * lum - 1)) / 255
  return sat < 0.16 || max < 38 || min > 238
}

export function colorDistance(hexA: string, hexB: string): number {
  const [r1, g1, b1] = hexToRgb(hexA)
  const [r2, g2, b2] = hexToRgb(hexB)
  return Math.hypot(r1 - r2, g1 - g2, b1 - b2)
}

export interface AccentTints {
  primary: string
  darkText: string
  softBg: string
  softBorder: string
}

export function deriveAccentTints(primaryHex: string): AccentTints {
  const primary = normalizeHex(primaryHex) ?? '#1d4ed8'
  const darkText = isDarkColor(primary) ? primary : mixHex(primary, '#09090b', 0.68)
  const softBg = mixHex(primary, '#ffffff', 0.1)
  const softBorder = mixHex(primary, '#ffffff', 0.28)
  return { primary, darkText, softBg, softBorder }
}

export function getFontCssStack(font?: PageFontFamily): string {
  return FONT_OPTIONS.find((item) => item.id === font)?.cssStack ?? FONT_OPTIONS[0]!.cssStack
}

export function resolveLineHeightNumber(theme: PageTheme): number {
  if (typeof theme.lineHeightValue === 'number' && Number.isFinite(theme.lineHeightValue)) {
    return Math.max(1.0, Math.min(2.2, Math.round(theme.lineHeightValue * 100) / 100))
  }
  if (theme.lineHeight === 'tight') return 1.35
  if (theme.lineHeight === 'relaxed') return 1.85
  return 1.55
}

export function resolveFontScalePct(theme: PageTheme): number {
  if (typeof theme.fontScalePct === 'number' && Number.isFinite(theme.fontScalePct)) {
    return Math.max(80, Math.min(135, Math.round(theme.fontScalePct)))
  }
  if (theme.fontScale === 'sm') return 90
  if (theme.fontScale === 'lg') return 112
  return 100
}

export function resolveParagraphGapPx(theme: PageTheme): number {
  if (typeof theme.paragraphGapPx === 'number' && Number.isFinite(theme.paragraphGapPx)) {
    return Math.max(0, Math.min(24, Math.round(theme.paragraphGapPx)))
  }
  return 4
}

export function buildPageThemeStyle(theme: PageTheme): Record<string, string> {
  const primary = normalizeHex(theme.primaryColor) ?? '#1d4ed8'
  const tints = deriveAccentTints(primary)
  const lineHeight = String(resolveLineHeightNumber(theme))
  const paragraphGap = `${resolveParagraphGapPx(theme)}px`
  const sectionGap = SECTION_SPACING_OPTIONS.find((o) => o.id === theme.sectionSpacing)?.value ?? '1.5rem'
  const outerBg = theme.bgColor || '#f6f4ef'
  const fallbackText = isDarkColor(outerBg) ? '#f4f4f5' : '#18181b'

  return {
    backgroundColor: outerBg,
    color: theme.textColor || fallbackText,
    fontFamily: getFontCssStack(theme.fontFamily),
    '--cv-primary': tints.primary,
    '--cv-primary-dark': tints.darkText,
    '--cv-primary-soft': tints.softBg,
    '--cv-primary-border': tints.softBorder,
    '--cv-font-scale': String(resolveFontScalePct(theme) / 100),
    '--cv-line-height': lineHeight,
    '--cv-paragraph-gap': paragraphGap,
    '--cv-section-gap': sectionGap,
  }
}

export function resolvePageWidthPx(theme: PageTheme): number {
  const layout = theme.pageLayout || 'a4'
  if (layout === 'custom') {
    return Math.max(480, Math.min(1600, Math.round(theme.customWidthPx || 900)))
  }
  return PAGE_LAYOUT_OPTIONS.find((o) => o.id === layout)?.width ?? 794
}

export function buildSheetStyle(theme: PageTheme): Record<string, string> {
  const isTransparent = theme.sheetMode === 'transparent'
  const widthPx = resolvePageWidthPx(theme)
  const defaultPad = isTransparent ? 12 : 36
  const pt = theme.marginTop ?? defaultPad
  const pb = theme.marginBottom ?? defaultPad
  const pl = theme.marginLeft ?? (isTransparent ? 16 : 36)
  const pr = theme.marginRight ?? (isTransparent ? 16 : 36)

  const sheetBg = isTransparent ? 'transparent' : (theme.paperBgColor || '#ffffff')
  const surfaceColor = isTransparent ? (theme.bgColor || '#f6f4ef') : sheetBg
  const defaultText = theme.textColor || (isDarkColor(surfaceColor) ? '#f4f4f5' : '#18181b')
  const basePx = (theme.pageLayout || 'a4') === 'a4' ? 13.5 : 14.5

  return {
    maxWidth: `${widthPx}px`,
    width: '100%',
    fontSize: `${((basePx * resolveFontScalePct(theme)) / 100).toFixed(2)}px`,
    paddingTop: `${pt}px`,
    paddingBottom: `${pb}px`,
    paddingLeft: `${pl}px`,
    paddingRight: `${pr}px`,
    backgroundColor: sheetBg,
    color: defaultText,
    borderRadius: isTransparent ? '0px' : '16px',
    border: isTransparent
      ? 'none'
      : `1px solid ${isDarkColor(sheetBg) ? 'rgba(255,255,255,0.12)' : '#e4e4e7'}`,
    boxShadow: isTransparent ? 'none' : '0 10px 30px rgba(15, 23, 42, 0.08)',
  }
}

const DEFAULT_BLUE_FAMILY = ['#1d4ed8', '#1e40af', '#2563eb', '#3b82f6', '#eff6ff', '#bfdbfe', '#dbeafe']

function matchesAccentSource(hex: string | undefined, oldPrimaryHex: string | null): boolean {
  const norm = normalizeHex(hex)
  if (!norm) return false
  if (oldPrimaryHex) {
    if (colorDistance(norm, oldPrimaryHex) < 60) return true
    const oldTints = deriveAccentTints(oldPrimaryHex)
    if (
      colorDistance(norm, oldTints.darkText) < 48 ||
      colorDistance(norm, oldTints.softBg) < 35 ||
      colorDistance(norm, oldTints.softBorder) < 42
    ) {
      return true
    }
  }
  return DEFAULT_BLUE_FAMILY.some((base) => colorDistance(norm, base) < 45)
}

export function computeAccentRecolorUpdates(
  nodes: ContentNode[],
  oldPrimary: string | undefined,
  newPrimary: string,
): Array<{ id: string; attrs: NodeAttrs }> {
  const nextPrimary = normalizeHex(newPrimary)
  if (!nextPrimary) return []
  const oldHex = normalizeHex(oldPrimary)
  const tints = deriveAccentTints(nextPrimary)
  const updates: Array<{ id: string; attrs: NodeAttrs }> = []

  for (const node of nodes) {
    const attrs = JSON.parse(JSON.stringify(node.attrs ?? {})) as NodeAttrs
    let changed = false

    if (node.tag === 'metric') {
      if (!attrs.valueColor || matchesAccentSource(attrs.valueColor, oldHex) || !isNeutralColor(attrs.valueColor)) {
        if (attrs.valueColor !== tints.primary) {
          attrs.valueColor = tints.primary
          changed = true
        }
      }
    } else if (node.tag === 'badge') {
      const hasAccentBadge =
        matchesAccentSource(attrs.textColor, oldHex) ||
        matchesAccentSource(attrs.bgColor, oldHex) ||
        matchesAccentSource(attrs.borderColor, oldHex) ||
        matchesAccentSource(attrs.dotColor, oldHex)
      if (hasAccentBadge) {
        attrs.bgColor = tints.softBg
        attrs.textColor = tints.darkText
        attrs.borderColor = tints.softBorder
        if (attrs.dotColor) attrs.dotColor = tints.primary
        changed = true
      }
    } else if (node.tag === 'heading') {
      if (matchesAccentSource(attrs.textColor, oldHex)) {
        attrs.textColor = tints.darkText
        changed = true
      }
    } else if (node.tag === 'icon') {
      if (matchesAccentSource(attrs.iconColor, oldHex)) {
        attrs.iconColor = tints.primary
        changed = true
      }
    } else if (node.tag === 'link-button') {
      if (matchesAccentSource(attrs.bgColor, oldHex)) {
        attrs.bgColor = tints.primary
        attrs.textColor = isDarkColor(tints.primary) ? '#ffffff' : '#09090b'
        changed = true
      }
    }

    if (attrs.iconColor && node.tag !== 'icon' && matchesAccentSource(attrs.iconColor, oldHex)) {
      attrs.iconColor = tints.primary
      changed = true
    }

    if (changed) {
      updates.push({ id: node.id, attrs })
    }
  }

  return updates
}
