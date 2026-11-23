<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, useTemplateRef, watch, type CSSProperties } from 'vue'
import { ArrowDown, ArrowUp, CornerUpLeft, ExternalLink, GripVertical, Pencil, Send, Settings2, Star, Trash2 } from '@lucide/vue'
import NodeInsertMenu from './NodeInsertMenu.vue'
import { getNodeIcon } from './nodeIcons'
import { useI18n } from '@/composables/useI18n'
import { useNodes } from '@/composables/useNodes'
import { useCanvasLayout } from '@/composables/useCanvasLayout'
import { resizeHandles, useNodeResize } from '@/composables/useNodeResize'
import { usePortfolio } from '@/composables/usePortfolio'
import { portfolioApi } from '@/services/api'
import type { ContentNode } from '@/types/node'
import type { Locale } from '@/types/profile'
import { sanitizeRichHtml } from '@/utils/sanitizeHtml'

defineOptions({ name: 'NodeRenderer' })

const props = defineProps<{
  node: ContentNode
}>()

const { isVi, locale } = useI18n()
const { editMode } = usePortfolio()
const {
  nodesTree, nodesById, canvasNodeId, draggedNodeId, dragPointer, dropTarget, autoEditNodeId,
  resizingNodeId, activeTextId, visualSaving, runVisualChange,
  canMoveNode, moveNode, moveNodeOrder, saveNodeText, saveNodeAttrs, deleteNode,
  inspectorDraft, workspacePanel,
} = useNodes()

const { canvasWidth, deviceAttrs, saveDeviceAttrs } = useCanvasLayout()
const shell = useTemplateRef<HTMLElement>('shell')
const { resizePreview, startResize, previewResize, finishResize, cancelResize, resizeWithKeys } =
  useNodeResize(
    () => ({ ...props.node, attrs: deviceAttrs(props.node.attrs, props.node.tag === 'grid') }),
    shell,
    (attrs) => saveNodeAttrs(props.node.id, saveDeviceAttrs(props.node.attrs, attrs, props.node.tag === 'grid')),
  )
const selected = computed(() => editMode.value && canvasNodeId.value === props.node.id)
const parent = computed(() => props.node.parentId ? nodesById.value.get(props.node.parentId) : undefined)
const inGrid = computed(() => parent.value?.tag === 'grid')
const effectiveAttrs = computed(() => resizePreview.value ??
  (inspectorDraft.value?.id === props.node.id ? inspectorDraft.value.attrs : deviceAttrs(props.node.attrs, props.node.tag === 'grid')))
const attachedIcon = computed(() => getNodeIcon(effectiveAttrs.value.icon))
const iconSize = computed(() => Math.min(128, Math.max(12, Number(effectiveAttrs.value.iconSize) || (props.node.tag === 'icon' ? 24 : 16))))
const iconBindings = computed(() => ({
  id: effectiveAttrs.value.icon === 'github' ? 'github' as const : undefined,
  style: { width: `${iconSize.value}px`, height: `${iconSize.value}px`, color: effectiveAttrs.value.iconColor },
  class: 'shrink-0',
  'aria-hidden': true,
}))
const canContainBlocks = computed(() => ['section', 'grid', 'card', 'badge-group'].includes(props.node.tag))
const hasLayout = computed(() => {
  const attrs = effectiveAttrs.value
  return inGrid.value || !!(attrs.blockWidth || attrs.minHeight || attrs.blockTop)
})
const gridColumns = computed(() => Math.min(12, Math.max(1, Math.round(Number(effectiveAttrs.value.cols)) || 1)))
const shellStyle = computed<CSSProperties>(() => {
  const attrs = effectiveAttrs.value
  return {
    '--node-grid-span': inGrid.value ? Math.max(1, Math.round(Number(attrs.colSpan) || 1)) : undefined,
    '--node-grid-start': inGrid.value && attrs.colStart ? Math.max(1, Math.round(Number(attrs.colStart))) : undefined,
    minHeight: attrs.minHeight ? `${Math.max(0, Number(attrs.minHeight))}px` : undefined,
    marginTop: attrs.blockTop ? `${Number(attrs.blockTop)}px` : undefined,
    alignSelf: attrs.minHeight && inGrid.value ? 'start' : undefined,
    width: !inGrid.value && attrs.blockWidth ? `${Math.min(100, Math.max(0, Number(attrs.blockWidth)))}%` : undefined,
    marginLeft: !inGrid.value && attrs.blockOffset ? `${Math.min(100, Math.max(0, Number(attrs.blockOffset)))}%` : undefined,
  }
})

function selectNode(event: MouseEvent) {
  if (!editMode.value) return
  event.stopPropagation()
  if (editingText.value || layoutLocked.value) return
  event.preventDefault()
  canvasNodeId.value = props.node.id
  shell.value?.focus({ preventScroll: true })
}

function selectWithKeyboard(event: KeyboardEvent) {
  if (!editMode.value || layoutLocked.value || event.target !== shell.value) return
  canvasNodeId.value = props.node.id
}

function selectParent() {
  if (!parent.value) return
  canvasNodeId.value = parent.value.id
  shell.value?.parentElement?.closest<HTMLElement>('[data-node-id]')?.focus({ preventScroll: true })
}

function showSettings() {
  workspacePanel.value = 'inspector'
}

