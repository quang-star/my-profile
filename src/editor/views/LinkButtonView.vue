<script setup lang="ts">
import { computed } from 'vue'
import { ExternalLink, Settings2, Trash2 } from '@lucide/vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { deleteBlockNode, requestOpenBlockSettings } from '@/editor/editorState'
import type { LinkButtonBlockAttrs } from '@/editor/extensions/LinkButtonBlock'

const props = defineProps(nodeViewProps)
const editable = computed(() => props.editor.isEditable)
const attrs = computed(() => props.node.attrs as LinkButtonBlockAttrs)

const alignClass = computed(() => {
  if (attrs.value.align === 'center') return 'justify-center'
  if (attrs.value.align === 'right') return 'justify-end'
  return 'justify-start'
})

function safeHref(raw: string): string {
  const val = raw?.trim() || '#'
  return /^(https?:\/\/|mailto:|tel:|#)/i.test(val) ? val : '#'
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
    class="group/links relative rounded-lg"
    :class="{ 'ring-2 ring-emerald-500 ring-offset-2': editable && selected }"
    :style="{ marginBottom: `${Number(attrs.marginBottom ?? 10)}px` }"
    contenteditable="false"
  >
    <div
      v-if="editable"
      class="no-print absolute -top-3 right-1 z-10 flex items-center gap-0.5 rounded-md border border-zinc-200 bg-white px-1 py-0.5 text-[10px] opacity-0 shadow-xs transition-opacity group-hover/links:opacity-100"
      :class="{ '!opacity-100': selected }"
    >
      <button type="button" class="rounded px-1.5 py-0.5 font-semibold text-emerald-700 hover:bg-emerald-50" @click.stop="openSettings">
        <Settings2 class="inline size-3" /> Sửa link
      </button>
      <button type="button" class="rounded p-0.5 text-rose-600 hover:bg-rose-50" title="Xóa" @click.stop="deleteBlockNode(editor, getPos)">
        <Trash2 class="size-3" />
      </button>
    </div>

    <div class="flex flex-wrap items-center gap-2" :class="alignClass">
      <a
        v-for="(item, idx) in (attrs.items || [])"
        :key="`${item.label}-${idx}`"
        :href="safeHref(item.href)"
        :target="item.href?.startsWith('http') ? '_blank' : undefined"
        :rel="item.href?.startsWith('http') ? 'noopener noreferrer' : undefined"
        class="inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-semibold transition-opacity hover:opacity-90"
        :style="
          item.variant === 'outline'
            ? {
                backgroundColor: 'transparent',
                color: 'inherit',
                borderColor: 'currentColor',
              }
            : {
                backgroundColor: 'var(--cv-primary, #1d4ed8)',
                color: '#ffffff',
                borderColor: 'transparent',
              }
        "
        @click="editable && ($event.preventDefault(), openSettings())"
      >
        <span>{{ item.label }}</span>
        <ExternalLink class="size-3 opacity-80" />
      </a>
    </div>
  </NodeViewWrapper>
</template>
