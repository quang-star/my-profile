<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, ArrowUp, GripVertical, Settings2, Trash2 } from '@lucide/vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { deleteBlockNode, moveBlockNode, requestOpenBlockSettings } from '@/editor/editorState'
import type { DividerAttrs } from '@/editor/extensions/DividerBlock'

const props = defineProps(nodeViewProps)
const editable = computed(() => props.editor.isEditable)
const attrs = computed(() => props.node.attrs as DividerAttrs)

const thicknessPx = computed(() => Math.max(1, Math.min(8, Number(attrs.value.thickness ?? 1))))
const widthPct = computed(() => Math.max(15, Math.min(100, Number(attrs.value.widthPct ?? 100))))

const wrapperStyle = computed(() => {
  const a = attrs.value
  const justify =
    a.align === 'center' ? 'center' : a.align === 'right' ? 'flex-end' : 'flex-start'
  return {
    marginTop: `${Math.max(0, Number(a.marginTop ?? 2))}px`,
    marginBottom: `${Math.max(0, Number(a.marginBottom ?? 6))}px`,
    display: 'flex',
    justifyContent: justify,
  }
})

const lineStyleObj = computed(() => {
  const a = attrs.value
  return {
    width: `${widthPct.value}%`,
    height: '0px',
    borderTopWidth: `${thicknessPx.value}px`,
    borderTopStyle: (a.lineStyle || 'solid') as 'solid' | 'dashed' | 'dotted',
    borderTopColor: a.color || 'var(--cv-primary, #18181b)',
  }
})

function selectDivider() {
  if (!editable.value) return
  const pos = props.getPos()
  if (typeof pos === 'number') {
    props.editor.commands.setNodeSelection(pos)
    requestOpenBlockSettings(pos)
  }
}

function cycleThickness() {
  const next = thicknessPx.value >= 3 ? 1 : thicknessPx.value + 1
  props.updateAttributes({ thickness: next })
}
</script>

<template>
  <NodeViewWrapper
    class="cv-divider-wrapper group/div relative py-0.5"
    :class="{ 'is-selected-divider': editable && selected }"
    :style="wrapperStyle"
    contenteditable="false"
    @click.stop="selectDivider"
  >
    <!-- Generous clickable hover target in edit mode -->
    <div
      v-if="editable"
      class="no-print cv-divider-hit absolute -inset-y-1.5 inset-x-0 cursor-pointer rounded transition-colors group-hover/div:bg-emerald-500/10"
      :class="{ 'bg-emerald-500/15 ring-1 ring-emerald-400/70': selected }"
    />

    <!-- Actual visible divider rule -->
    <div class="cv-divider-line pointer-events-none" :style="lineStyleObj" />

    <!-- Centered floating toolbar (never overlaps parent BoxBlock top-right bar) -->
    <div
      v-if="editable"
      class="no-print pointer-events-none absolute -top-3.5 left-1/2 z-20 flex -translate-x-1/2 items-center gap-0.5 rounded-lg border border-zinc-200 bg-white px-1 py-0.5 text-[10px] font-semibold text-zinc-600 opacity-0 shadow-sm transition-opacity group-hover/div:pointer-events-auto group-hover/div:opacity-100"
      :class="{ '!pointer-events-auto !opacity-100 border-emerald-400': selected }"
    >
      <button
        type="button"
        data-drag-handle
        class="cursor-grab rounded p-0.5 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 active:cursor-grabbing"
        title="Kéo thả đường kẻ"
      >
        <GripVertical class="size-3" />
      </button>
      <button
        type="button"
        class="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-emerald-700 hover:bg-emerald-50"
        title="Chỉnh đường kẻ"
        @click.stop="selectDivider"
      >
        <Settings2 class="size-3" />
        <span>Đường kẻ</span>
      </button>
      <button
        type="button"
        class="rounded px-1 py-0.5 font-mono text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900"
        title="Đổi nhanh độ dày (1px / 2px / 3px)"
        @click.stop="cycleThickness"
      >
        {{ thicknessPx }}px
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
        class="rounded p-0.5 text-rose-600 hover:bg-rose-50"
        title="Xóa đường kẻ"
        @click.stop="deleteBlockNode(editor, getPos)"
      >
        <Trash2 class="size-3" />
      </button>
    </div>
  </NodeViewWrapper>
</template>