async function removeSelectedNode() {
  if (layoutLocked.value || draggedNodeId.value) return
  const hasChildren = props.node.children.length > 0
  const prompt = isVi.value
    ? hasChildren ? 'Xóa khối này và toàn bộ nội dung bên trong?' : 'Xóa khối này?'
    : hasChildren ? 'Delete this block and everything inside it?' : 'Delete this block?'
  if (window.confirm(prompt)) await runVisualChange(() => deleteNode(props.node.id))
}
const rawContent = computed(() => {
  if (isVi.value) {
    return props.node.contentVi || props.node.contentEn || ''
  }
  return props.node.contentEn || props.node.contentVi || ''
})

const safeHtml = computed(() => sanitizeRichHtml(rawContent.value))
const inlineTags = ['heading', 'text', 'list', 'badge', 'metric', 'link-button']
const canEditText = computed(() => inlineTags.includes(props.node.tag))
const editingText = computed(() => editMode.value && activeTextId.value === props.node.id)
const layoutLocked = computed(() => visualSaving.value || !!activeTextId.value || !!resizingNodeId.value || !!inspectorDraft.value)
const textElement = useTemplateRef<HTMLElement>('inlineText')
const startHtml = ref('')
const drafts: Partial<Record<Locale, string>> = {}

function translation(language: Locale) {
  return language === 'vi' ? props.node.contentVi : props.node.contentEn
}

const inlineHtml = computed(() => {
  if (editingText.value) return startHtml.value
  if (editMode.value) return sanitizeRichHtml(translation(locale.value))
  return safeHtml.value
})

const inlineKey = computed(() => editingText.value ? locale.value : 'preview')

const inlineAttrs = computed(() => editMode.value ? {
  contenteditable: editingText.value && !visualSaving.value,
  tabindex: 0,
  role: editingText.value ? 'textbox' : undefined,
  'aria-label': isVi.value ? 'Sửa nội dung tiếng Việt' : 'Edit English content',
  'aria-multiline': editingText.value ? ['text', 'list'].includes(props.node.tag) : undefined,
  'data-placeholder': isVi.value ? 'Bấm để nhập nội dung tiếng Việt' : 'Click to enter English text',
} : {})

function rememberText(language: Locale) {
  if (textElement.value) drafts[language] = textElement.value.innerHTML
}

watch(locale, (language, previous) => {
  if (!editingText.value) return
  rememberText(previous)
  startHtml.value = sanitizeRichHtml(drafts[language] ?? translation(language))
}, { flush: 'sync' })

function closeTextEditor(restoreFocus = false) {
  if (activeTextId.value !== props.node.id) return
  activeTextId.value = null
  delete drafts.vi
  delete drafts.en
  if (restoreFocus) void nextTick(() => shell.value?.focus({ preventScroll: true }))
}

watch(editMode, (enabled) => { if (!enabled) closeTextEditor() })
onBeforeUnmount(closeTextEditor)

async function startTextEditor(event?: Event) {
  if (!editMode.value || editingText.value) return
  event?.stopPropagation()
  event?.preventDefault()
  if (layoutLocked.value) return
  canvasNodeId.value = props.node.id
  startHtml.value = sanitizeRichHtml(translation(locale.value))
  activeTextId.value = props.node.id
  await nextTick()
  const element = textElement.value
  if (!element) return
  element.focus()
  const selection = window.getSelection()
  if (!selection) return
  const caret = document.createRange()
  caret.selectNodeContents(element)
  caret.collapse(false)
  selection.removeAllRanges()
  selection.addRange(caret)
}

watch(autoEditNodeId, (id) => {
  if (id !== props.node.id) return
  autoEditNodeId.value = null
  void startTextEditor()
}, { immediate: true, flush: 'post' })

async function saveText() {
  if (!editingText.value || visualSaving.value) return
  rememberText(locale.value)
  const changes: Partial<Pick<ContentNode, 'contentVi' | 'contentEn'>> = {}
  for (const language of ['vi', 'en'] as const) {
    const draft = drafts[language]
    if (draft === undefined) continue
    const html = sanitizeRichHtml(draft)
    if (html !== sanitizeRichHtml(translation(language))) {
      changes[language === 'vi' ? 'contentVi' : 'contentEn'] = html
    }
  }
  if (!Object.keys(changes).length || await runVisualChange(() => saveNodeText(props.node.id, changes))) {
    closeTextEditor(true)
  }
}

function handleTextKey(event: KeyboardEvent) {
  if (!editMode.value || event.isComposing) return
  if (event.key === 'Escape' && editingText.value && !visualSaving.value) {
    event.preventDefault()
    event.stopPropagation()
    closeTextEditor(true)
    return
  }
  if (event.key !== 'Enter') return
  event.preventDefault()
  if (!editingText.value) {
    void startTextEditor()
  } else if (event.ctrlKey || event.metaKey || !['text', 'list'].includes(props.node.tag)) {
    void saveText()
  } else {
    document.execCommand('insertLineBreak')
  }
}

function pasteText(event: ClipboardEvent) {
  if (!editingText.value) return
  event.preventDefault()
  const text = event.clipboardData?.getData('text/plain') ?? ''
  const multiline = ['text', 'list'].includes(props.node.tag)
  document.execCommand('insertText', false, multiline ? text : text.replace(/[\r\n]+/g, ' '))
}

const textEvents = { dblclick: startTextEditor, keydown: handleTextKey, paste: pasteText }
const siblings = computed(() => parent.value?.children ?? nodesTree.value)
const siblingIndex = computed(() => siblings.value.findIndex((node) => node.id === props.node.id))
const nextSibling = computed(() => siblings.value[siblingIndex.value + 1])
const dropEdge = computed(() => dropTarget.value?.id === props.node.id ? dropTarget.value.edge : undefined)
const acceptsInside = computed(() => canContainBlocks.value && draggedNodeId.value &&
  canMoveNode(draggedNodeId.value, props.node.id, 'inside'))

