<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowDown, ArrowUp, Bold, Copy, Italic, Link2, Minus, Plus, RemoveFormatting, Strikethrough, Trash2, Underline } from '@lucide/vue'
import type { JSONContent } from '@tiptap/core'
import { TextSelection } from '@tiptap/pm/state'
import { Editor, EditorContent } from '@tiptap/vue-3'
import { useI18n } from '@/composables/useI18n'
import { createDocumentExtensions } from '@/editor/createDocumentEditor'
import {
  deleteBlockNode,
  deleteTableRowAt,
  duplicateBlockNode,
  insertTableRowBelow,
  moveBlockNode,
  moveTableRow,
} from '@/editor/editorState'
import type { PageTheme } from '@/types/saas'
import { buildSheetStyle } from '@/utils/pageTheme'

const props = withDefaults(
  defineProps<{
    editor?: Editor | null
    content?: JSONContent | null
    theme: PageTheme
    editable?: boolean
    rightPanelOpen?: boolean
  }>(),
  {
    editor: null,
    content: null,
    editable: false,
    rightPanelOpen: false,
  },
)

const { isVi } = useI18n()

const sheetStyle = computed(() => buildSheetStyle(props.theme))
const sheetRef = ref<HTMLElement | null>(null)
const zoomPct = ref(100)
const sheetHeightPx = ref(0)

// When used in read-only mode (PublicPageShell), instantiate a lightweight read-only Editor
let readonlyEditor: Editor | null = null
if (!props.editor) {
  readonlyEditor = new Editor({
    editable: false,
    content: props.content || { type: 'doc', content: [{ type: 'paragraph' }] },
    extensions: createDocumentExtensions(''),
  })
}

watch(
  () => props.content,
  (next) => {
    if (readonlyEditor && next) {
      readonlyEditor.commands.setContent(next, false)
    }
  },
)

const activeEditor = computed(() => props.editor ?? readonlyEditor)

// A4 Page break guides (only shown in editable A4 mode when enabled)
const A4_PAGE_HEIGHT_PX = 1122
const showPageBreaksOverlay = computed(
  () => props.editable && (props.theme.pageLayout || 'a4') === 'a4' && props.theme.showPageBreaks !== false,
)

const pageBreakPositions = computed(() => {
  if (!showPageBreaksOverlay.value || sheetHeightPx.value <= A4_PAGE_HEIGHT_PX + 24) {
    return []
  }
  const list: number[] = []
  let y = A4_PAGE_HEIGHT_PX
  while (y < sheetHeightPx.value - 24) {
    list.push(y)
    y += A4_PAGE_HEIGHT_PX
  }
  return list
})

let resizeObserver: ResizeObserver | null = null
onMounted(() => {
  if (typeof ResizeObserver !== 'undefined' && sheetRef.value) {
    sheetHeightPx.value = sheetRef.value.offsetHeight
    resizeObserver = new ResizeObserver(() => {
      if (sheetRef.value) {
        sheetHeightPx.value = sheetRef.value.offsetHeight
      }
    })
    resizeObserver.observe(sheetRef.value)
  }
})

// Floating hover controls (Table Row & Top-Level Block)
interface HoverRowState {
  pos: number
  top: number
  right: number
}

interface HoverBlockState {
  pos: number
  top: number
  label: string
}

const hoverRow = ref<HoverRowState | null>(null)
const hoverBlock = ref<HoverBlockState | null>(null)
let hideTimer: ReturnType<typeof setTimeout> | null = null

function cancelHideTimer() {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
}

function scheduleHideHover() {
  cancelHideTimer()
  hideTimer = setTimeout(() => {
    hoverRow.value = null
    hoverBlock.value = null
  }, 150)
}

function getScaleFactor(sheetEl: HTMLElement, sheetRect: DOMRect): number {
  return sheetEl.offsetHeight > 0 ? sheetRect.height / sheetEl.offsetHeight : 1
}

