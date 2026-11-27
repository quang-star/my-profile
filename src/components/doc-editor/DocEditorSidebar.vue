<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import {
  ArrowDown,
  ArrowUp,
  Check,
  ChevronLeft,
  ChevronRight,
  FileCode2,
  FileText,
  FileUp,
  Globe,
  GripVertical,
  Languages,
  LayoutGrid,
  ListTree,
  Loader2,
  Maximize2,
  Plus,
  RotateCcw,
  Settings2,
  SlidersHorizontal,
  Sparkles,
  Trash2,
  Upload,
} from '@lucide/vue'
import BlockSettingsPanel from './BlockSettingsPanel.vue'
import { useI18n } from '@/composables/useI18n'
import { DOC_BLOCK_PRESETS, type DocPresetCategory } from '@/editor/documentPresets'
import {
  deleteBlockNode,
  getDocumentOutline,
  inspectorRequestCount,
  moveBlockNode,
  reorderTopLevelBlock,
  type OutlineBlockItem,
} from '@/editor/editorState'
import { pageApi } from '@/services/pageApi'
import {
  parsePageTheme,
  serializePageTheme,
  type PageLanguageMode,
  type PageSummary,
  type PageTheme,
  type PageVisibility,
} from '@/types/saas'
import { formatApiError } from '@/utils/apiErrors'
import {
  BG_COLOR_PRESETS,
  FONT_OPTIONS,
  MARGIN_PRESETS,
  PAGE_LAYOUT_OPTIONS,
  PRIMARY_COLOR_PRESETS,
  SURFACE_THEME_PRESETS,
  resolveFontScalePct,
  resolveLineHeightNumber,
  resolvePageWidthPx,
  resolveParagraphGapPx,
} from '@/utils/pageTheme'

const props = defineProps<{
  editor: Editor | null
  page: PageSummary | null
  theme: PageTheme
  savingTheme?: boolean
}>()

const emit = defineEmits<{
  'update-theme': [patch: Partial<PageTheme>]
  'page-updated': [page: PageSummary]
  'open-import': [file?: File]
  'open-full-settings': []
  'open-publish': []
  'sync-language': [from: 'vi' | 'en', to: 'vi' | 'en']
  'request-reset': []
}>()

const { isVi } = useI18n()
type SidebarTab = 'page-style' | 'blocks' | 'outline' | 'block-settings' | 'import' | 'meta'
const activeTab = ref<SidebarTab>('page-style')
const drawerOpen = ref(true)
const dragOverImport = ref(false)
const importInputRef = ref<HTMLInputElement | null>(null)
const blockCategoryFilter = ref<'all' | DocPresetCategory>('all')
const outlineItems = ref<OutlineBlockItem[]>([])

watch(inspectorRequestCount, (count) => {
  if (count > 0) {
    activeTab.value = 'block-settings'
    drawerOpen.value = true
  }
})

const railTabs: Array<{ id: SidebarTab; vi: string; en: string; icon: unknown }> = [
  { id: 'page-style', vi: 'Trang & Lề', en: 'Page', icon: Maximize2 },
  { id: 'blocks', vi: 'Chèn Khối', en: 'Blocks', icon: LayoutGrid },
  { id: 'outline', vi: 'Bố cục', en: 'Outline', icon: ListTree },
  { id: 'block-settings', vi: 'Sửa Khối', en: 'Block', icon: SlidersHorizontal },
  { id: 'import', vi: 'Nhập CV', en: 'Import', icon: FileUp },
  { id: 'meta', vi: 'Đường dẫn', en: 'URL', icon: Globe },
]

function refreshOutline() {
  outlineItems.value = props.editor ? getDocumentOutline(props.editor) : []
}

let boundEditor: Editor | null = null
watch(
  () => props.editor,
  (nextEd) => {
    if (boundEditor) {
      boundEditor.off('update', refreshOutline)
    }
    boundEditor = nextEd
    if (nextEd) {
      nextEd.on('update', refreshOutline)
    }
    refreshOutline()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  boundEditor?.off('update', refreshOutline)
  boundEditor = null
})

function focusOutlineBlock(pos: number) {
  if (!props.editor) return
  const safePos = Math.min(pos + 1, props.editor.state.doc.content.size)
  props.editor.chain().focus().setTextSelection(safePos).scrollIntoView().run()
}

function moveOutlineBlock(pos: number, dir: -1 | 1) {
  if (!props.editor) return
  moveBlockNode(props.editor, () => pos, dir)
  refreshOutline()
}

function deleteOutlineBlock(pos: number) {
  if (!props.editor) return
  deleteBlockNode(props.editor, () => pos)
  refreshOutline()
}

const dragStartIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

function onOutlineDragStart(idx: number, event: DragEvent) {
  dragStartIndex.value = idx
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(idx))
  }
}

function onOutlineDragOver(idx: number) {
  if (dragStartIndex.value !== null && dragStartIndex.value !== idx) {
    dragOverIndex.value = idx
  }
}

function onOutlineDrop(idx: number) {
  if (props.editor && dragStartIndex.value !== null && dragStartIndex.value !== idx) {
    reorderTopLevelBlock(props.editor, dragStartIndex.value, idx)
    refreshOutline()
  }
  dragStartIndex.value = null
  dragOverIndex.value = null
}

function onOutlineDragEnd() {
  dragStartIndex.value = null
  dragOverIndex.value = null
}

const filteredBlockPresets = computed(() =>
  blockCategoryFilter.value === 'all'
    ? DOC_BLOCK_PRESETS
    : DOC_BLOCK_PRESETS.filter((p) => p.category === blockCategoryFilter.value),
)

function selectTab(tab: SidebarTab) {
  if (activeTab.value === tab) drawerOpen.value = !drawerOpen.value
  else {
    activeTab.value = tab
    drawerOpen.value = true
  }
  if (tab === 'outline') refreshOutline()
}

