<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ArrowDown, ArrowUp, Columns2, Copy, GripVertical, Settings2, Trash2 } from '@lucide/vue'
import { NodeViewContent, NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import {
  deleteBlockNode,
  duplicateBlockNode,
  moveBlockNode,
  requestOpenBlockSettings,
} from '@/editor/editorState'
import type { ColumnsBlockAttrs } from '@/editor/extensions/ColumnsBlock'

const props = defineProps(nodeViewProps)

const editable = computed(() => props.editor.isEditable)
const attrs = computed(() => props.node.attrs as ColumnsBlockAttrs)

const directHover = ref(false)
const gridHostRef = ref<HTMLElement | null>(null)
const draggingSplitterIdx = ref<number | null>(null)
const liveSplitLabel = ref('')

const wrapperStyle = computed(() => ({
  marginTop: `${Number(attrs.value.marginTop ?? 0)}px`,
  marginBottom: `${Number(attrs.value.marginBottom ?? 16)}px`,
}))

const normalizedWidths = computed(() => {
  const count = Math.max(2, props.node.childCount || 2)
  const raw = Array.isArray(attrs.value.widths) && attrs.value.widths.length === count
    ? attrs.value.widths.map((w) => Math.max(10, Number(w) || 10))
    : Array.from({ length: count }, () => Math.round((100 / count) * 10) / 10)
  const sum = raw.reduce((a, b) => a + b, 0) || 100
  return raw.map((w) => Math.round((w / sum) * 100))
})

const splitterOffsets = computed(() => {
  const offsets: Array<{ index: number; leftPct: number }> = []
  let acc = 0
  for (let i = 0; i < normalizedWidths.value.length - 1; i++) {
    acc += normalizedWidths.value[i]!
    offsets.push({ index: i, leftPct: acc })
  }
  return offsets
})

const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: normalizedWidths.value.map((w) => `minmax(0, ${w}fr)`).join(' '),
  gap: `${Number(attrs.value.gap ?? 20)}px`,
  alignItems: 'stretch',
  whiteSpace: 'normal',
}))

function openSettings() {
  const pos = props.getPos()
  if (typeof pos === 'number') {
    props.editor.commands.setNodeSelection(pos)
    requestOpenBlockSettings(pos)
  }
}

let activeIdx = 0
let startX = 0
let containerWidthPx = 600
let startWidths: number[] = []

function onSplitterMove(event: PointerEvent) {
  if (containerWidthPx <= 0) return
  const minCol = startWidths.length >= 4 ? 10 : 15
  const pairSum = (startWidths[activeIdx] ?? 50) + (startWidths[activeIdx + 1] ?? 50)
  const deltaPct = ((event.clientX - startX) / containerWidthPx) * 100
  const nextLeft = Math.max(minCol, Math.min(pairSum - minCol, Math.round((startWidths[activeIdx] ?? 50) + deltaPct)))
  const nextRight = pairSum - nextLeft

  const nextWidths = [...startWidths]
  nextWidths[activeIdx] = nextLeft
  nextWidths[activeIdx + 1] = nextRight
  liveSplitLabel.value = nextWidths.map((w) => `${w}%`).join(' / ')
  props.updateAttributes({ widths: nextWidths })
}

function stopSplitterDrag() {
  draggingSplitterIdx.value = null
  liveSplitLabel.value = ''
  window.removeEventListener('pointermove', onSplitterMove)
  window.removeEventListener('pointerup', stopSplitterDrag)
}

function startSplitterDrag(index: number, event: PointerEvent) {
  if (!editable.value || !gridHostRef.value) return
  event.preventDefault()
  event.stopPropagation()

  activeIdx = index
  draggingSplitterIdx.value = index
  startX = event.clientX
  containerWidthPx = Math.max(200, gridHostRef.value.getBoundingClientRect().width)
  startWidths = [...normalizedWidths.value]
  liveSplitLabel.value = startWidths.map((w) => `${w}%`).join(' / ')

  window.addEventListener('pointermove', onSplitterMove)
  window.addEventListener('pointerup', stopSplitterDrag)
}

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onSplitterMove)
  window.removeEventListener('pointerup', stopSplitterDrag)
})
</script>