function onSheetMouseMove(event: MouseEvent) {
  if (!props.editable || !activeEditor.value || !sheetRef.value) return
  const target = event.target as HTMLElement | null
  if (!target) return

  cancelHideTimer()

  const sheetEl = sheetRef.value
  const sheetRect = sheetEl.getBoundingClientRect()
  const scale = getScaleFactor(sheetEl, sheetRect)
  const ed = activeEditor.value

  // 1. Check table row hover first
  const trEl = target.closest('.ProseMirror table tr') as HTMLElement | null
  if (trEl && sheetEl.contains(trEl)) {
    try {
      const rawPos = ed.view.posAtDOM(trEl, 0)
      const $pos = ed.state.doc.resolve(Math.max(0, Math.min(rawPos, ed.state.doc.content.size)))
      let rowPos: number | null = null
      for (let d = $pos.depth; d >= 0; d--) {
        if ($pos.node(d).type.name === 'tableRow') {
          rowPos = $pos.before(d)
          break
        }
      }
      if (rowPos !== null) {
        const trRect = trEl.getBoundingClientRect()
        hoverRow.value = {
          pos: rowPos,
          top: Math.max(4, (trRect.top - sheetRect.top) / scale + (trRect.height / scale) / 2 - 14),
          right: Math.max(6, (sheetRect.right - trRect.right) / scale + 4),
        }
        hoverBlock.value = null
        return
      }
    } catch {
      // ignore transient DOM sync errors
    }
  }
  hoverRow.value = null

  // 2. Check standard top-level block hover (skip custom NodeViews which already have their own bars)
  const proseMirror = sheetEl.querySelector('.ProseMirror')
  if (!proseMirror) {
    hoverBlock.value = null
    return
  }

  let el: HTMLElement | null = target
  while (el && el.parentElement !== proseMirror) {
    el = el.parentElement
  }
  if (
    !el ||
    el.classList.contains('cv-box-wrapper') ||
    el.classList.contains('cv-columns-wrapper') ||
    el.hasAttribute('data-node-view-wrapper') ||
    el.classList.contains('ProseMirror-gapcursor')
  ) {
    hoverBlock.value = null
    return
  }

  try {
    const rawPos = ed.view.posAtDOM(el, 0)
    const clampedPos = Math.max(0, Math.min(rawPos, ed.state.doc.content.size))
    const $pos = ed.state.doc.resolve(clampedPos)
    let blockPos: number | null = null
    if ($pos.depth >= 1) {
      blockPos = $pos.before(1)
    } else if (ed.state.doc.nodeAt(clampedPos)) {
      blockPos = clampedPos
    } else if (clampedPos > 0) {
      const prev = ed.state.doc.resolve(clampedPos - 1)
      if (prev.depth >= 1) blockPos = prev.before(1)
    }

    if (blockPos !== null) {
      const node = ed.state.doc.nodeAt(blockPos)
      // Don't float a toolbar on the lone empty placeholder paragraph
      if (!node || (ed.state.doc.childCount === 1 && !node.textContent.trim())) {
        hoverBlock.value = null
        return
      }
      const elRect = el.getBoundingClientRect()
      const typeName = node.type.name
      const label =
        typeName === 'heading'
          ? `H${node.attrs?.level || 2}`
          : typeName === 'table'
            ? (isVi.value ? 'Bảng' : 'Table')
            : typeName === 'bulletList' || typeName === 'orderedList'
              ? (isVi.value ? 'Danh sách' : 'List')
              : typeName === 'horizontalRule'
                ? (isVi.value ? 'Đường kẻ' : 'Line')
                : (isVi.value ? 'Đoạn' : 'Block')
      hoverBlock.value = {
        pos: blockPos,
        top: Math.max(4, (elRect.top - sheetRect.top) / scale - 14),
        label,
      }
      return
    }
  } catch {
    // ignore transient DOM sync errors
  }
  hoverBlock.value = null
}

function handleMoveRow(dir: -1 | 1) {
  if (!activeEditor.value || !hoverRow.value) return
  moveTableRow(activeEditor.value, hoverRow.value.pos, dir)
  hoverRow.value = null
}

function handleAddRowBelow() {
  if (!activeEditor.value || !hoverRow.value) return
  insertTableRowBelow(activeEditor.value, hoverRow.value.pos)
  hoverRow.value = null
}

function handleDeleteRow() {
  if (!activeEditor.value || !hoverRow.value) return
  deleteTableRowAt(activeEditor.value, hoverRow.value.pos)
  hoverRow.value = null
}