let moveGesture: { pointer: number; handle: HTMLElement; x: number; y: number } | null = null

function startMove(event: PointerEvent, fromHandle = false) {
  if (event.button !== 0 || !event.isPrimary || !selected.value || layoutLocked.value || moveGesture) return
  const handle = event.currentTarget as HTMLElement
  if (fromHandle) {
    event.preventDefault()
    handle.setPointerCapture(event.pointerId)
  } else {
    const target = event.target
    if (event.pointerType === 'touch' || !(target instanceof Element) ||
      target.closest('[data-node-id]') !== handle ||
      target.closest('[data-editor-control], button, input, select, textarea, [contenteditable="true"]')) return
  }
  moveGesture = { pointer: event.pointerId, handle, x: event.clientX, y: event.clientY }
  window.addEventListener('pointermove', updateMove)
  window.addEventListener('pointerup', finishMove)
  window.addEventListener('pointercancel', cancelPointerMove)
  window.addEventListener('blur', cancelMove)
  handle.addEventListener('lostpointercapture', cancelPointerMove)
}

function dropPlacement(id: string, fraction: number) {
  let before = fraction < 0.5
  const position = siblings.value.findIndex((node) => node.id === id)
  const source = siblingIndex.value
  if (position >= 0 && source >= 0) {
    if (position === source + 1 || position === source - 1 || (fraction > 0.25 && fraction < 0.75)) {
      before = position < source
    }
  }
  return before ? 'before' as const : 'after' as const
}

function updateMove(event: PointerEvent) {
  if (moveGesture?.pointer !== event.pointerId) return
  if (!draggedNodeId.value) {
    if (Math.hypot(event.clientX - moveGesture.x, event.clientY - moveGesture.y) < 5) return
    if (event.type === 'pointermove' && !moveGesture.handle.hasPointerCapture(event.pointerId)) {
      moveGesture.handle.setPointerCapture(event.pointerId)
    }
  }
  draggedNodeId.value = props.node.id
  dragPointer.value = { x: event.clientX, y: event.clientY }
  dropTarget.value = null
  let hit = document.elementFromPoint(event.clientX, event.clientY)
  const inside = hit?.closest<HTMLElement>('[data-drop-inside]')?.dataset.dropInside
  if (inside && canMoveNode(props.node.id, inside, 'inside')) {
    dropTarget.value = { id: inside, placement: 'inside', edge: 'inside' }
    return
  }
  while (hit) {
    const target = hit.closest<HTMLElement>('[data-node-id]')
    const id = target?.dataset.nodeId
    if (!target || !id) return
    const bounds = target.getBoundingClientRect()
    const style = target.parentElement ? getComputedStyle(target.parentElement) : null
    const horizontal = (style?.display === 'flex' && style.flexDirection === 'row') ||
      (style?.display === 'grid' && style.gridTemplateColumns.split(' ').length > 1)
    const fraction = horizontal
      ? (event.clientX - bounds.left) / Math.max(bounds.width, 1)
      : (event.clientY - bounds.top) / Math.max(bounds.height, 1)
    const placement = dropPlacement(id, fraction)
    if (canMoveNode(props.node.id, id, placement)) {
      const before = placement === 'before'
      dropTarget.value = { id, placement, edge: horizontal ? (before ? 'left' : 'right') : (before ? 'top' : 'bottom') }
      return
    }
    if (canMoveNode(props.node.id, id, 'inside')) {
      dropTarget.value = { id, placement: 'inside', edge: 'inside' }
      return
    }
    hit = target.parentElement
  }
}

function cancelPointerMove(event: PointerEvent) {
  if (moveGesture?.pointer === event.pointerId) cancelMove()
}

function cancelMove() {
  if (!moveGesture) return
  const { handle, pointer } = moveGesture
  moveGesture = null
  draggedNodeId.value = null
  dragPointer.value = null
  dropTarget.value = null
  window.removeEventListener('pointermove', updateMove)
  window.removeEventListener('pointerup', finishMove)
  window.removeEventListener('pointercancel', cancelPointerMove)
  window.removeEventListener('blur', cancelMove)
  handle.removeEventListener('lostpointercapture', cancelPointerMove)
  if (handle.hasPointerCapture(pointer)) handle.releasePointerCapture(pointer)
}

async function finishMove(event: PointerEvent) {
  if (moveGesture?.pointer !== event.pointerId) return
  updateMove(event)
  const target = dropTarget.value
  cancelMove()
  if (target) await runVisualChange(() => moveNode(props.node.id, target.id, target.placement))
}

onBeforeUnmount(cancelMove)

function cancelNodeAction(event: KeyboardEvent) {
  if (!selected.value || visualSaving.value) return
  if (moveGesture || resizingNodeId.value === props.node.id) {
    event.stopPropagation()
    cancelMove()
    cancelResize()
  }
}

const paddingClass = computed(() => {
  const p = effectiveAttrs.value?.padding
  if (p === 'none') return 'p-0'
  if (p === 'sm') return 'p-4'
  if (p === 'lg') return canvasWidth.value >= 640 ? 'p-8' : 'p-6'
  return canvasWidth.value >= 640 ? 'p-6' : 'p-5'
})

const sectionPaddingClass = computed(() => {
  const p = effectiveAttrs.value?.padding
  if (p === 'none') return 'py-2'
  if (p === 'sm') return 'py-6'
  if (p === 'lg') return canvasWidth.value >= 640 ? 'pt-12 pb-8' : 'py-8'
  return 'py-8'
})

