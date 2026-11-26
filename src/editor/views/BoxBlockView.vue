<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ArrowDown, ArrowUp, Copy, GripVertical, Settings2, Trash2, Ungroup } from '@lucide/vue'
import { NodeViewContent, NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import {
  deleteBlockNode,
  duplicateBlockNode,
  moveBlockNode,
  requestOpenBlockSettings,
  unwrapBoxNode,
} from '@/editor/editorState'
import type { BoxBlockAttrs } from '@/editor/extensions/BoxBlock'

const props = defineProps(nodeViewProps)

const editable = computed(() => props.editor.isEditable)
const attrs = computed(() => props.node.attrs as BoxBlockAttrs)

const wrapperRef = ref<{ $el?: HTMLElement } | null>(null)
const directHover = ref(false)
const resizing = ref(false)
const liveWidthPct = ref<number | null>(null)

const wrapperStyle = computed(() => {
  const a = attrs.value
  const widthPct = Math.max(20, Math.min(100, Number(a.widthPct ?? 100)))
  const style: Record<string, string> = {
    marginTop: `${Number(a.marginTop ?? 0)}px`,
    marginBottom: `${Number(a.marginBottom ?? 14)}px`,
    width: `${widthPct}%`,
    maxWidth: '100%',
  }
  if (widthPct < 100) {
    if (a.align === 'center') {
      style.marginLeft = 'auto'
      style.marginRight = 'auto'
    } else if (a.align === 'right') {
      style.marginLeft = 'auto'
    }
  }
  return style
})

const boxStyle = computed(() => {
  const a = attrs.value
  const bw = Number(a.borderWidth ?? 1)
  const bc = a.borderColor || '#e4e4e7'
  const side = a.borderSide || 'all'
  const shadowMap = {
    none: 'none',
    sm: '0 1px 3px rgba(15, 23, 42, 0.08)',
    md: '0 8px 24px rgba(15, 23, 42, 0.10)',
  }

  const style: Record<string, string> = {
    backgroundColor: a.bgColor || 'transparent',
    borderRadius: `${Number(a.radius ?? 12)}px`,
    padding: `${Number(a.paddingY ?? 16)}px ${Number(a.paddingX ?? 20)}px`,
    boxShadow: shadowMap[a.shadow || 'none'] || 'none',
    borderStyle: 'solid',
    borderColor: bc,
    borderWidth: '0px',
  }
  if (a.textColor) {
    style.color = a.textColor
  }
  if (bw > 0) {
    if (side === 'all') style.borderWidth = `${bw}px`
    else if (side === 'left') style.borderLeftWidth = `${bw}px`
    else if (side === 'bottom') style.borderBottomWidth = `${bw}px`
    else if (side === 'top') style.borderTopWidth = `${bw}px`
  }
  return style
})

function openSettings() {
  const pos = props.getPos()
  if (typeof pos === 'number') {
    props.editor.commands.setNodeSelection(pos)
    requestOpenBlockSettings(pos)
  }
}

let startX = 0
let startWidthPct = 100
let parentWidthPx = 700

function onResizeMove(event: PointerEvent) {
  if (parentWidthPx <= 0) return
  const deltaPct = ((event.clientX - startX) / parentWidthPx) * 100
  const nextPct = Math.max(25, Math.min(100, Math.round(startWidthPct + deltaPct)))
  liveWidthPct.value = nextPct
  props.updateAttributes({ widthPct: nextPct })
}

function stopResize() {
  resizing.value = false
  liveWidthPct.value = null
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', stopResize)
}

function startResize(event: PointerEvent) {
  if (!editable.value) return
  event.preventDefault()
  event.stopPropagation()

  const hostEl = wrapperRef.value?.$el ?? (event.currentTarget as HTMLElement | null)?.closest('.cv-box-wrapper')
  const containerEl = hostEl?.parentElement
  parentWidthPx = containerEl ? Math.max(200, containerEl.getBoundingClientRect().width) : 700
  startWidthPct = Math.max(25, Math.min(100, Number(attrs.value.widthPct ?? 100)))
  startX = event.clientX
  resizing.value = true
  liveWidthPct.value = startWidthPct

  window.addEventListener('pointermove', onResizeMove)
  window.addEventListener('pointerup', stopResize)
}

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', stopResize)
})
</script>

