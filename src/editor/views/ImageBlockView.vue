<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { AlignCenter, AlignLeft, AlignRight, GripVertical, Trash2 } from '@lucide/vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { deleteBlockNode } from '@/editor/editorState'

const props = defineProps(nodeViewProps)

const editable = computed(() => props.editor.isEditable)
const align = computed<'left' | 'center' | 'right'>(() => {
  const raw = props.node.attrs.align
  return raw === 'center' || raw === 'right' ? raw : 'left'
})

const widthStyle = computed(() => {
  const raw = props.node.attrs.width
  return typeof raw === 'string' && raw.trim() ? raw.trim() : 'auto'
})

const wrapperJustifyClass = computed(() => {
  if (align.value === 'center') return 'justify-center'
  if (align.value === 'right') return 'justify-end'
  return 'justify-start'
})

const boxRef = ref<HTMLElement | null>(null)
const resizing = ref(false)
const liveWidthLabel = ref('')

let startX = 0
let startWidthPx = 0
let maxWidthPx = 700

function onResizeMove(event: PointerEvent) {
  const dx = event.clientX - startX
  const nextPx = Math.max(60, Math.min(maxWidthPx, Math.round(startWidthPx + dx)))
  liveWidthLabel.value = `${nextPx}px`
  props.updateAttributes({ width: `${nextPx}px` })
}

function stopResize() {
  resizing.value = false
  liveWidthLabel.value = ''
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', stopResize)
}

function startResize(event: PointerEvent) {
  if (!editable.value || !boxRef.value) return
  event.preventDefault()
  event.stopPropagation()

  const parentEl = boxRef.value.parentElement
  maxWidthPx = parentEl ? Math.max(240, Math.round(parentEl.getBoundingClientRect().width)) : 700
  startWidthPx = Math.round(boxRef.value.getBoundingClientRect().width)
  startX = event.clientX
  resizing.value = true
  liveWidthLabel.value = `${startWidthPx}px`

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
    class="cv-image-wrapper flex w-full py-1"
    :class="wrapperJustifyClass"
  >
    <div
      ref="boxRef"
      class="group/img relative inline-block max-w-full"
      :class="{ 'is-selected-img': editable && selected }"
      :style="{ width: widthStyle, maxWidth: '100%' }"
    >
      <!-- Hover Toolbar -->
      <div
        v-if="editable"
        class="no-print pointer-events-none absolute -top-3.5 right-1 z-20 flex items-center gap-0.5 rounded-lg border border-zinc-200 bg-white px-1 py-0.5 text-[11px] font-semibold text-zinc-600 opacity-0 shadow-sm transition-opacity group-hover/img:pointer-events-auto group-hover/img:opacity-100"
        :class="{ '!pointer-events-auto !opacity-100 border-emerald-400': selected }"
        contenteditable="false"
      >
        <button
          type="button"
          data-drag-handle
          class="cursor-grab rounded p-0.5 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 active:cursor-grabbing"
          title="Kéo thả ảnh"
        >
          <GripVertical class="size-3" />
        </button>
        <button
          type="button"
          class="rounded p-0.5 hover:bg-zinc-100"
          :class="{ 'bg-emerald-50 text-emerald-700': align === 'left' }"
          title="Căn trái"
          @click.stop="updateAttributes({ align: 'left' })"
        >
          <AlignLeft class="size-3" />
        </button>
        <button
          type="button"
          class="rounded p-0.5 hover:bg-zinc-100"
          :class="{ 'bg-emerald-50 text-emerald-700': align === 'center' }"
          title="Căn giữa"
          @click.stop="updateAttributes({ align: 'center' })"
        >
          <AlignCenter class="size-3" />
        </button>
        <button
          type="button"
          class="rounded p-0.5 hover:bg-zinc-100"
          :class="{ 'bg-emerald-50 text-emerald-700': align === 'right' }"
          title="Căn phải"
          @click.stop="updateAttributes({ align: 'right' })"
        >
          <AlignRight class="size-3" />
        </button>
        <button
          type="button"
          class="rounded p-0.5 text-rose-600 hover:bg-rose-50"
          title="Xóa ảnh"
          @click.stop="deleteBlockNode(editor, getPos)"
        >
          <Trash2 class="size-3" />
        </button>
      </div>

      <img
        :src="node.attrs.src"
        :alt="node.attrs.alt || ''"
        :title="node.attrs.title || ''"
        class="cv-doc-image block h-auto w-full rounded-xl"
      />

      <!-- Right-edge Resize Handle -->
      <div
        v-if="editable"
        class="no-print absolute top-2 -right-1.5 bottom-2 z-20 flex w-3 cursor-ew-resize items-center justify-center opacity-0 transition-opacity group-hover/img:opacity-100"
        :class="{ '!opacity-100': resizing || selected }"
        contenteditable="false"
        title="Kéo để chỉnh kích thước ảnh"
        @pointerdown="startResize"
      >
        <span class="h-8 w-1 rounded-full bg-emerald-500 shadow" />
      </div>

      <div
        v-if="resizing && liveWidthLabel"
        class="no-print pointer-events-none absolute right-2 bottom-2 z-30 rounded bg-zinc-900/85 px-1.5 py-0.5 font-mono text-[10px] font-bold text-white"
        contenteditable="false"
      >
        {{ liveWidthLabel }}
      </div>
    </div>
  </NodeViewWrapper>
</template>

<style scoped>
.is-selected-img > img {
  outline: 2px solid #059669;
  outline-offset: 2px;
}
</style>