const roundedClass = computed(() => {
  const r = effectiveAttrs.value?.rounded
  if (r === 'none') return 'rounded-none'
  if (r === 'sm') return 'rounded'
  if (r === 'md') return 'rounded-md'
  if (r === 'lg') return 'rounded-lg'
  if (r === 'xl') return 'rounded-xl'
  if (r === '2xl') return 'rounded-2xl'
  if (r === '3xl') return 'rounded-3xl'
  if (r === 'full') return 'rounded-full'
  return 'rounded-lg'
})

const fontSizeClass = computed(() => {
  const f = effectiveAttrs.value?.fontSize
  switch (f) {
    case 'xs':
      return 'text-xs'
    case 'sm':
      return 'text-sm'
    case 'lg':
      return 'text-lg'
    case 'xl':
      return 'text-xl'
    case '2xl':
      return canvasWidth.value >= 640 ? 'text-3xl' : 'text-2xl'
    case '3xl':
      return canvasWidth.value >= 640 ? 'text-4xl' : 'text-3xl'
    case '4xl':
      return canvasWidth.value >= 640 ? 'text-5xl' : 'text-3xl'
    default:
      return 'text-base'
  }
})

const alignClass = computed(() => {
  const a = effectiveAttrs.value?.align
  if (a === 'center') return 'text-center'
  if (a === 'right') return 'text-right'
  return 'text-left'
})

const gridGapClass = computed(() => {
  const gap = Number(effectiveAttrs.value?.gap ?? 4)
  if (gap <= 2) return 'gap-2'
  if (gap === 3) return 'gap-3'
  if (gap === 5) return 'gap-5'
  if (gap >= 6) return 'gap-6'
  return 'gap-4'
})

const stackStyle = computed<CSSProperties>(() => ({
  flexDirection: effectiveAttrs.value.stackDirection || 'column',
  flexWrap: effectiveAttrs.value.stackWrap ? 'wrap' : undefined,
  justifyContent: effectiveAttrs.value.stackJustify,
  gap: effectiveAttrs.value.gap ? `${Math.min(24, Math.max(0, Number(effectiveAttrs.value.gap))) * 4}px` : undefined,
}))

const safeLink = computed(() => {
  try {
    const value = effectiveAttrs.value.href?.trim() || '#'
    const url = new URL(value, 'https://portfolio.invalid/')
    return ['https:', 'http:', 'mailto:', 'tel:'].includes(url.protocol) ? value : '#'
  } catch {
    return '#'
  }
})

const containerStyle = computed(() => {
  const style: Record<string, string> = {}
  if (effectiveAttrs.value?.bgColor && effectiveAttrs.value.bgColor !== 'transparent') {
    style.backgroundColor = String(effectiveAttrs.value.bgColor)
  }
  if (effectiveAttrs.value?.textColor) {
    style.color = String(effectiveAttrs.value.textColor)
  }
  if (effectiveAttrs.value?.borderColor) {
    style.borderColor = String(effectiveAttrs.value.borderColor)
  }
  return style
})

const headingTag = computed(() => {
  const level = effectiveAttrs.value?.level
  if (level === 'h1' || level === 'h2' || level === 'h3' || level === 'h4') {
    return level
  }
  return 'h2'
})

// Contact form state (for contact-form tag)
const contactForm = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})
const contactSending = ref(false)
const contactStatus = ref<'idle' | 'sent' | 'error'>('idle')
const contactError = ref('')

async function submitContactForm() {
  if (!contactForm.name.trim() || !contactForm.email.trim() || !contactForm.message.trim()) return
  contactSending.value = true
  contactStatus.value = 'idle'
  contactError.value = ''
  try {
    await portfolioApi.submitContact({
      name: contactForm.name.trim(),
      email: contactForm.email.trim(),
      subject: contactForm.subject.trim() || 'Portfolio Contact',
      message: contactForm.message.trim(),
    })
    contactStatus.value = 'sent'
    contactForm.name = ''
    contactForm.email = ''
    contactForm.subject = ''
    contactForm.message = ''
  } catch (err) {
    contactStatus.value = 'error'
    contactError.value = err instanceof Error ? err.message : 'Failed to send'
  } finally {
    contactSending.value = false
  }
}
</script>