const currentPrimary = computed(() => props.theme.primaryColor || '#1d4ed8')
const currentOuterBg = computed(() => props.theme.bgColor || '#f6f4ef')
const currentPaperBg = computed(() => props.theme.paperBgColor || '#ffffff')
const currentSheetMode = computed(() => props.theme.sheetMode || 'paper')
const currentLayout = computed(() => props.theme.pageLayout || 'a4')
const currentWidthPx = computed(() => resolvePageWidthPx(props.theme))
const currentFont = computed(() => props.theme.fontFamily || 'inter')
const currentScalePct = computed(() => resolveFontScalePct(props.theme))
const currentLineHeightVal = computed(() => resolveLineHeightNumber(props.theme))
const currentParagraphGap = computed(() => resolveParagraphGapPx(props.theme))

const marginTop = computed(() => props.theme.marginTop ?? (currentSheetMode.value === 'transparent' ? 12 : 36))
const marginBottom = computed(() => props.theme.marginBottom ?? (currentSheetMode.value === 'transparent' ? 24 : 36))
const marginLeft = computed(() => props.theme.marginLeft ?? (currentSheetMode.value === 'transparent' ? 16 : 36))
const marginRight = computed(() => props.theme.marginRight ?? (currentSheetMode.value === 'transparent' ? 16 : 36))

function applyUniformMargin(px: number) {
  emit('update-theme', { marginTop: px, marginBottom: px, marginLeft: px, marginRight: px })
}

const autoFitNotice = ref('')
let autoFitTimer: ReturnType<typeof setTimeout> | null = null

function showAutoFitMessage(msg: string) {
  autoFitNotice.value = msg
  if (autoFitTimer) clearTimeout(autoFitTimer)
  autoFitTimer = setTimeout(() => {
    autoFitNotice.value = ''
  }, 3200)
}

function autoFitOnePage() {
  const sheetEl = document.querySelector('.cv-doc-sheet') as HTMLElement | null
  if (!sheetEl) return
  const TARGET_H = 1116
  const h = sheetEl.offsetHeight
  if (h <= 1122) {
    showAutoFitMessage(isVi.value ? 'Trang đã vừa gọn 1 trang A4!' : 'Already fits cleanly on 1 A4 page!')
    return
  }

  const ratio = TARGET_H / h
  const nextScale = Math.max(82, Math.min(100, Math.round(currentScalePct.value * Math.sqrt(ratio))))
  const nextLineHeight = Number(Math.max(1.15, Math.min(1.5, currentLineHeightVal.value - (1 - ratio) * 0.45)).toFixed(2))
  const nextGap = Math.max(1, Math.min(currentParagraphGap.value, Math.round(currentParagraphGap.value * ratio)))
  const nextMarginV = Math.max(16, Math.min(marginTop.value, Math.round(marginTop.value * ratio)))

  emit('update-theme', {
    fontScalePct: nextScale,
    fontScale: undefined,
    lineHeightValue: nextLineHeight,
    paragraphGapPx: nextGap,
    marginTop: nextMarginV,
    marginBottom: nextMarginV,
  })

  // Second fine-tune pass after Vue updates CSS vars
  setTimeout(() => {
    const newH = sheetEl.offsetHeight
    if (newH > 1122) {
      const fineScale = Math.max(80, Math.round(nextScale * Math.sqrt(TARGET_H / newH)))
      emit('update-theme', {
        fontScalePct: fineScale,
        fontScale: undefined,
        lineHeightValue: Math.max(1.12, Number((nextLineHeight - 0.05).toFixed(2))),
        marginTop: 14,
        marginBottom: 14,
        paragraphGapPx: 1,
      })
      setTimeout(() => {
        showAutoFitMessage(
          sheetEl.offsetHeight <= 1122
            ? (isVi.value ? 'Đã co vừa khít 1 trang A4!' : 'Fitted cleanly onto 1 A4 page!')
            : (isVi.value ? 'Nội dung khá dài, đã co tối đa mức dễ đọc.' : 'Content is long — compacted as much as readable.'),
        )
      }, 60)
    } else {
      showAutoFitMessage(isVi.value ? 'Đã co vừa khít 1 trang A4!' : 'Fitted cleanly onto 1 A4 page!')
    }
  }, 80)
}

function resetA4Spacing() {
  emit('update-theme', {
    fontScalePct: 100,
    fontScale: undefined,
    lineHeightValue: 1.45,
    paragraphGapPx: 4,
    marginTop: 28,
    marginBottom: 28,
  })
  showAutoFitMessage(isVi.value ? 'Đã đưa mật độ chữ & lề về mặc định.' : 'Spacing reset to default.')
}

function insertBlockPreset(presetId: string) {
  const preset = DOC_BLOCK_PRESETS.find((p) => p.id === presetId)
  if (!preset || !props.editor) return
  props.editor.chain().focus().insertContent(preset.build(isVi.value)).run()
}

function onQuickImportFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) emit('open-import', file)
}

function onQuickImportDrop(event: DragEvent) {
  dragOverImport.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) emit('open-import', file)
}

// Inline Page Meta Form (Tab 5)
const pageForm = reactive({
  title: '',
  slug: '',
  description: '',
  visibility: 'DRAFT' as PageVisibility,
  isPrimary: false,
  languageMode: 'bilingual' as PageLanguageMode,
})
const savingPage = ref(false)
const pageSaveMsg = ref('')
const pageSaveError = ref('')

watch(
  () => props.page,
  (p) => {
    if (!p) return
    pageForm.title = p.title
    pageForm.slug = p.slug
    pageForm.description = p.description || ''
    pageForm.visibility = (p.visibility as PageVisibility) || 'DRAFT'
    pageForm.isPrimary = p.isPrimary
    pageForm.languageMode = parsePageTheme(p.themeJson).languageMode || 'bilingual'
  },
  { immediate: true },
)

