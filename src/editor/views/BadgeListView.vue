<script setup lang="ts">
import { computed, ref } from 'vue'
import { Plus, Settings2, Trash2, X } from '@lucide/vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { deleteBlockNode, requestOpenBlockSettings } from '@/editor/editorState'
import type { BadgeListAttrs } from '@/editor/extensions/BadgeListBlock'

const props = defineProps(nodeViewProps)
const editable = computed(() => props.editor.isEditable)
const attrs = computed(() => props.node.attrs as BadgeListAttrs)
const newTag = ref('')

const alignClass = computed(() => {
  if (attrs.value.align === 'center') return 'justify-center'
  if (attrs.value.align === 'right') return 'justify-end'
  return 'justify-start'
})

const chipStyle = computed(() => ({
  backgroundColor: attrs.value.bgColor || 'var(--cv-primary-soft, #eff6ff)',
  color: attrs.value.textColor || 'var(--cv-primary-dark, #1e40af)',
  borderColor: attrs.value.borderColor || 'var(--cv-primary-border, #bfdbfe)',
  borderRadius: `${Number(attrs.value.radius ?? 999)}px`,
}))

function addTag() {
  const val = newTag.value.trim()
  if (!val) return
  props.updateAttributes({ items: [...(attrs.value.items || []), val] })
  newTag.value = ''
}

function removeTag(idx: number) {
  const next = [...(attrs.value.items || [])]
  next.splice(idx, 1)
  props.updateAttributes({ items: next })
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
    class="group/badges relative rounded-lg"
    :class="{ 'ring-2 ring-emerald-500 ring-offset-2': editable && selected }"
    :style="{ marginBottom: `${Number(attrs.marginBottom ?? 10)}px` }"
    contenteditable="false"
  >
    <div
      v-if="editable"
      class="no-print absolute -top-3 right-1 z-10 flex items-center gap-0.5 rounded-md border border-zinc-200 bg-white px-1 py-0.5 text-[10px] opacity-0 shadow-xs transition-opacity group-hover/badges:opacity-100"
      :class="{ '!opacity-100': selected }"
    >
      <button type="button" class="rounded p-0.5 text-zinc-600 hover:bg-zinc-100" title="Cài đặt thẻ" @click.stop="openSettings">
        <Settings2 class="size-3" />
      </button>
      <button type="button" class="rounded p-0.5 text-rose-600 hover:bg-rose-50" title="Xóa" @click.stop="deleteBlockNode(editor, getPos)">
        <Trash2 class="size-3" />
      </button>
    </div>

    <div class="flex flex-wrap items-center gap-1.5" :class="alignClass">
      <span
        v-for="(item, idx) in (attrs.items || [])"
        :key="`${item}-${idx}`"
        class="inline-flex items-center gap-1 border px-2.5 py-0.5 text-xs font-medium"
        :style="chipStyle"
      >
        <span>{{ item }}</span>
        <button
          v-if="editable"
          type="button"
          class="no-print -mr-0.5 rounded-full p-0.5 opacity-60 hover:opacity-100"
          title="Xóa thẻ"
          @click.stop="removeTag(idx)"
        >
          <X class="size-2.5" />
        </button>
      </span>

      <form v-if="editable" class="no-print inline-flex items-center gap-1" @submit.prevent="addTag" @click.stop>
        <input
          v-model="newTag"
          type="text"
          placeholder="+ Thêm thẻ"
          class="w-24 rounded-full border border-dashed border-zinc-300 bg-white/80 px-2.5 py-0.5 text-xs text-zinc-800 outline-none focus:border-emerald-500"
        />
        <button
          v-if="newTag.trim()"
          type="submit"
          class="rounded-full bg-emerald-600 p-1 text-white hover:bg-emerald-700"
        >
          <Plus class="size-3" />
        </button>
      </form>
    </div>
  </NodeViewWrapper>
</template>