<template>
  <div
    ref="shell"
    v-show="!effectiveAttrs.hidden || selected"
    :class="[editMode || hasLayout ? 'node-shell relative min-w-0' : 'contents', { 'visual-node': editMode }]"
    :style="hasLayout ? shellStyle : undefined"
    :data-grid-item="inGrid || undefined"
    :data-grid-positioned="(inGrid && effectiveAttrs.colStart) || undefined"
    :data-sized-height="!!effectiveAttrs.minHeight || undefined"
    :data-node-id="editMode ? node.id : undefined"
    :data-hidden-block="effectiveAttrs.hidden || undefined"
    :data-selected="selected || undefined"
    :data-editing="editingText || undefined"
    :data-moving="draggedNodeId === node.id || undefined"
    :data-drop-edge="dropEdge || undefined"
    :tabindex="editMode ? 0 : undefined"
    :aria-label="editMode ? node.tag : undefined"
    @click="selectNode"
    @pointerdown="startMove($event)"
    @dragstart="editMode && $event.preventDefault()"
    @keydown.enter.self="selectWithKeyboard"
    @keydown.esc="cancelNodeAction"
  >
    <div
      v-if="selected"
      class="node-tools no-print absolute right-0 bottom-full z-20 mb-1 flex items-center gap-0.5 rounded-lg border border-slate-200 bg-white p-0.5 text-xs text-slate-700 shadow-sm"
      data-editor-control
      @click.stop
    >
      <template v-if="editingText">
        <button type="button" class="rounded-md bg-emerald-700 px-2 py-1 font-semibold text-white disabled:opacity-50" :disabled="visualSaving" @click="saveText">
          {{ visualSaving ? (isVi ? 'Đang lưu…' : 'Saving…') : (isVi ? 'Lưu' : 'Save') }}
        </button>
        <button type="button" class="rounded-md px-2 py-1 hover:bg-slate-100" :disabled="visualSaving" @click="closeTextEditor(true)">
          {{ isVi ? 'Hủy' : 'Cancel' }}
        </button>
      </template>
      <template v-else>
        <button
          type="button"
          class="node-tool touch-none cursor-grab active:cursor-grabbing"
          :disabled="layoutLocked || (!node.parentId && siblings.length < 2)"
          :aria-label="isVi ? 'Kéo để di chuyển' : 'Drag to move'"
          :title="isVi ? 'Kéo để di chuyển' : 'Drag to move'"
          @pointerdown.stop="startMove($event, true)"
        >
          <GripVertical class="size-3.5" />
        </button>
        <button type="button" class="node-tool" :disabled="layoutLocked || siblingIndex <= 0" :aria-label="isVi ? 'Đưa ra trước' : 'Move earlier'" :title="isVi ? 'Đưa ra trước' : 'Move earlier'" @click="runVisualChange(() => moveNodeOrder(node, -1))">
          <ArrowUp class="size-3.5" />
        </button>
        <button type="button" class="node-tool" :disabled="layoutLocked || siblingIndex >= siblings.length - 1" :aria-label="isVi ? 'Đưa ra sau' : 'Move later'" :title="isVi ? 'Đưa ra sau' : 'Move later'" @click="runVisualChange(() => moveNodeOrder(node, 1))">
          <ArrowDown class="size-3.5" />
        </button>
        <button v-if="canEditText" type="button" class="node-tool" :disabled="layoutLocked" :aria-label="isVi ? 'Sửa chữ' : 'Edit text'" :title="isVi ? 'Sửa chữ' : 'Edit text'" @click="startTextEditor">
          <Pencil class="size-3.5" />
        </button>
        <button v-if="parent" type="button" class="node-tool" :disabled="layoutLocked" :aria-label="isVi ? 'Chọn khối cha' : 'Select parent block'" :title="isVi ? 'Chọn khối cha' : 'Select parent block'" @click="selectParent">
          <CornerUpLeft class="size-3.5" />
        </button>
        <button type="button" class="node-tool" :disabled="layoutLocked" :aria-label="isVi ? 'Thiết lập khối' : 'Block settings'" :title="isVi ? 'Thiết lập khối' : 'Block settings'" @click="showSettings">
          <Settings2 class="size-3.5" />
        </button>
        <button
          type="button"
          class="node-tool inline-flex items-center gap-1 text-rose-700 hover:bg-rose-50"
          :disabled="layoutLocked || !!draggedNodeId"
          :aria-label="isVi ? 'Xóa khối' : 'Delete block'"
          :title="isVi ? 'Xóa khối' : 'Delete block'"
          @click="removeSelectedNode"
        >
          <Trash2 class="size-3.5" />
          <span class="hidden sm:inline">{{ isVi ? 'Xóa' : 'Delete' }}</span>
        </button>
      </template>
    </div>
    <template v-if="selected && !editingText">
      <button
        v-for="{ edge, vi, en } in resizeHandles"
        :key="edge"
        type="button"
        class="resize-handle no-print"
        :data-edge="edge"
        :disabled="visualSaving || !!draggedNodeId"
        :aria-label="isVi ? `Đổi kích thước: ${vi}` : `Resize: ${en}`"
        :title="isVi ? vi : en"
        data-editor-control
        @click.stop
        @pointerdown="startResize($event, edge)"
        @pointermove="previewResize"
        @pointerup="finishResize"
        @pointercancel="cancelResize"
        @lostpointercapture="cancelResize"
        @keydown="resizeWithKeys($event, edge)"
      />
      <div v-if="!draggedNodeId && !resizingNodeId" class="node-add no-print">
        <NodeInsertMenu v-if="canContainBlocks && (node.tag !== 'card' || node.children.length)" :parent-id="node.id" :parent-tag="node.tag" :label="isVi ? 'Thêm khối vào đây' : 'Add block inside'" />
        <NodeInsertMenu v-if="siblingIndex === 0" :parent-id="node.parentId" :parent-tag="parent?.tag" :before-id="node.id" :label="isVi ? 'Chèn trước' : 'Insert before'" />
        <NodeInsertMenu :parent-id="node.parentId" :parent-tag="parent?.tag" :before-id="nextSibling?.id" :label="isVi ? 'Chèn sau' : 'Insert after'" />
      </div>
    </template>
    <div
      v-if="dropEdge"
      class="drop-indicator no-print"
      :data-edge="dropEdge"
      aria-hidden="true"
    >
      <span v-if="dropEdge !== 'inside'" class="drop-indicator-pill">
        {{ dropTarget?.placement === 'before' ? (isVi ? 'Chèn trước' : 'Insert before') : (isVi ? 'Chèn sau' : 'Insert after') }}
      </span>
    </div>
    <div
      v-if="editMode && acceptsInside"
      class="no-print absolute right-2 top-3 z-20 rounded-md border border-dashed px-2.5 py-1 text-xs font-semibold transition-all"
      :class="dropEdge === 'inside'
        ? 'scale-105 border-emerald-700 bg-emerald-600 text-white shadow-md'
        : 'border-emerald-500 bg-emerald-50/95 text-emerald-800'"
      :data-drop-inside="node.id"
    >
      {{ isVi ? 'Thả vào đây' : 'Drop inside' }}
    </div>
    <!-- SECTION -->
    <section
      v-if="node.tag === 'section'"
      :id="effectiveAttrs?.anchor || undefined"
      class="relative"
      :class="sectionPaddingClass"
      :style="containerStyle"
    >

      <div class="flex gap-4" :style="stackStyle">
        <NodeRenderer v-for="child in node.children" :key="child.id" :node="child" />
      </div>
    </section>

    <!-- GRID -->
    <div
      v-else-if="node.tag === 'grid'"
      class="node-grid relative grid"
      :class="[gridGapClass, editMode && !node.children.length ? 'min-h-12' : '']"
      :style="{ ...containerStyle, '--layout-columns': gridColumns }"
    >
      <NodeRenderer v-for="child in node.children" :key="child.id" :node="child" />
    </div>

    <!-- CARD -->
    <div
      v-else-if="node.tag === 'card'"
      class="node-card relative border transition-colors"
      :class="[
        paddingClass,
        roundedClass,
        !effectiveAttrs?.bgColor ? 'bg-white' : '',
        !effectiveAttrs?.borderColor ? 'border-zinc-200' : '',
      ]"
      :style="containerStyle"
    >
      <div
        class="flex"
        :class="[
          effectiveAttrs?.divideY ? 'divide-y divide-zinc-100 gap-0' : 'gap-3.5',
          editMode && !node.children.length ? 'min-h-12' : '',
        ]"
        :style="stackStyle"
      >
        <NodeRenderer v-for="child in node.children" :key="child.id" :node="child" />
        <NodeInsertMenu
          v-if="editMode && !node.children.length"
          class="no-print self-start"
          :parent-id="node.id"
          :parent-tag="node.tag"
          :label="isVi ? 'Thêm khối vào đây' : 'Add block inside'"
        />
      </div>
    </div>

    <!-- HEADING -->
    <component
      :is="headingTag"
      v-else-if="node.tag === 'heading'"
      class="relative font-bold tracking-tight"
      :class="[
        fontSizeClass,
        alignClass,
      ]"
      :style="containerStyle"
    >
      <span :class="attachedIcon ? 'inline-flex items-baseline gap-2' : ''">
        <component v-if="attachedIcon" :is="attachedIcon.component" v-bind="iconBindings" />
        <span :key="inlineKey" ref="inlineText" v-bind="inlineAttrs" v-on="textEvents" v-html="inlineHtml" />
      </span>
    </component>

    <!-- TEXT / LIST (Rich HTML with inline tags & colors) -->
    <div
      v-else-if="node.tag === 'text' || node.tag === 'list'"
      class="rich-node-content relative leading-relaxed"
      :class="[
        fontSizeClass,
        alignClass,
      ]"
      :style="containerStyle"
    >
      <div :class="attachedIcon ? 'flex items-start gap-2' : ''">
        <component v-if="attachedIcon" :is="attachedIcon.component" v-bind="iconBindings" />
        <div :key="inlineKey" ref="inlineText" class="min-w-0 flex-1" v-bind="inlineAttrs" v-on="textEvents" v-html="inlineHtml" />
      </div>
    </div>

    <!-- BADGE GROUP -->
    <div
      v-else-if="node.tag === 'badge-group'"
      class="relative flex flex-wrap items-center"
      :class="[
        Number(effectiveAttrs?.gap ?? 2) >= 3 ? 'gap-3' : 'gap-2',
        editMode && !node.children.length ? 'min-h-8' : '',
      ]"
    >
      <NodeRenderer v-for="child in node.children" :key="child.id" :node="child" />
    </div>

    <!-- BADGE -->
    <span
      v-else-if="node.tag === 'badge'"
      class="inline-flex items-center gap-1.5 rounded border px-2 py-0.5 text-xs font-medium"
      :class="[
        effectiveAttrs?.mono ? 'font-mono' : '',
        !effectiveAttrs?.bgColor ? 'bg-zinc-50' : '',
        !effectiveAttrs?.textColor ? 'text-zinc-700' : '',
        !effectiveAttrs?.borderColor ? 'border-zinc-200' : '',
      ]"
      :style="containerStyle"
    >
      <span
        v-if="effectiveAttrs?.dotColor"
        class="h-1.5 w-1.5 rounded-full"
        :style="{ backgroundColor: String(effectiveAttrs.dotColor) }"
      />
      <component v-if="attachedIcon" :is="attachedIcon.component" v-bind="iconBindings" />
      <span :key="inlineKey" ref="inlineText" v-bind="inlineAttrs" v-on="textEvents" v-html="inlineHtml" />
    </span>

    <div v-else-if="node.tag === 'icon'" class="relative" :class="alignClass" :style="containerStyle">
      <component :is="attachedIcon?.component || Star" v-bind="iconBindings" class="inline-block" />
    </div>

    <!-- METRIC -->
    <div
      v-else-if="node.tag === 'metric'"
      class="rounded-lg border border-transparent p-3.5"
      :style="containerStyle"
    >
      <div
        class="font-mono text-2xl font-bold tracking-tight"
        :style="{ color: effectiveAttrs.valueColor || '#047857' }"
      >
        {{ effectiveAttrs?.value || '0' }}
      </div>
      <div
        class="mt-0.5 text-xs font-medium"
        :style="{ color: effectiveAttrs.labelColor || '#64748b' }"
        :key="inlineKey"
        ref="inlineText"
        v-bind="inlineAttrs"
        v-on="textEvents"
        v-html="inlineHtml"
      />
    </div>

    <!-- LINK BUTTON -->
    <a
      v-else-if="node.tag === 'link-button'"
      :href="safeLink"
      :target="effectiveAttrs?.target === '_blank' ? '_blank' : undefined"
      :rel="effectiveAttrs?.target === '_blank' ? 'noopener noreferrer' : undefined"
      class="inline-flex items-center gap-1.5 rounded border px-3 py-1.5 text-xs font-semibold transition-colors hover:opacity-90"
      :class="[
        !effectiveAttrs?.bgColor ? 'bg-zinc-900' : '',
        !effectiveAttrs?.textColor ? 'text-white' : '',
        !effectiveAttrs?.borderColor ? 'border-transparent' : '',
      ]"
      :style="containerStyle"
      @click="editMode && $event.preventDefault()"
    >
      <component v-if="attachedIcon" :is="attachedIcon.component" v-bind="iconBindings" />
      <ExternalLink v-else class="h-3.5 w-3.5" />
      <span :key="inlineKey" ref="inlineText" v-bind="inlineAttrs" v-on="textEvents" v-html="inlineHtml" />
    </a>

    <!-- IMAGE -->
    <div
      v-else-if="node.tag === 'image'"
    >
      <img
        v-if="effectiveAttrs?.src"
        :src="effectiveAttrs.src"
        :alt="String(effectiveAttrs?.alt || rawContent || 'image')"
        class="max-w-full object-cover"
        :class="roundedClass"
        :style="effectiveAttrs.width ? { width: effectiveAttrs.width } : undefined"
      />
      <p v-else-if="editMode" class="rounded-xl border border-dashed border-slate-300 px-3 py-4 text-sm text-slate-500">
        {{ isVi ? 'Thêm đường dẫn ảnh trong Thiết lập.' : 'Add an image URL in Settings.' }}
      </p>
    </div>

    <!-- DIVIDER -->
    <hr
      v-else-if="node.tag === 'divider'"
      class="my-2 border-t"
      :style="{ borderColor: effectiveAttrs.borderColor || '#e2e8f0' }"
    />

    <!-- CONTACT FORM -->
    <form
      v-else-if="node.tag === 'contact-form'"
      class="mt-2 space-y-3"
      @submit.prevent="!editMode && submitContactForm()"
    >
      <div class="grid gap-3 sm:grid-cols-2">
        <input
          v-model="contactForm.name"
          type="text"
          required
          :placeholder="isVi ? 'Tên của bạn' : 'Your name'"
          class="rounded-lg border border-zinc-200 bg-zinc-50/70 px-3.5 py-2 text-sm focus:border-emerald-600 focus:bg-white focus:outline-none"
        />
        <input
          v-model="contactForm.email"
          type="email"
          required
          placeholder="Email"
          class="rounded-lg border border-zinc-200 bg-zinc-50/70 px-3.5 py-2 text-sm focus:border-emerald-600 focus:bg-white focus:outline-none"
        />
      </div>
      <input
        v-model="contactForm.subject"
        type="text"
        :placeholder="isVi ? 'Tiêu đề' : 'Subject'"
        class="w-full rounded-lg border border-zinc-200 bg-zinc-50/70 px-3.5 py-2 text-sm focus:border-emerald-600 focus:bg-white focus:outline-none"
      />
      <textarea
        v-model="contactForm.message"
        rows="3"
        required
        :placeholder="isVi ? 'Nội dung tin nhắn...' : 'Your message...'"
        class="w-full rounded-lg border border-zinc-200 bg-zinc-50/70 px-3.5 py-2 text-sm focus:border-emerald-600 focus:bg-white focus:outline-none"
      />
      <div class="flex flex-wrap items-center gap-3">
        <button
          type="submit"
          :disabled="contactSending"
          class="inline-flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
          :style="{ backgroundColor: effectiveAttrs.accentColor || '#047857' }"
        >
          <Send class="h-4 w-4" />
          <span>{{ contactSending ? (isVi ? 'Đang gửi...' : 'Sending...') : (isVi ? 'Gửi tin nhắn' : 'Send message') }}</span>
        </button>
        <span v-if="contactStatus === 'sent'" class="text-xs font-semibold text-emerald-600">
          {{ isVi ? 'Đã gửi tin nhắn thành công!' : 'Message sent successfully!' }}
        </span>
        <span v-if="contactStatus === 'error'" class="text-xs font-semibold text-rose-600">
          {{ contactError }}
        </span>
      </div>
    </form>

    <!-- FALLBACK CUSTOM TAG -->
    <div
      v-else
      class="relative rounded-xl border p-3"
      :style="containerStyle"
    >
      <div v-if="safeHtml" class="rich-node-content" v-html="safeHtml" />
      <NodeRenderer v-for="child in node.children" :key="child.id" :node="child" />
    </div>
  </div>