function handleMoveTopBlock(dir: -1 | 1) {
  if (!activeEditor.value || !hoverBlock.value) return
  const pos = hoverBlock.value.pos
  moveBlockNode(activeEditor.value, () => pos, dir)
  hoverBlock.value = null
}

function handleDuplicateTopBlock() {
  if (!activeEditor.value || !hoverBlock.value) return
  const pos = hoverBlock.value.pos
  duplicateBlockNode(activeEditor.value, () => pos)
  hoverBlock.value = null
}

function handleDeleteTopBlock() {
  if (!activeEditor.value || !hoverBlock.value) return
  const pos = hoverBlock.value.pos
  deleteBlockNode(activeEditor.value, () => pos)
  hoverBlock.value = null
}

function stepZoom(delta: number) {
  zoomPct.value = Math.max(60, Math.min(140, zoomPct.value + delta))
}

// Floating Selection Bubble Menu
const bubbleMenuRef = ref<HTMLElement | null>(null)
const bubbleMenu = ref<{ top: number; left: number } | null>(null)
const bubbleColorOpen = ref(false)
const bubbleLinkOpen = ref(false)
const bubbleLinkUrl = ref('')
const BUBBLE_COLORS = ['#18181b', '#52525b', '#00b14f', '#1d4ed8', '#be123c', '#b45309']

function updateBubbleMenu() {
  const ed = activeEditor.value
  if (!props.editable || !ed || !sheetRef.value) {
    bubbleMenu.value = null
    return
  }
  const sel = ed.state.selection
  if (sel.empty || !(sel instanceof TextSelection)) {
    if (!bubbleLinkOpen.value) {
      bubbleMenu.value = null
      bubbleColorOpen.value = false
    }
    return
  }

  try {
    const start = ed.view.coordsAtPos(sel.from)
    const end = ed.view.coordsAtPos(sel.to)
    const sheetRect = sheetRef.value.getBoundingClientRect()
    const scale = getScaleFactor(sheetRef.value, sheetRect)
    const rawMidX = ((start.left + end.right) / 2 - sheetRect.left) / scale
    const maxX = Math.max(140, sheetRef.value.offsetWidth - 140)
    const left = Math.max(140, Math.min(maxX, rawMidX))
    const top = Math.max(6, (Math.min(start.top, end.top) - sheetRect.top) / scale - 44)
    bubbleMenu.value = { top, left }
  } catch {
    bubbleMenu.value = null
  }
}

function handleEditorBlur() {
  setTimeout(() => {
    const active = document.activeElement
    if (bubbleMenuRef.value && active && bubbleMenuRef.value.contains(active)) {
      return
    }
    bubbleMenu.value = null
    bubbleColorOpen.value = false
    bubbleLinkOpen.value = false
  }, 120)
}

function stepBubbleFontSize(delta: number) {
  const ed = activeEditor.value
  if (!ed) return
  const raw = String(ed.getAttributes('textStyle')?.fontSize || '14px')
  const num = parseInt(raw, 10) || 14
  const next = Math.max(10, Math.min(44, num + delta))
  ed.chain().focus().setFontSize(`${next}px`).run()
}

function toggleBubbleLink() {
  if (bubbleLinkOpen.value) {
    bubbleLinkOpen.value = false
    return
  }
  bubbleColorOpen.value = false
  bubbleLinkUrl.value = activeEditor.value?.getAttributes('link')?.href || 'https://'
  bubbleLinkOpen.value = true
}

function applyBubbleLink() {
  const ed = activeEditor.value
  if (!ed) return
  const clean = bubbleLinkUrl.value.trim()
  if (!clean || clean === 'https://') {
    ed.chain().focus().unsetLink().run()
  } else {
    ed.chain().focus().setLink({ href: clean }).run()
  }
  bubbleLinkOpen.value = false
}