<template>
  <NodeViewWrapper
    ref="wrapperRef"
    class="cv-box-wrapper relative"
    :class="{
      'is-editable-box': editable,
      'is-direct-hovered': editable && directHover,
      'is-selected-box': editable && selected,
    }"
    :style="wrapperStyle"
    @mouseover.stop="directHover = true"
    @mouseleave="directHover = false"
  >
    <!-- Floating block toolbar in edit mode -->
    <div
      v-if="editable"
      class="no-print cv-block-bar pointer-events-none absolute -top-3.5 right-2 z-20 flex items-center gap-0.5 rounded-lg border border-zinc-200 bg-white px-1 py-0.5 text-[11px] font-semibold text-zinc-600 opacity-0 shadow-sm transition-opacity"
      :class="{
        '!pointer-events-auto !opacity-100': directHover || selected,
        'border-emerald-400': selected,
      }"
      contenteditable="false"
    >
      <button
        type="button"
        data-drag-handle
        class="cursor-grab rounded p-0.5 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 active:cursor-grabbing"
        title="Kéo thả khung"
      >
        <GripVertical class="size-3" />
      </button>
      <button
        type="button"
        class="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-emerald-700 hover:bg-emerald-50"
        title="Cài đặt khung"
        @click.stop="openSettings"
      >
        <Settings2 class="size-3" />
        <span>Khung</span>
      </button>
      <button
        type="button"
        class="rounded p-0.5 hover:bg-zinc-100"
        title="Đưa lên"
        @click.stop="moveBlockNode(editor, getPos, -1)"
      >
        <ArrowUp class="size-3" />
      </button>
      <button
        type="button"
        class="rounded p-0.5 hover:bg-zinc-100"
        title="Đưa xuống"
        @click.stop="moveBlockNode(editor, getPos, 1)"
      >
        <ArrowDown class="size-3" />
      </button>
      <button
        type="button"
        class="rounded p-0.5 hover:bg-zinc-100"
        title="Nhân bản"
        @click.stop="duplicateBlockNode(editor, getPos)"
      >
        <Copy class="size-3" />
      </button>
      <button
        type="button"
        class="rounded p-0.5 hover:bg-zinc-100"
        title="Tách khung (giữ lại nội dung bên trong)"
        @click.stop="unwrapBoxNode(editor, getPos())"
      >
        <Ungroup class="size-3" />
      </button>
      <button
        type="button"
        class="rounded p-0.5 text-rose-600 hover:bg-rose-50"
        title="Xóa khung"
        @click.stop="deleteBlockNode(editor, getPos)"
      >
        <Trash2 class="size-3" />
      </button>
    </div>

    <div class="cv-box-inner relative" :style="boxStyle">
      <NodeViewContent class="cv-box-content" />

      <!-- Right-edge Width Resize Handle -->
      <div
        v-if="editable"
        class="no-print absolute top-2 -right-1.5 bottom-2 z-20 flex w-3 cursor-ew-resize items-center justify-center opacity-0 transition-opacity"
        :class="{ '!opacity-100': directHover || resizing || selected }"
        contenteditable="false"
        title="Kéo để chỉnh chiều rộng khung (%)"
        @pointerdown="startResize"
      >
        <span class="h-7 w-1 rounded-full bg-emerald-500/80 shadow" />
      </div>

      <div
        v-if="resizing && liveWidthPct !== null"
        class="no-print pointer-events-none absolute right-2 bottom-1.5 z-30 rounded bg-zinc-900/85 px-1.5 py-0.5 font-mono text-[10px] font-bold text-white"
        contenteditable="false"
      >
        {{ liveWidthPct }}%
      </div>
    </div>
  </NodeViewWrapper>
</template>

<style scoped>
.is-editable-box > .cv-box-inner {
  transition: outline-color 120ms ease;
}
.is-direct-hovered > .cv-box-inner {
  outline: 1.5px dashed rgba(16, 185, 129, 0.55);
  outline-offset: 2px;
}
.is-selected-box > .cv-box-inner {
  outline: 2px solid #059669;
  outline-offset: 2px;
}
</style>