function onSlugInput(event: Event) {
  pageForm.slug = (event.target as HTMLInputElement).value.toLowerCase().replace(/[^a-z0-9-]/g, '')
}

async function savePageMeta() {
  if (!props.page || savingPage.value) return
  pageSaveError.value = ''
  pageSaveMsg.value = ''
  if (!pageForm.title.trim()) {
    pageSaveError.value = isVi.value ? 'Tiêu đề không được để trống.' : 'Title is required.'
    return
  }
  const cleanSlug = pageForm.slug.trim()
  if (!/^[a-z0-9-]{1,64}$/.test(cleanSlug) || cleanSlug === 'edit') {
    pageSaveError.value = isVi.value ? 'Slug không hợp lệ hoặc trùng từ khóa hệ thống.' : 'Invalid slug.'
    return
  }

  savingPage.value = true
  try {
    const existingTheme = parsePageTheme(props.page.themeJson)
    const themeJson = serializePageTheme({
      ...existingTheme,
      ...props.theme,
      languageMode: pageForm.languageMode,
    })
    const updated = await pageApi.updatePage(props.page.id, {
      title: pageForm.title.trim(),
      slug: cleanSlug,
      description: pageForm.description.trim(),
      visibility: pageForm.visibility,
      isPrimary: pageForm.isPrimary,
      cvPdfUrl: props.page.cvPdfUrl || null,
      themeJson,
    })
    emit('page-updated', updated)
    pageSaveMsg.value = isVi.value ? 'Đã lưu thông tin trang!' : 'Page settings saved!'
    setTimeout(() => { pageSaveMsg.value = '' }, 2200)
  } catch (err) {
    pageSaveError.value = formatApiError(err, isVi.value, 'Không thể lưu thông tin trang.', 'Failed to save page settings')
  } finally {
    savingPage.value = false
  }
}
</script>