let boundBubbleEditor: Editor | null = null
watch(
  activeEditor,
  (nextEd) => {
    if (boundBubbleEditor) {
      boundBubbleEditor.off('selectionUpdate', updateBubbleMenu)
      boundBubbleEditor.off('transaction', updateBubbleMenu)
      boundBubbleEditor.off('blur', handleEditorBlur)
    }
    boundBubbleEditor = nextEd
    if (nextEd) {
      nextEd.on('selectionUpdate', updateBubbleMenu)
      nextEd.on('transaction', updateBubbleMenu)
      nextEd.on('blur', handleEditorBlur)
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (boundBubbleEditor) {
    boundBubbleEditor.off('selectionUpdate', updateBubbleMenu)
    boundBubbleEditor.off('transaction', updateBubbleMenu)
    boundBubbleEditor.off('blur', handleEditorBlur)
    boundBubbleEditor = null
  }
  cancelHideTimer()
  resizeObserver?.disconnect()
  resizeObserver = null
  readonlyEditor?.destroy()
  readonlyEditor = null
})
</script>

<template>
  <div class="cv-sheet-outer relative flex w-full justify-center">
    <div
      class="cv-sheet-zoom-wrap flex w-full justify-center"
      :style="editable && zoomPct !== 100 ? { zoom: zoomPct / 100 } : undefined"
    >
      <div
        ref="sheetRef"
        class="cv-doc-sheet relative min-w-0 transition-all"
        :class="{ 'is-transparent-sheet': theme.sheetMode === 'transparent' }"
        :style="sheetStyle"
        @mousemove="onSheetMouseMove"
        @mouseleave="scheduleHideHover"
      >
        <EditorContent
          v-if="activeEditor"
          :editor="activeEditor"
          class="tiptap-doc-content"
        />

        <!-- A4 Page Break Guides -->
        <template v-if="showPageBreaksOverlay">
          <div
            v-for="(breakY, idx) in pageBreakPositions"
            :key="breakY"
            class="no-print pointer-events-none absolute inset-x-0 z-10 flex items-center"
            :style="{ top: `${breakY}px` }"
          >
            <div class="w-full border-t-2 border-dashed border-emerald-500/75" />
            <span
              class="absolute -left-2 -translate-x-full rounded-md bg-emerald-500 px-2 py-0.5 text-[10px] font-bold text-white shadow-sm"
            >
              {{ isVi ? `Trang ${idx + 2}` : `Page ${idx + 2}` }}
            </span>
          </div>
        </template>

        <!-- Floating Table Row Controls on Hover -->
        <div
          v-if="editable && hoverRow"
          class="no-print absolute z-30 flex items-center gap-0.5 rounded-lg border border-zinc-200 bg-white/95 px-1 py-0.5 text-[11px] font-semibold text-zinc-700 shadow-md backdrop-blur-xs"
          :style="{ top: `${hoverRow.top}px`, right: `${hoverRow.right}px` }"
          contenteditable="false"
          @mouseenter="cancelHideTimer"
          @mouseleave="scheduleHideHover"
        >
          <button
            type="button"
            class="rounded p-1 hover:bg-zinc-100"
            :title="isVi ? 'Đưa dòng lên' : 'Move row up'"
            @click.stop="handleMoveRow(-1)"
          >
            <ArrowUp class="size-3" />
          </button>
          <button
            type="button"
            class="rounded p-1 hover:bg-zinc-100"
            :title="isVi ? 'Đưa dòng xuống' : 'Move row down'"
            @click.stop="handleMoveRow(1)"
          >
            <ArrowDown class="size-3" />
          </button>
          <button
            type="button"
            class="inline-flex items-center gap-0.5 rounded bg-emerald-600 px-1.5 py-0.5 text-[10px] font-bold text-white hover:bg-emerald-700"
            :title="isVi ? 'Thêm dòng bên dưới' : 'Add row below'"
            @click.stop="handleAddRowBelow"
          >
            <Plus class="size-3" />
            <span>{{ isVi ? 'Thêm' : 'Add' }}</span>
          </button>
          <button
            type="button"
            class="inline-flex items-center gap-0.5 rounded bg-rose-500 px-1.5 py-0.5 text-[10px] font-bold text-white hover:bg-rose-600"
            :title="isVi ? 'Xóa dòng này' : 'Delete row'"
            @click.stop="handleDeleteRow"
          >
            <Trash2 class="size-3" />
            <span>{{ isVi ? 'Xóa' : 'Del' }}</span>
          </button>
        </div>

        <!-- Floating Top-Level Block Controls on Hover -->
        <div
          v-else-if="editable && hoverBlock"
          class="no-print absolute right-2 z-20 flex items-center gap-0.5 rounded-lg border border-zinc-200 bg-white/95 px-1.5 py-0.5 text-[11px] font-semibold text-zinc-600 shadow-sm backdrop-blur-xs"
          :style="{ top: `${hoverBlock.top}px` }"
          contenteditable="false"
          @mouseenter="cancelHideTimer"
          @mouseleave="scheduleHideHover"
        >
          <span class="px-1 text-[10px] font-bold uppercase text-zinc-400">{{ hoverBlock.label }}</span>
          <button
            type="button"
            class="rounded p-0.5 hover:bg-zinc-100"
            :title="isVi ? 'Đưa lên' : 'Move up'"
            @click.stop="handleMoveTopBlock(-1)"
          >
            <ArrowUp class="size-3" />
          </button>
          <button
            type="button"
            class="rounded p-0.5 hover:bg-zinc-100"
            :title="isVi ? 'Đưa xuống' : 'Move down'"
            @click.stop="handleMoveTopBlock(1)"
          >
            <ArrowDown class="size-3" />
          </button>
          <button
            type="button"
            class="rounded p-0.5 hover:bg-zinc-100"
            :title="isVi ? 'Nhân bản' : 'Duplicate'"
            @click.stop="handleDuplicateTopBlock"
          >
            <Copy class="size-3" />
          </button>
          <button
            type="button"
            class="rounded p-0.5 text-rose-600 hover:bg-rose-50"
            :title="isVi ? 'Xóa' : 'Delete'"
            @click.stop="handleDeleteTopBlock"
          >
            <Trash2 class="size-3" />
          </button>
        </div>

        <!-- Floating Selection Bubble Menu (Bold, Italic, Size, Color, Link) -->
        <div
          v-if="editable && bubbleMenu && activeEditor"
          ref="bubbleMenuRef"
          data-bubble-menu
          class="no-print absolute z-40 -translate-x-1/2 rounded-xl border border-zinc-800 bg-zinc-900 px-1.5 py-1 text-white shadow-xl"
          :style="{ top: `${bubbleMenu.top}px`, left: `${bubbleMenu.left}px` }"
          contenteditable="false"
        >
          <div class="flex items-center gap-0.5 text-[11px]">
            <button
              type="button"
              class="rounded-md p-1.5 hover:bg-zinc-800"
              :class="{ 'bg-emerald-600 text-white': activeEditor.isActive('bold') }"
              title="In đậm"
              @mousedown.prevent
              @click="activeEditor.chain().focus().toggleBold().run()"
            >
              <Bold class="size-3.5" />
            </button>
            <button
              type="button"
              class="rounded-md p-1.5 hover:bg-zinc-800"
              :class="{ 'bg-emerald-600 text-white': activeEditor.isActive('italic') }"
              title="In nghiêng"
              @mousedown.prevent
              @click="activeEditor.chain().focus().toggleItalic().run()"
            >
              <Italic class="size-3.5" />
            </button>
            <button
              type="button"
              class="rounded-md p-1.5 hover:bg-zinc-800"
              :class="{ 'bg-emerald-600 text-white': activeEditor.isActive('underline') }"
              title="Gạch chân"
              @mousedown.prevent
              @click="activeEditor.chain().focus().toggleUnderline().run()"
            >
              <Underline class="size-3.5" />
            </button>
            <button
              type="button"
              class="rounded-md p-1.5 hover:bg-zinc-800"
              :class="{ 'bg-emerald-600 text-white': activeEditor.isActive('strike') }"
              title="Gạch ngang"
              @mousedown.prevent
              @click="activeEditor.chain().focus().toggleStrike().run()"
            >
              <Strikethrough class="size-3.5" />
            </button>

            <span class="mx-0.5 h-4 w-px bg-zinc-700" />

            <button
              type="button"
              class="rounded-md px-1.5 py-1 font-mono text-[10px] font-bold hover:bg-zinc-800"
              title="Giảm cỡ chữ (-1px)"
              @mousedown.prevent
              @click="stepBubbleFontSize(-1)"
            >
              A-
            </button>
            <button
              type="button"
              class="rounded-md px-1.5 py-1 font-mono text-[10px] font-bold hover:bg-zinc-800"
              title="Tăng cỡ chữ (+1px)"
              @mousedown.prevent
              @click="stepBubbleFontSize(1)"
            >
              A+
            </button>

            <button
              type="button"
              class="rounded-md p-1.5 hover:bg-zinc-800"
              title="Màu chữ"
              @mousedown.prevent
              @click="bubbleColorOpen = !bubbleColorOpen; bubbleLinkOpen = false"
            >
              <span class="block size-3.5 rounded-full border border-white/60" :style="{ backgroundColor: activeEditor.getAttributes('textStyle')?.color || '#ffffff' }" />
            </button>
            <button
              type="button"
              class="rounded-md p-1.5 hover:bg-zinc-800"
              :class="{ 'bg-emerald-600 text-white': activeEditor.isActive('link') }"
              title="Gắn liên kết"
              @mousedown.prevent
              @click="toggleBubbleLink"
            >
              <Link2 class="size-3.5" />
            </button>
            <button
              type="button"
              class="rounded-md p-1.5 text-zinc-300 hover:bg-zinc-800 hover:text-white"
              title="Xóa định dạng"
              @mousedown.prevent
              @click="activeEditor.chain().focus().unsetAllMarks().run()"
            >
              <RemoveFormatting class="size-3.5" />
            </button>
          </div>

          <!-- Inline Color Swatches -->
          <div v-if="bubbleColorOpen" class="mt-1.5 flex items-center gap-1 border-t border-zinc-700 pt-1.5">
            <button
              v-for="hex in BUBBLE_COLORS"
              :key="hex"
              type="button"
              class="size-5 rounded-full border border-white/40 transition-transform hover:scale-110"
              :style="{ backgroundColor: hex }"
              @mousedown.prevent
              @click="activeEditor.chain().focus().setColor(hex).run(); bubbleColorOpen = false"
            />
            <button
              type="button"
              class="ml-1 rounded bg-zinc-800 px-1.5 py-0.5 text-[10px] text-zinc-200 hover:bg-zinc-700"
              @mousedown.prevent
              @click="activeEditor.chain().focus().unsetColor().run(); bubbleColorOpen = false"
            >
              Auto
            </button>
          </div>

          <!-- Inline Link Form -->
          <form v-if="bubbleLinkOpen" class="mt-1.5 flex items-center gap-1 border-t border-zinc-700 pt-1.5" @submit.prevent="applyBubbleLink">
            <input
              v-model="bubbleLinkUrl"
              type="text"
              placeholder="https://… hoặc mailto:…"
              class="w-44 rounded border border-zinc-700 bg-zinc-800 px-2 py-1 text-[11px] text-white outline-none focus:border-emerald-500"
              @mousedown.stop
            />
            <button type="submit" class="rounded bg-emerald-600 px-2 py-1 text-[10px] font-bold text-white hover:bg-emerald-500">
              OK
            </button>
            <button
              type="button"
              class="rounded bg-zinc-800 px-1.5 py-1 text-[10px] text-zinc-300 hover:bg-zinc-700"
              @click="bubbleLinkUrl = ''; applyBubbleLink()"
            >
              Gỡ
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- Floating Zoom Control -->
    <div
      v-if="editable"
      class="no-print fixed bottom-5 z-30 flex items-center gap-1 rounded-full border border-zinc-200 bg-white/95 px-2 py-1 text-xs font-semibold text-zinc-700 shadow-lg backdrop-blur-md transition-[right]"
      :class="rightPanelOpen ? 'right-5 xl:right-[364px]' : 'right-5'"
      data-editor-control
    >
      <button
        type="button"
        class="rounded-full p-1 hover:bg-zinc-100 disabled:opacity-40"
        :disabled="zoomPct <= 60"
        :title="isVi ? 'Thu nhỏ' : 'Zoom out'"
        @click="stepZoom(-10)"
      >
        <Minus class="size-3.5" />
      </button>
      <button
        type="button"
        class="min-w-11 rounded-full px-1.5 py-0.5 text-center font-mono text-[11px] font-bold hover:bg-zinc-100"
        :title="isVi ? 'Đặt lại 100%' : 'Reset to 100%'"
        @click="zoomPct = 100"
      >
        {{ zoomPct }}%
      </button>
      <button
        type="button"
        class="rounded-full p-1 hover:bg-zinc-100 disabled:opacity-40"
        :disabled="zoomPct >= 140"
        :title="isVi ? 'Phóng to' : 'Zoom in'"
        @click="stepZoom(10)"
      >
        <Plus class="size-3.5" />
      </button>
    </div>
  </div>
</template>