<template>
  <NodeViewWrapper
    class="cv-columns-wrapper relative"
    :class="{
      'is-editable-cols': editable,
      'is-direct-hovered-cols': editable && directHover,
      'is-selected-cols': editable && selected,
    }"
    :style="wrapperStyle"
    @mouseover.stop="directHover = true"
    @mouseleave="directHover = false"
  >
    <div
      v-if="editable"
      class="no-print pointer-events-none absolute -top-3.5 right-2 z-20 flex items-center gap-0.5 rounded-lg border border-zinc-200 bg-white px-1 py-0.5 text-[11px] font-semibold text-zinc-600 opacity-0 shadow-sm transition-opacity"
      :class="{
        '!pointer-events-auto !opacity-100': directHover || selected,
        'border-blue-400': selected,
      }"
      contenteditable="false"
    >
      <button
        type="button"
        data-drag-handle
        class="cursor-grab rounded p-0.5 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 active:cursor-grabbing"
        title="Kéo thả khối cột"
      >
        <GripVertical class="size-3" />
      </button>
      <button
        type="button"
        class="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-blue-700 hover:bg-blue-50"
        title="Cài đặt chia cột"
        @click.stop="openSettings"
      >
        <Columns2 class="size-3" />
        <Settings2 class="size-3" />
        <span>Chia cột</span>
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
        class="rounded p-0.5 text-rose-600 hover:bg-rose-50"
        title="Xóa khối cột"
        @click.stop="deleteBlockNode(editor, getPos)"
      >
        <Trash2 class="size-3" />
      </button>
    </div>

    <div ref="gridHostRef" class="relative">
      <NodeViewContent
        class="cv-columns-grid"
        :class="{ 'cv-cols-stack-mobile': attrs.stackMobile !== false }"
        :style="gridStyle"
      />

      <!-- Live Column Width Splitter Handles -->
      <template v-if="editable">
        <div
          v-for="sp in splitterOffsets"
          :key="sp.index"
          class="no-print absolute top-1 bottom-1 z-20 flex w-3 -translate-x-1/2 cursor-col-resize items-center justify-center opacity-0 transition-opacity"
          :class="{ '!opacity-100': directHover || draggingSplitterIdx === sp.index || selected }"
          :style="{ left: `${sp.leftPct}%` }"
          contenteditable="false"
          title="Kéo để chỉnh tỉ lệ chia cột (%)"
          @pointerdown="startSplitterDrag(sp.index, $event)"
        >
          <span class="h-full max-h-16 min-h-6 w-1 rounded-full bg-blue-500/80 shadow" />
        </div>

        <div
          v-if="draggingSplitterIdx !== null && liveSplitLabel"
          class="no-print pointer-events-none absolute top-1/2 left-1/2 z-30 -translate-x-1/2 -translate-y-1/2 rounded-md bg-zinc-900/90 px-2 py-0.5 font-mono text-[11px] font-bold text-white shadow"
          contenteditable="false"
        >
          {{ liveSplitLabel }}
        </div>
      </template>
    </div>
  </NodeViewWrapper>
</template>

<style scoped>
.is-direct-hovered-cols > .relative > .cv-columns-grid {
  outline: 1.5px dashed rgba(37, 99, 235, 0.45);
  outline-offset: 3px;
  border-radius: 10px;
}
.is-selected-cols > .relative > .cv-columns-grid {
  outline: 2px solid #2563eb;
  outline-offset: 3px;
  border-radius: 10px;
}
.is-direct-hovered-cols :deep(.cv-column-cell),
.is-selected-cols :deep(.cv-column-cell) {
  outline: 1px dashed rgba(148, 163, 184, 0.45);
}
</style>