<template>
  <aside
    class="cv-doc-sidebar no-print flex shrink-0 select-none"
    data-editor-control
    :aria-label="isVi ? 'Thanh thiết lập trang và khối' : 'Page & Block Sidebar'"
  >
    <!-- Icon Rail -->
    <nav class="flex w-[68px] shrink-0 flex-col items-center justify-between border-r border-zinc-200 bg-white py-3">
      <div class="flex w-full flex-col items-center gap-1.5 px-1.5">
        <button
          v-for="tab in railTabs"
          :key="tab.id"
          type="button"
          class="flex w-full flex-col items-center gap-1 rounded-xl px-1 py-2.5 text-[10px] font-semibold transition-colors"
          :class="
            activeTab === tab.id && drawerOpen
              ? 'bg-emerald-50 text-emerald-700'
              : 'text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900'
          "
          @click="selectTab(tab.id)"
        >
          <component :is="tab.icon" class="size-4.5" />
          <span class="leading-tight">{{ isVi ? tab.vi : tab.en }}</span>
        </button>
      </div>

      <button
        type="button"
        class="mt-2 flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-200 text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900"
        :title="drawerOpen ? (isVi ? 'Thu gọn' : 'Collapse') : (isVi ? 'Mở rộng' : 'Expand')"
        @click="drawerOpen = !drawerOpen"
      >
        <ChevronLeft v-if="drawerOpen" class="size-4" />
        <ChevronRight v-else class="size-4" />
      </button>
    </nav>

    <!-- Collapsible Drawer -->
    <div
      v-if="drawerOpen"
      class="cv-doc-drawer flex w-[308px] flex-col overflow-hidden border-r border-zinc-200 bg-white/95 backdrop-blur-sm"
    >
      <div class="flex items-center justify-between border-b border-zinc-100 px-4 py-3">
        <h2 class="text-xs font-bold uppercase tracking-wider text-zinc-800">
          {{ isVi ? railTabs.find((t) => t.id === activeTab)?.vi : railTabs.find((t) => t.id === activeTab)?.en }}
        </h2>
        <span v-if="savingTheme" class="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-700">
          <Loader2 class="size-3 animate-spin" />
          {{ isVi ? 'Đang lưu…' : 'Saving…' }}
        </span>
      </div>

      <div class="flex-1 overflow-y-auto p-4">
        <!-- TAB 1: PAGE SETUP, MARGINS & DUAL-LAYER BACKGROUNDS -->
        <div v-if="activeTab === 'page-style'" class="space-y-5 text-xs">
          <!-- Surface Presets -->
          <div class="space-y-2">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Kiểu nền & Bề mặt nhanh' : 'Surface presets' }}</span>
            <div class="flex flex-wrap gap-1.5">
              <button
                v-for="preset in SURFACE_THEME_PRESETS"
                :key="preset.id"
                type="button"
                class="rounded-lg border border-zinc-200 bg-white px-2.5 py-1.5 text-[11px] font-semibold text-zinc-700 hover:border-emerald-400 hover:bg-emerald-50/40"
                @click="emit('update-theme', preset.patch)"
              >
                {{ isVi ? preset.vi : preset.en }}
              </button>
            </div>
          </div>

          <!-- Page Width Mode -->
          <div class="space-y-2 border-t border-zinc-100 pt-4">
            <div class="flex items-center justify-between">
              <span class="font-bold text-zinc-800">{{ isVi ? 'Khổ trang' : 'Page size' }}</span>
              <span class="font-mono text-[11px] text-zinc-500">{{ currentWidthPx }} px</span>
            </div>
            <div class="grid grid-cols-2 gap-1.5">
              <button
                v-for="opt in PAGE_LAYOUT_OPTIONS"
                :key="opt.id"
                type="button"
                class="rounded-xl border px-2.5 py-2 text-left transition-colors"
                :class="currentLayout === opt.id ? 'border-emerald-600 bg-emerald-50 text-emerald-900' : 'border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50'"
                @click="emit('update-theme', { pageLayout: opt.id, ...(opt.id === 'custom' && !theme.customWidthPx ? { customWidthPx: 900 } : {}) })"
              >
                <p class="font-bold">{{ isVi ? opt.vi : opt.en }}</p>
                <p class="font-mono text-[10px] opacity-70">{{ opt.id === 'custom' ? '480–1600px' : `${opt.width}px` }}</p>
              </button>
            </div>
            <div v-if="currentLayout === 'custom'" class="space-y-1 pt-1">
              <input
                type="range"
                min="480"
                max="1500"
                step="10"
                :value="currentWidthPx"
                class="w-full accent-emerald-600"
                @input="emit('update-theme', { customWidthPx: Number(($event.target as HTMLInputElement).value) })"
              />
            </div>
          </div>

          <!-- Sheet Mode: Paper vs Transparent Web -->
          <div class="space-y-2.5 border-t border-zinc-100 pt-4">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Khung nội dung' : 'Content sheet' }}</span>
            <div class="grid grid-cols-2 gap-1.5">
              <button
                type="button"
                class="rounded-xl border px-2.5 py-2 text-left"
                :class="currentSheetMode === 'paper' ? 'border-emerald-600 bg-emerald-50 text-emerald-900' : 'border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50'"
                @click="emit('update-theme', { sheetMode: 'paper' })"
              >
                <p class="font-bold">{{ isVi ? 'Tờ giấy riêng' : 'Paper sheet' }}</p>
                <p class="text-[10px] opacity-75">{{ isVi ? 'Có nền & bóng giấy' : 'Carded paper surface' }}</p>
              </button>
              <button
                type="button"
                class="rounded-xl border px-2.5 py-2 text-left"
                :class="currentSheetMode === 'transparent' ? 'border-emerald-600 bg-emerald-50 text-emerald-900' : 'border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50'"
                @click="emit('update-theme', { sheetMode: 'transparent' })"
              >
                <p class="font-bold">{{ isVi ? 'Theo nền web' : 'Transparent web' }}</p>
                <p class="text-[10px] opacity-75">{{ isVi ? 'Trong suốt, không khung' : 'Seamless on page bg' }}</p>
              </button>
            </div>

            <!-- Outer Web Background -->
            <div class="space-y-1.5 pt-1">
              <div class="flex items-center justify-between">
                <span class="font-semibold text-zinc-700">{{ isVi ? 'Màu nền ngoài (Web)' : 'Web background' }}</span>
                <span class="font-mono text-[11px] text-zinc-500">{{ currentOuterBg }}</span>
              </div>
              <div class="flex flex-wrap items-center gap-1.5">
                <button
                  v-for="bg in BG_COLOR_PRESETS"
                  :key="bg.hex"
                  type="button"
                  class="h-6 w-6 rounded-full border transition-transform hover:scale-110"
                  :class="currentOuterBg.toLowerCase() === bg.hex ? 'border-zinc-900 ring-2 ring-emerald-500/35' : 'border-zinc-300'"
                  :style="{ backgroundColor: bg.hex }"
                  :title="isVi ? bg.vi : bg.en"
                  @click="emit('update-theme', { bgColor: bg.hex })"
                />
                <input
                  type="color"
                  :value="currentOuterBg"
                  class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
                  @input="emit('update-theme', { bgColor: ($event.target as HTMLInputElement).value })"
                />
              </div>
            </div>

            <!-- Paper Background & Default Text Color -->
            <div class="grid grid-cols-2 gap-2 pt-1">
              <label v-if="currentSheetMode === 'paper'" class="grid gap-1 font-semibold text-zinc-700">
                <span>{{ isVi ? 'Màu tờ giấy' : 'Paper color' }}</span>
                <div class="flex items-center gap-1.5">
                  <input
                    type="color"
                    :value="currentPaperBg"
                    class="h-7 w-9 cursor-pointer rounded border border-zinc-200 p-0"
                    @input="emit('update-theme', { paperBgColor: ($event.target as HTMLInputElement).value })"
                  />
                  <button type="button" class="rounded border border-zinc-200 px-1.5 py-1 text-[10px]" @click="emit('update-theme', { paperBgColor: '#ffffff' })">Trắng</button>
                </div>
              </label>

              <label class="grid gap-1 font-semibold text-zinc-700">
                <span>{{ isVi ? 'Màu chữ chung' : 'Default text' }}</span>
                <div class="flex items-center gap-1.5">
                  <input
                    type="color"
                    :value="theme.textColor || '#18181b'"
                    class="h-7 w-9 cursor-pointer rounded border border-zinc-200 p-0"
                    @input="emit('update-theme', { textColor: ($event.target as HTMLInputElement).value })"
                  />
                  <button type="button" class="rounded border border-zinc-200 px-1.5 py-1 text-[10px]" @click="emit('update-theme', { textColor: undefined })">Auto</button>
                </div>
              </label>
            </div>
          </div>

          <!-- 4-side Margins -->
          <div class="space-y-2 border-t border-zinc-100 pt-4">
            <div class="flex items-center justify-between">
              <span class="font-bold text-zinc-800">{{ isVi ? 'Căn lề trang (px)' : 'Page margins (px)' }}</span>
              <div class="flex gap-1">
                <button
                  v-for="m in MARGIN_PRESETS"
                  :key="m.id"
                  type="button"
                  class="rounded border border-zinc-200 px-1.5 py-0.5 text-[10px] font-semibold hover:bg-zinc-50"
                  @click="applyUniformMargin(m.px)"
                >
                  {{ isVi ? m.vi : m.en }}
                </button>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-2">
              <label class="grid gap-1">
                <span class="text-[11px] text-zinc-500">{{ isVi ? 'Lề trên' : 'Top' }}</span>
                <input type="number" min="0" max="120" :value="marginTop" class="rounded-lg border border-zinc-200 px-2 py-1" @input="emit('update-theme', { marginTop: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span class="text-[11px] text-zinc-500">{{ isVi ? 'Lề dưới' : 'Bottom' }}</span>
                <input type="number" min="0" max="120" :value="marginBottom" class="rounded-lg border border-zinc-200 px-2 py-1" @input="emit('update-theme', { marginBottom: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span class="text-[11px] text-zinc-500">{{ isVi ? 'Lề trái' : 'Left' }}</span>
                <input type="number" min="0" max="120" :value="marginLeft" class="rounded-lg border border-zinc-200 px-2 py-1" @input="emit('update-theme', { marginLeft: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span class="text-[11px] text-zinc-500">{{ isVi ? 'Lề phải' : 'Right' }}</span>
                <input type="number" min="0" max="120" :value="marginRight" class="rounded-lg border border-zinc-200 px-2 py-1" @input="emit('update-theme', { marginRight: Number(($event.target as HTMLInputElement).value) })" />
              </label>
            </div>
          </div>

          <!-- Primary Accent, Typography & Navbar -->
          <div class="space-y-3 border-t border-zinc-100 pt-4">
            <div class="space-y-1.5">
              <span class="block font-bold text-zinc-800">{{ isVi ? 'Màu chủ đạo (Nhấn)' : 'Accent color' }}</span>
              <div class="flex flex-wrap items-center gap-1.5">
                <button
                  v-for="preset in PRIMARY_COLOR_PRESETS"
                  :key="preset.hex"
                  type="button"
                  class="flex h-6 w-6 items-center justify-center rounded-full border transition-transform hover:scale-110"
                  :class="currentPrimary.toLowerCase() === preset.hex ? 'border-zinc-900 ring-2 ring-emerald-500/35' : 'border-zinc-200'"
                  :style="{ backgroundColor: preset.hex }"
                  :title="isVi ? preset.vi : preset.en"
                  @click="emit('update-theme', { primaryColor: preset.hex })"
                >
                  <Check v-if="currentPrimary.toLowerCase() === preset.hex" class="size-3 text-white drop-shadow" />
                </button>
                <input
                  type="color"
                  :value="currentPrimary"
                  class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
                  @input="emit('update-theme', { primaryColor: ($event.target as HTMLInputElement).value })"
                />
              </div>
            </div>

            <label class="grid gap-1 font-bold text-zinc-800">
              <span>{{ isVi ? 'Phông chữ mặc định' : 'Base font family' }}</span>
              <select
                :value="currentFont"
                class="rounded-xl border border-zinc-200 bg-white px-2.5 py-2 font-medium"
                @change="emit('update-theme', { fontFamily: ($event.target as HTMLSelectElement).value as PageTheme['fontFamily'] })"
              >
                <option v-for="f in FONT_OPTIONS" :key="f.id" :value="f.id">{{ f.label }}</option>
              </select>
            </label>

            <!-- Font Scale Slider -->
            <div class="space-y-1.5">
              <div class="flex items-center justify-between">
                <span class="font-semibold text-zinc-700">{{ isVi ? 'Cỡ chữ toàn trang' : 'Base font scale' }}</span>
                <div class="flex items-center gap-1.5">
                  <span class="font-mono text-[11px] font-bold text-emerald-700">{{ currentScalePct }}%</span>
                  <button
                    v-if="currentScalePct !== 100"
                    type="button"
                    class="rounded border border-zinc-200 px-1.5 py-0.5 text-[10px] hover:bg-zinc-50"
                    @click="emit('update-theme', { fontScalePct: 100, fontScale: undefined })"
                  >
                    100%
                  </button>
                </div>
              </div>
              <input
                type="range"
                min="80"
                max="130"
                step="2"
                :value="currentScalePct"
                class="w-full accent-emerald-600"
                @input="emit('update-theme', { fontScalePct: Number(($event.target as HTMLInputElement).value) })"
              />
            </div>

            <!-- Line Height Slider + Quick Presets -->
            <div class="space-y-1.5">
              <div class="flex items-center justify-between">
                <span class="font-semibold text-zinc-700">{{ isVi ? 'Giãn dòng toàn trang' : 'Line height' }}</span>
                <span class="font-mono text-[11px] font-bold text-emerald-700">{{ currentLineHeightVal.toFixed(2) }}x</span>
              </div>
              <input
                type="range"
                min="1.0"
                max="2.2"
                step="0.05"
                :value="currentLineHeightVal"
                class="w-full accent-emerald-600"
                @input="emit('update-theme', { lineHeightValue: Number(($event.target as HTMLInputElement).value) })"
              />
              <div class="grid grid-cols-4 gap-1">
                <button
                  v-for="lh in [1.15, 1.4, 1.6, 1.85]"
                  :key="lh"
                  type="button"
                  class="rounded-lg border py-0.5 font-mono text-[10px] font-semibold"
                  :class="Math.abs(currentLineHeightVal - lh) < 0.03 ? 'border-emerald-600 bg-emerald-50 text-emerald-800' : 'border-zinc-200 bg-white hover:bg-zinc-50'"
                  @click="emit('update-theme', { lineHeightValue: lh })"
                >
                  {{ lh }}
                </button>
              </div>
            </div>

            <!-- Paragraph Gap Slider -->
            <div class="space-y-1.5">
              <div class="flex items-center justify-between">
                <span class="font-semibold text-zinc-700">{{ isVi ? 'Khoảng cách đoạn' : 'Paragraph spacing' }}</span>
                <span class="font-mono text-[11px] font-bold text-emerald-700">{{ currentParagraphGap }} px</span>
              </div>
              <input
                type="range"
                min="0"
                max="18"
                step="1"
                :value="currentParagraphGap"
                class="w-full accent-emerald-600"
                @input="emit('update-theme', { paragraphGapPx: Number(($event.target as HTMLInputElement).value) })"
              />
            </div>

            <div
              v-if="currentLayout === 'a4'"
              class="space-y-2 rounded-xl border border-emerald-200/80 bg-emerald-50/50 p-2.5"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="text-[11px] font-bold text-emerald-950">
                  {{ isVi ? 'Tối ưu 1 trang A4' : '1-Page A4 Fit' }}
                </span>
                <div class="flex items-center gap-1">
                  <button
                    type="button"
                    class="rounded-lg border border-zinc-200 bg-white px-2 py-1 text-[10px] font-semibold text-zinc-600 hover:bg-zinc-50"
                    @click="resetA4Spacing"
                  >
                    {{ isVi ? 'Mặc định' : 'Reset' }}
                  </button>
                  <button
                    type="button"
                    class="rounded-lg bg-emerald-600 px-2.5 py-1 text-[11px] font-semibold text-white hover:bg-emerald-700"
                    @click="autoFitOnePage"
                  >
                    {{ isVi ? 'Ép vừa 1 trang' : 'Fit 1 page' }}
                  </button>
                </div>
              </div>
              <p v-if="autoFitNotice" class="text-[11px] font-semibold text-emerald-700">{{ autoFitNotice }}</p>
            </div>

            <label
              v-if="currentLayout === 'a4'"
              class="flex cursor-pointer items-center justify-between gap-2 rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-2 font-medium text-zinc-700"
            >
              <span>{{ isVi ? 'Hiện vạch ngắt trang A4 (Trang 2…)' : 'Show A4 page break guides' }}</span>
              <input
                type="checkbox"
                :checked="theme.showPageBreaks !== false"
                class="rounded border-zinc-300 text-emerald-600"
                @change="emit('update-theme', { showPageBreaks: ($event.target as HTMLInputElement).checked })"
              />
            </label>

            <label class="flex cursor-pointer items-center justify-between gap-2 rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-2 font-medium text-zinc-700">
              <span>{{ isVi ? 'Hiện thanh menu trên trang công khai' : 'Show public top menu bar' }}</span>
              <input
                type="checkbox"
                :checked="!theme.hideNavbar"
                class="rounded border-zinc-300 text-emerald-600"
                @change="emit('update-theme', { hideNavbar: !($event.target as HTMLInputElement).checked })"
              />
            </label>
          </div>
        </div>

        <!-- TAB 2: INSERT BLOCKS -->
        <div v-else-if="activeTab === 'blocks'" class="space-y-2.5">
          <div class="grid grid-cols-3 gap-1 rounded-xl border border-zinc-200 bg-zinc-50 p-1 text-[11px] font-semibold">
            <button
              type="button"
              class="rounded-lg py-1 transition-colors"
              :class="blockCategoryFilter === 'all' ? 'bg-white text-emerald-700 shadow-2xs' : 'text-zinc-600 hover:text-zinc-900'"
              @click="blockCategoryFilter = 'all'"
            >
              {{ isVi ? 'Tất cả' : 'All' }}
            </button>
            <button
              type="button"
              class="rounded-lg py-1 transition-colors"
              :class="blockCategoryFilter === 'page' ? 'bg-white text-emerald-700 shadow-2xs' : 'text-zinc-600 hover:text-zinc-900'"
              @click="blockCategoryFilter = 'page'"
            >
              {{ isVi ? 'Trang & Web' : 'Page & Web' }}
            </button>
            <button
              type="button"
              class="rounded-lg py-1 transition-colors"
              :class="blockCategoryFilter === 'cv' ? 'bg-white text-emerald-700 shadow-2xs' : 'text-zinc-600 hover:text-zinc-900'"
              @click="blockCategoryFilter = 'cv'"
            >
              {{ isVi ? 'CV / Hồ sơ' : 'CV / Resume' }}
            </button>
          </div>
          <p class="text-[11px] leading-relaxed text-zinc-500">
            {{ isVi ? 'Bấm để chèn khối tại vị trí con trỏ. Hỗ trợ cho cả Portfolio, Bio lẫn CV.' : 'Click to insert at cursor. Works for Portfolios, Bio pages and CVs.' }}
          </p>
          <div class="grid gap-1.5 pt-0.5">
            <button
              v-for="preset in filteredBlockPresets"
              :key="preset.id"
              type="button"
              class="group flex items-center justify-between gap-2 rounded-xl border border-zinc-200 bg-white p-2.5 text-left transition-colors hover:border-emerald-400 hover:bg-emerald-50/40"
              @click="insertBlockPreset(preset.id)"
            >
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-1.5">
                  <span
                    class="rounded px-1.5 py-0.5 text-[9px] font-bold uppercase"
                    :class="preset.category === 'cv' ? 'bg-blue-50 text-blue-700' : 'bg-emerald-50 text-emerald-700'"
                  >
                    {{ preset.category === 'cv' ? 'CV' : 'Web' }}
                  </span>
                  <p class="truncate text-xs font-bold text-zinc-900">{{ isVi ? preset.vi : preset.en }}</p>
                </div>
                <p class="mt-1 line-clamp-2 text-[11px] leading-snug text-zinc-500">{{ isVi ? preset.descVi : preset.descEn }}</p>
              </div>
              <Plus class="size-4 shrink-0 text-zinc-400 group-hover:text-emerald-700" />
            </button>
          </div>
        </div>

        <!-- TAB 3: OUTLINE / LAYOUT -->
        <div v-else-if="activeTab === 'outline'" class="space-y-2.5">
          <div class="flex items-center justify-between text-[11px] text-zinc-500">
            <span>{{ isVi ? 'Sắp xếp thứ tự các khối trên trang' : 'Reorder & jump to page blocks' }}</span>
            <span class="font-mono font-bold text-zinc-700">{{ outlineItems.length }}</span>
          </div>
          <div v-if="!outlineItems.length" class="rounded-xl border border-dashed border-zinc-200 p-4 text-center text-xs text-zinc-500">
            {{ isVi ? 'Trang chưa có khối nào.' : 'No blocks yet.' }}
          </div>
          <div v-else class="space-y-1.5">
            <div
              v-for="(item, idx) in outlineItems"
              :key="`${item.pos}-${idx}`"
              draggable="true"
              class="group flex cursor-grab items-center gap-1.5 rounded-xl border bg-white px-2 py-2 transition-colors active:cursor-grabbing"
              :class="[
                dragOverIndex === idx ? 'border-emerald-500 bg-emerald-50/60' : 'border-zinc-200 hover:border-emerald-400',
                dragStartIndex === idx ? 'opacity-50' : '',
              ]"
              @dragstart="onOutlineDragStart(idx, $event)"
              @dragover.prevent="onOutlineDragOver(idx)"
              @drop.prevent="onOutlineDrop(idx)"
              @dragend="onOutlineDragEnd"
            >
              <GripVertical class="size-3.5 shrink-0 text-zinc-300 group-hover:text-zinc-500" />
              <button
                type="button"
                class="flex min-w-0 flex-1 items-center gap-2 text-left"
                @click="focusOutlineBlock(item.pos)"
              >
                <span class="shrink-0 rounded bg-zinc-100 px-1.5 py-0.5 font-mono text-[10px] font-bold text-zinc-600">
                  {{ item.badge }}
                </span>
                <span class="truncate text-xs font-medium text-zinc-800">{{ item.label }}</span>
              </button>
              <div class="flex shrink-0 items-center gap-0.5">
                <button
                  type="button"
                  class="rounded p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-800 disabled:opacity-30"
                  :disabled="idx === 0"
                  :title="isVi ? 'Đưa lên' : 'Move up'"
                  @click="moveOutlineBlock(item.pos, -1)"
                >
                  <ArrowUp class="size-3" />
                </button>
                <button
                  type="button"
                  class="rounded p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-800 disabled:opacity-30"
                  :disabled="idx === outlineItems.length - 1"
                  :title="isVi ? 'Đưa xuống' : 'Move down'"
                  @click="moveOutlineBlock(item.pos, 1)"
                >
                  <ArrowDown class="size-3" />
                </button>
                <button
                  type="button"
                  class="rounded p-1 text-zinc-400 hover:bg-rose-50 hover:text-rose-600"
                  :title="isVi ? 'Xóa khối' : 'Delete block'"
                  @click="deleteOutlineBlock(item.pos)"
                >
                  <Trash2 class="size-3" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- TAB 3: SELECTED BLOCK SETTINGS -->
        <div v-else-if="activeTab === 'block-settings'">
          <BlockSettingsPanel :editor="editor" />
        </div>

        <!-- TAB 4: IMPORT CV -->
        <div v-else-if="activeTab === 'import'" class="space-y-4">
          <div
            class="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed px-4 py-6 text-center transition-colors"
            :class="dragOverImport ? 'border-emerald-500 bg-emerald-50/60' : 'border-zinc-300 bg-zinc-50/60 hover:border-zinc-400'"
            @dragover.prevent="dragOverImport = true"
            @dragleave.prevent="dragOverImport = false"
            @drop.prevent="onQuickImportDrop"
          >
            <div class="mb-2 flex items-center gap-2">
              <FileText class="size-6 text-rose-500" />
              <FileCode2 class="size-6 text-blue-600" />
            </div>
            <p class="text-xs font-bold text-zinc-900">
              {{ isVi ? 'Nhập từ file PDF hoặc HTML' : 'Import from PDF or HTML' }}
            </p>
            <p class="mt-1 text-[11px] leading-relaxed text-zinc-500">
              {{ isVi ? 'Tự tách cột, banner màu, đoạn văn, kỹ năng và giữ màu chủ đạo vào trình soạn thảo.' : 'Extracts columns, color banners, paragraphs & palette directly into the editor.' }}
            </p>
            <div class="mt-3 flex flex-wrap justify-center gap-1.5">
              <button
                type="button"
                class="inline-flex items-center gap-1 rounded-xl bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700"
                @click="importInputRef?.click()"
              >
                <Upload class="size-3.5" />
                <span>{{ isVi ? 'Chọn file' : 'Choose file' }}</span>
              </button>
              <button
                type="button"
                class="inline-flex items-center gap-1 rounded-xl border border-zinc-200 bg-white px-3 py-1.5 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
                @click="emit('open-import')"
              >
                <Sparkles class="size-3.5 text-emerald-600" />
                <span>{{ isVi ? 'Dán HTML' : 'Paste HTML' }}</span>
              </button>
            </div>
            <input
              ref="importInputRef"
              type="file"
              accept=".pdf,.html,.htm,application/pdf,text/html"
              class="sr-only"
              @change="onQuickImportFile"
            />
          </div>

          <div class="space-y-1.5 border-t border-zinc-100 pt-4">
            <span class="block text-xs font-bold text-zinc-800">{{ isVi ? 'Tiện ích tài liệu' : 'Document utilities' }}</span>
            <button
              type="button"
              class="flex w-full items-center gap-2 rounded-xl border border-zinc-200 bg-white px-3 py-2 text-left text-xs font-medium text-zinc-700 hover:bg-zinc-50"
              @click="emit('sync-language', 'vi', 'en')"
            >
              <Languages class="size-3.5 text-blue-600" />
              <span>{{ isVi ? 'Sao chép nội dung VI → EN' : 'Copy VI content → EN' }}</span>
            </button>
            <button
              type="button"
              class="flex w-full items-center gap-2 rounded-xl border border-zinc-200 bg-white px-3 py-2 text-left text-xs font-medium text-zinc-700 hover:bg-zinc-50"
              @click="emit('sync-language', 'en', 'vi')"
            >
              <Languages class="size-3.5 text-blue-600" />
              <span>{{ isVi ? 'Sao chép nội dung EN → VI' : 'Copy EN content → VI' }}</span>
            </button>
            <button
              type="button"
              class="flex w-full items-center gap-2 rounded-xl border border-rose-200 bg-rose-50/40 px-3 py-2 text-left text-xs font-semibold text-rose-700 hover:bg-rose-50"
              @click="emit('request-reset')"
            >
              <RotateCcw class="size-3.5" />
              <span>{{ isVi ? 'Đặt lại về mẫu khởi đầu' : 'Reset to starter template' }}</span>
            </button>
          </div>
        </div>

        <!-- TAB 5: PAGE URL & META -->
        <div v-else class="space-y-3.5">
          <form class="space-y-3" @submit.prevent="savePageMeta">
            <label class="grid gap-1 text-xs font-semibold text-zinc-700">
              <span>{{ isVi ? 'Tên trang / CV' : 'Page title' }}</span>
              <input
                v-model="pageForm.title"
                type="text"
                maxlength="160"
                class="rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-1.5 text-xs font-normal text-zinc-900 outline-none focus:border-emerald-600 focus:bg-white"
              />
            </label>

            <label class="grid gap-1 text-xs font-semibold text-zinc-700">
              <span>{{ isVi ? 'Đường dẫn (Slug)' : 'URL Slug' }}</span>
              <input
                :value="pageForm.slug"
                type="text"
                maxlength="64"
                class="rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-1.5 font-mono text-xs font-normal text-zinc-900 outline-none focus:border-emerald-600 focus:bg-white"
                @input="onSlugInput"
              />
            </label>

            <label class="grid gap-1 text-xs font-semibold text-zinc-700">
              <span>{{ isVi ? 'Trạng thái hiển thị' : 'Visibility' }}</span>
              <select
                v-model="pageForm.visibility"
                class="rounded-xl border border-zinc-200 bg-zinc-50 px-2.5 py-1.5 text-xs font-medium text-zinc-900 outline-none focus:border-emerald-600 focus:bg-white"
              >
                <option value="DRAFT">{{ isVi ? 'Bản nháp (Chỉ mình bạn)' : 'Draft (Private)' }}</option>
                <option value="PUBLIC">{{ isVi ? 'Công khai (Public)' : 'Public' }}</option>
                <option value="UNLISTED">{{ isVi ? 'Ẩn link (Ai có link xem được)' : 'Unlisted (Link only)' }}</option>
              </select>
            </label>

            <label class="grid gap-1 text-xs font-semibold text-zinc-700">
              <span>{{ isVi ? 'Ngôn ngữ trang' : 'Language mode' }}</span>
              <select
                v-model="pageForm.languageMode"
                class="rounded-xl border border-zinc-200 bg-zinc-50 px-2.5 py-1.5 text-xs font-medium text-zinc-900 outline-none focus:border-emerald-600 focus:bg-white"
              >
                <option value="bilingual">{{ isVi ? 'Song ngữ VI + EN' : 'Bilingual VI + EN' }}</option>
                <option value="vi">{{ isVi ? 'Chỉ Tiếng Việt' : 'Vietnamese only' }}</option>
                <option value="en">{{ isVi ? 'Chỉ Tiếng Anh' : 'English only' }}</option>
              </select>
            </label>

            <label class="grid gap-1 text-xs font-semibold text-zinc-700">
              <span>{{ isVi ? 'Mô tả ngắn' : 'Description' }}</span>
              <textarea
                v-model="pageForm.description"
                rows="2"
                maxlength="500"
                class="rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-1.5 text-xs font-normal text-zinc-900 outline-none focus:border-emerald-600 focus:bg-white"
              />
            </label>

            <label class="flex cursor-pointer items-center gap-2 rounded-xl border border-zinc-200 bg-zinc-50/70 px-3 py-2 text-xs font-medium text-zinc-700">
              <input v-model="pageForm.isPrimary" type="checkbox" class="rounded border-zinc-300 text-emerald-600" />
              <span>{{ isVi ? 'Đặt làm trang chính (/@username)' : 'Set as primary page' }}</span>
            </label>

            <p v-if="pageSaveError" class="rounded-lg bg-rose-50 px-2.5 py-1.5 text-[11px] font-medium text-rose-700">
              {{ pageSaveError }}
            </p>
            <p v-if="pageSaveMsg" class="rounded-lg bg-emerald-50 px-2.5 py-1.5 text-[11px] font-medium text-emerald-700">
              {{ pageSaveMsg }}
            </p>

            <button
              type="submit"
              class="w-full rounded-xl bg-emerald-600 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
              :disabled="savingPage"
            >
              {{ savingPage ? (isVi ? 'Đang lưu…' : 'Saving…') : (isVi ? 'Lưu thông tin trang' : 'Save page info') }}
            </button>
          </form>

          <div class="space-y-1.5 border-t border-zinc-100 pt-3">
            <button
              type="button"
              class="flex w-full items-center justify-center gap-1.5 rounded-xl border border-zinc-200 bg-white px-3 py-2 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
              @click="emit('open-full-settings')"
            >
              <Settings2 class="size-3.5" />
              <span>{{ isVi ? 'Đính kèm PDF & Ảnh chia sẻ (OG)' : 'PDF Attachment & OG Image' }}</span>
            </button>
            <button
              type="button"
              class="flex w-full items-center justify-center gap-1.5 rounded-xl border border-blue-200 bg-blue-50/60 px-3 py-2 text-xs font-semibold text-blue-700 hover:bg-blue-50"
              @click="emit('open-publish')"
            >
              <Globe class="size-3.5" />
              <span>{{ isVi ? 'Xuất bản & Lấy link chia sẻ' : 'Publish & Share Link' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.cv-doc-sidebar {
  position: sticky;
  top: 93px;
  height: calc(100dvh - 93px);
  z-index: 25;
}

@media (max-width: 1023px) {
  .cv-doc-sidebar {
    position: fixed;
    left: 0;
    top: 93px;
    bottom: 0;
    height: auto;
    z-index: 35;
  }
  .cv-doc-drawer {
    box-shadow: 0 20px 35px rgba(15, 23, 42, 0.18);
  }
}
</style>