</template>

<style scoped>
.node-grid { grid-template-columns: repeat(var(--layout-columns), minmax(0, 1fr)); }
.node-grid > [data-grid-item] {
  --effective-span: min(var(--node-grid-span, 1), var(--layout-columns));
  grid-column-end: span var(--effective-span);
}
.node-grid > [data-grid-positioned] {
  grid-column-start: min(var(--node-grid-start), calc(var(--layout-columns) - var(--effective-span) + 1));
}
.node-shell > .node-card { height: 100%; }
.node-shell[data-sized-height] { display: grid; }

.visual-node { border-radius: 0.25rem; }
.visual-node:hover:not(:has(.visual-node:hover, [data-editor-control]:hover)) {
  outline: 1px dashed #94a3b8;
  outline-offset: 2px;
}
.visual-node[data-selected] {
  outline: 2px solid #059669;
  outline-offset: 2px;
}
.visual-node[data-selected]:not([data-editing]) {
  cursor: grab;
  user-select: none;
}
.visual-node[data-hidden-block] { opacity: 0.5; }
.visual-node[data-moving] {
  opacity: 0.4;
  outline: 2px dashed #059669;
  outline-offset: 2px;
  background-color: rgba(16, 185, 129, 0.06);
  cursor: grabbing;
}

.node-add {
  position: absolute;
  z-index: 15;
  top: calc(100% + 0.375rem);
  right: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: end;
  gap: 0.25rem;
  width: max-content;
  max-width: calc(100vw - 2rem);
}

