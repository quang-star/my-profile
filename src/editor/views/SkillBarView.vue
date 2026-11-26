<script setup lang="ts">
import { computed } from 'vue'
import { Settings2, Trash2 } from '@lucide/vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { deleteBlockNode, requestOpenBlockSettings } from '@/editor/editorState'
import type { SkillBarAttrs } from '@/editor/extensions/SkillBarBlock'

const props = defineProps(nodeViewProps)
const editable = computed(() => props.editor.isEditable)
const attrs = computed(() => props.node.attrs as SkillBarAttrs)

const pct = computed(() => Math.max(5, Math.min(100, Number(attrs.value.level ?? 85))))
const barColor = computed(() => attrs.value.color || 'var(--cv-primary, #1d4ed8)')

function onLabelInput(event: Event) {
  props.updateAttributes({ label: (event.target as HTMLInputElement).value })
}

function onLevelInput(event: Event) {
  props.updateAttributes({ level: Number((event.target as HTMLInputElement).value) })
}

function openSettings() {
  const pos = props.getPos()
  if (typeof pos === 'number') {
    props.editor.commands.setNodeSelection(pos)
    requestOpenBlockSettings(pos)
  }
}
</script>

<template>
  <NodeViewWrapper
    class="group/skill relative rounded-lg"
    :class="{ 'ring-2 ring-emerald-500 ring-offset-2': editable && selected }"
    :style="{ marginBottom: `${Number(attrs.marginBottom ?? 10)}px` }"
    contenteditable="false"
  >
    <div
      v-if="editable"
      class="no-print absolute -top-3 right-1 z-10 flex items-center gap-0.5 rounded-md border border-zinc-200 bg-white px-1 py-0.5 text-[10px] opacity-0 shadow-xs transition-opacity group-hover/skill:opacity-100"
      :class="{ '!opacity-100': selected }"
    >
      <button type="button" class="rounded p-0.5 text-zinc-600 hover:bg-zinc-100" title="Cài đặt" @click.stop="openSettings">
        <Settings2 class="size-3" />
      </button>
      <button type="button" class="rounded p-0.5 text-rose-600 hover:bg-rose-50" title="Xóa" @click.stop="deleteBlockNode(editor, getPos)">
        <Trash2 class="size-3" />
      </button>
    </div>

    <div class="mb-1 flex items-center justify-between gap-2 text-xs font-semibold">
      <input
        v-if="editable"
        :value="attrs.label"
        type="text"
        class="min-w-0 flex-1 bg-transparent font-semibold outline-none focus:underline"
        placeholder="Tên kỹ năng..."
        @input="onLabelInput"
        @click.stop
      />
      <span v-else class="min-w-0 flex-1">{{ attrs.label }}</span>
      <span v-if="attrs.showPercent !== false" class="shrink-0 font-mono text-[11px] opacity-75">
        {{ pct }}%
      </span>
    </div>

    <div class="relative h-2 w-full overflow-hidden rounded-full bg-zinc-200/80">
      <div
        class="h-full rounded-full transition-all"
        :style="{ width: `${pct}%`, backgroundColor: barColor }"
      />
      <input
        v-if="editable"
        type="range"
        min="5"
        max="100"
        step="5"
        :value="pct"
        class="no-print absolute inset-0 h-full w-full cursor-pointer opacity-0"
        title="Kéo để chỉnh mức %"
        @input="onLevelInput"
        @click.stop
      />
    </div>
  </NodeViewWrapper>
</template>