.resize-handle {
  position: absolute;
  z-index: 15;
  width: 1rem;
  height: 1rem;
  display: grid;
  place-items: center;
  touch-action: none;
}
.resize-handle::before {
  content: '';
  width: 0.625rem;
  height: 0.625rem;
  border: 1px solid #059669;
  border-radius: 0.125rem;
  background: white;
}
.resize-handle:hover::before, .resize-handle:focus-visible::before { background: #059669; }
.resize-handle[data-edge='w'], .resize-handle[data-edge='e'] { top: 50%; cursor: ew-resize; }
.resize-handle[data-edge='w']::before, .resize-handle[data-edge='e']::before { height: 1rem; }
.resize-handle[data-edge$='w'] { left: 0; transform: translate(-50%, -50%); }
.resize-handle[data-edge$='e'] { right: 0; transform: translate(50%, -50%); }
.resize-handle[data-edge^='n'] { top: 0; }
.resize-handle[data-edge^='s'] { top: 100%; }
.resize-handle[data-edge='nw'], .resize-handle[data-edge='se'] { cursor: nwse-resize; }
.resize-handle[data-edge='ne'], .resize-handle[data-edge='sw'] { cursor: nesw-resize; }

.node-tool {
  border-radius: 0.375rem;
  padding: 0.25rem;
}

.node-tool:hover { background-color: #f1f5f9; }
.node-tool:disabled { opacity: 0.35; }
.node-tool svg { pointer-events: none; }

[data-placeholder] { cursor: default; }

[data-placeholder]:empty::before {
  content: attr(data-placeholder);
  opacity: 0.65;
}

[contenteditable='true'] {
  cursor: text;
  outline: 2px solid #10b981;
  outline-offset: 3px;
  border-radius: 0.25rem;
}

.drop-indicator {
  position: absolute;
  pointer-events: none;
  z-index: 40;
}
.drop-indicator[data-edge='top'],
.drop-indicator[data-edge='bottom'] {
  left: -2px;
  right: -2px;
  height: 3px;
  border-radius: 9999px;
  background: #059669;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.28);
}
.drop-indicator[data-edge='top'] { top: -2px; }
.drop-indicator[data-edge='bottom'] { bottom: -2px; }
.drop-indicator[data-edge='left'],
.drop-indicator[data-edge='right'] {
  top: -2px;
  bottom: -2px;
  width: 3px;
  border-radius: 9999px;
  background: #059669;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.28);
}
.drop-indicator[data-edge='left'] { left: -2px; }
.drop-indicator[data-edge='right'] { right: -2px; }
.drop-indicator[data-edge='top']::before,
.drop-indicator[data-edge='top']::after,
.drop-indicator[data-edge='bottom']::before,
.drop-indicator[data-edge='bottom']::after,
.drop-indicator[data-edge='left']::before,
.drop-indicator[data-edge='left']::after,
.drop-indicator[data-edge='right']::before,
.drop-indicator[data-edge='right']::after {
  content: '';
  position: absolute;
  width: 9px;
  height: 9px;
  border-radius: 9999px;
  background: #059669;
  border: 2px solid #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.25);
}
.drop-indicator[data-edge='top']::before,
.drop-indicator[data-edge='bottom']::before { left: -3px; top: 50%; transform: translateY(-50%); }
.drop-indicator[data-edge='top']::after,
.drop-indicator[data-edge='bottom']::after { right: -3px; top: 50%; transform: translateY(-50%); }
.drop-indicator[data-edge='left']::before,
.drop-indicator[data-edge='right']::before { top: -3px; left: 50%; transform: translateX(-50%); }
.drop-indicator[data-edge='left']::after,
.drop-indicator[data-edge='right']::after { bottom: -3px; left: 50%; transform: translateX(-50%); }
.drop-indicator-pill {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  white-space: nowrap;
  border-radius: 9999px;
  background: #059669;
  color: #fff;
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.4;
  box-shadow: 0 2px 6px rgba(5, 150, 105, 0.35);
}
.drop-indicator[data-edge='inside'] {
  inset: 0;
  border-radius: 0.5rem;
  border: 2px dashed #059669;
  background: rgba(16, 185, 129, 0.1);
}

@media (hover: none) {
  .node-tool { padding: 0.4rem; }
  .resize-handle { width: 1.5rem; height: 1.5rem; }
}
</style>
