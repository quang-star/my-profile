<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { GripVertical, Layers, Monitor, Redo2, Settings2, Smartphone, Tablet, Undo2 } from '@lucide/vue'
import NodeInspector from './NodeInspector.vue'
import NodeOutline from './NodeOutline.vue'
import { canvasDevices, useCanvasLayout } from '@/composables/useCanvasLayout'
import { useI18n } from '@/composables/useI18n'
import { useNodes } from '@/composables/useNodes'
import { usePortfolio } from '@/composables/usePortfolio'

const { isVi } = useI18n()
const { editMode } = usePortfolio()
const { editorActive, devicePreview, viewportWidth, canvasWidth, deviceAttrs } = useCanvasLayout()
const {
  workspacePanel, inspectorDraft, visualSaving, activeTextId, draggedNodeId, dragPointer, resizingNodeId,
  visualSaved, visualError, undoStack, redoStack, stepHistory, tagEditorOpen, dropTarget, nodesById, nodesTree,
} = useNodes()
const deviceIcons = { desktop: Monitor, tablet: Tablet, phone: Smartphone }
const busy = computed(() => !!inspectorDraft.value || visualSaving.value || !!activeTextId.value ||
  !!draggedNodeId.value || !!resizingNodeId.value || tagEditorOpen.value)
const dropPreview = computed(() => {
  const source = draggedNodeId.value ? nodesById.value.get(draggedNodeId.value) : undefined
  const drop = dropTarget.value
  const target = drop ? nodesById.value.get(drop.id) : undefined
  if (!source || !target || !drop) return null
  const parent = drop.placement === 'inside' ? target : target.parentId ? nodesById.value.get(target.parentId) : undefined
  const columns = parent?.tag === 'grid' ? Math.max(1, Math.min(12, deviceAttrs(parent.attrs, true).cols ?? 1)) : 1
  const siblings = (parent?.children ?? nodesTree.value).filter((node) => node.id !== source.id)
  const index = drop.placement === 'inside' ? siblings.length
    : siblings.findIndex((node) => node.id === target.id) + Number(drop.placement === 'after')
  siblings.splice(index, 0, source)
  const placement = isVi.value
    ? { before: 'Trước', after: 'Sau', inside: 'Trong' }[drop.placement]
    : { before: 'Before', after: 'After', inside: 'Inside' }[drop.placement]
  return {
    columns,
    label: `${placement} ${target.attrs.canvasLabel || target.attrs.anchor || target.id}`,
    blocks: siblings.map((node) => {
      const attrs = deviceAttrs(node.attrs, false)
      const span = Math.max(1, Math.min(columns, Number(attrs.colSpan) || 1))
      const moving = node.id === source.id
      const start = moving || !attrs.colStart ? 'auto' : Math.max(1, Math.min(columns - span + 1, attrs.colStart))
      return { id: node.id, label: attrs.canvasLabel || attrs.anchor || node.id, moving, gridColumn: `${start} / span ${span}` }
    }),
  }
})
const dragGhost = computed(() => {
  if (!draggedNodeId.value || !dragPointer.value) return null
  const source = nodesById.value.get(draggedNodeId.value)
  if (!source) return null
  return {
    x: dragPointer.value.x + 14,
    y: dragPointer.value.y + 14,
    name: source.attrs.canvasLabel || source.attrs.anchor || source.tag,
    targetLabel: dropPreview.value?.label ?? null,
  }
})

function handleKey(event: KeyboardEvent) {
  if (!editMode.value || busy.value || !(event.ctrlKey || event.metaKey) || event.altKey ||
    (event.target instanceof Element && event.target.closest('input,textarea,select,[contenteditable="true"]'))) return
  const key = event.key.toLowerCase()
  if (key !== 'z' && key !== 'y') return
  const redo = key === 'y' || event.shiftKey
  if (!(redo ? redoStack.value : undoStack.value).length) return
  event.preventDefault()
  void stepHistory(redo ? 'redo' : 'undo')
}

function onResize() { viewportWidth.value = window.innerWidth }

watch(editMode, (enabled) => {
  if (enabled) {
    devicePreview.value = viewportWidth.value < 768 ? 'phone'
      : viewportWidth.value < 1024 ? 'tablet' : 'desktop'
  } else {
    inspectorDraft.value = null
    workspacePanel.value = null
  }
  editorActive.value = enabled
}, { immediate: true, flush: 'sync' })

onMounted(() => {
  onResize()
  window.addEventListener('resize', onResize)
  window.addEventListener('keydown', handleKey)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', handleKey)
})
</script>

<template>
  <div v-if="editMode" class="workspace-toolbar no-print" data-editor-control>
    <div class="flex items-center gap-1 xl:hidden">
      <button type="button" class="workspace-button" :aria-pressed="workspacePanel === 'layers'" @click="workspacePanel = workspacePanel === 'layers' ? null : 'layers'">
        <Layers class="size-4" /> {{ isVi ? 'Cây khối' : 'Layers' }}
      </button>
      <button type="button" class="workspace-button" :aria-pressed="workspacePanel === 'inspector'" @click="workspacePanel = workspacePanel === 'inspector' ? null : 'inspector'">
        <Settings2 class="size-4" /> {{ isVi ? 'Thiết lập' : 'Settings' }}
      </button>
    </div>
    <div class="flex items-center gap-1" role="group" :aria-label="isVi ? 'Màn hình xem trước' : 'Device preview'">
      <button
        v-for="device in canvasDevices"
        :key="device.id"
        type="button"
        class="workspace-button"
        :title="isVi ? device.vi : device.en"
        :aria-label="isVi ? device.vi : device.en"
        :aria-pressed="devicePreview === device.id"
        :disabled="busy"
        @click="devicePreview = device.id"
      >
        <component :is="deviceIcons[device.id]" class="size-4" />
        <span class="hidden sm:inline">{{ isVi ? device.vi : device.en }}</span>
      </button>
    </div>
    <div class="flex items-center gap-1" role="group" :aria-label="isVi ? 'Lịch sử chỉnh sửa' : 'Edit history'">
      <button type="button" class="workspace-button" :disabled="busy || !undoStack.length" :aria-label="isVi ? 'Hoàn tác' : 'Undo'" :title="isVi ? 'Hoàn tác (Ctrl+Z)' : 'Undo (Ctrl+Z)'" @click="stepHistory('undo')">
        <Undo2 class="size-4" />
      </button>
      <button type="button" class="workspace-button" :disabled="busy || !redoStack.length" :aria-label="isVi ? 'Làm lại' : 'Redo'" :title="isVi ? 'Làm lại (Ctrl+Shift+Z)' : 'Redo (Ctrl+Shift+Z)'" @click="stepHistory('redo')">
        <Redo2 class="size-4" />
      </button>
    </div>
    <p role="status" class="min-w-0 text-xs text-slate-600">
      <template v-if="dropPreview">{{ dropPreview.label }}</template>
      <template v-else-if="visualSaving">{{ isVi ? 'Đang lưu…' : 'Saving…' }}</template>
      <template v-else-if="inspectorDraft">{{ isVi ? 'Thiết lập chưa lưu' : 'Unsaved settings' }}</template>
      <template v-else-if="visualSaved && !visualError">{{ isVi ? 'Đã lưu' : 'Saved' }}</template>
    </p>
    <div v-if="dropPreview && dropPreview.columns > 1" class="drop-preview" data-editor-control aria-hidden="true">
      <div class="grid gap-1" :style="{ gridTemplateColumns: `repeat(${dropPreview.columns}, minmax(0, 1fr))` }">
        <span
          v-for="block in dropPreview.blocks"
          :key="block.id"
          class="truncate rounded border border-slate-200 bg-white px-1 py-0.5 text-[10px] text-slate-700"
          :class="{ 'border-emerald-400 bg-emerald-50 font-semibold text-emerald-800': block.moving }"
          :style="{ gridColumn: block.gridColumn }"
        >{{ block.label }}</span>
      </div>
    </div>
  </div>
  <div :class="[editMode ? 'editor-workspace' : undefined, draggedNodeId ? 'is-dragging' : undefined]">
    <aside v-if="editMode" class="workspace-panel workspace-layers no-print" :class="{ 'workspace-open': workspacePanel === 'layers' }" data-editor-control>
      <NodeOutline />
    </aside>
    <div :class="editMode ? 'workspace-canvas' : undefined" :style="editMode ? { '--canvas-width': canvasWidth + 'px' } : undefined">
      <slot />
    </div>
    <aside v-if="editMode" class="workspace-panel workspace-inspector no-print" :class="{ 'workspace-open': workspacePanel === 'inspector' }" data-editor-control>
      <NodeInspector />
    </aside>
  </div>
  <div
    v-if="dragGhost"
    class="drag-ghost no-print"
    :style="{ left: dragGhost.x + 'px', top: dragGhost.y + 'px' }"
    aria-hidden="true"
  >
    <span class="flex items-center gap-1.5 font-semibold text-slate-900">
      <GripVertical class="size-3.5 text-emerald-600" />
      <span class="max-w-36 truncate">{{ dragGhost.name }}</span>
    </span>
    <span v-if="dragGhost.targetLabel" class="rounded bg-emerald-50 px-1.5 py-0.5 text-[11px] font-semibold text-emerald-700">
      → {{ dragGhost.targetLabel }}
    </span>
    <span v-else class="text-[11px] text-slate-500">
      {{ isVi ? 'Kéo đến vị trí muốn thả' : 'Drag to a drop target' }}
    </span>
  </div>
</template>

<style scoped>
.workspace-toolbar {
  position: sticky;
  top: 4rem;
  z-index: 25;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.35rem 0.75rem;
  padding: 0.4rem 0.75rem;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}
.workspace-button {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.35rem 0.5rem;
  border-radius: 0.45rem;
  color: #334155;
  font-size: 0.75rem;
  font-weight: 600;
}
.workspace-button:hover { background: #f1f5f9; }
.workspace-button[aria-pressed='true'] { background: #ecfdf5; color: #047857; }
.workspace-button:disabled { opacity: 0.38; }
.drop-preview {
  position: absolute;
  inset-block-start: 100%;
  z-index: 1;
  width: min(22rem, 88vw);
  max-height: 7rem;
  overflow: hidden;
  padding: 0.35rem;
  border: 1px solid #a7f3d0;
  border-radius: 0 0 0.6rem 0.6rem;
  background: #f8fafc;
  pointer-events: none;
}
.editor-workspace { display: grid; grid-template-columns: minmax(0, 1fr); }
.editor-workspace.is-dragging,
.editor-workspace.is-dragging * { cursor: grabbing !important; }
.drag-ghost {
  position: fixed;
  z-index: 60;
  pointer-events: none;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  max-width: min(22rem, 85vw);
  padding: 0.35rem 0.6rem;
  border: 1px solid #059669;
  border-radius: 0.6rem;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.16);
  font-size: 0.75rem;
  backdrop-filter: blur(6px);
}
.workspace-canvas { width: min(100%, var(--canvas-width)); min-width: 0; margin-inline: auto; }
.workspace-panel {
  display: none;
  position: fixed;
  z-index: 30;
  inset-block: 9rem 5.5rem;
  width: min(18rem, calc(100% - 1.5rem));
  padding: 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 0.8rem;
  background: #fff;
  box-shadow: 0 12px 25px #0f172a20;
}
.workspace-panel.workspace-open { display: flex; flex-direction: column; }
.workspace-layers { left: 0.5rem; }
.workspace-inspector { right: 0.5rem; overflow-y: auto; }

@media (min-width: 80rem) {
  .editor-workspace { grid-template-columns: minmax(13rem, 15rem) minmax(0, 1fr) minmax(14rem, 17rem); }
  .workspace-panel {
    display: flex;
    flex-direction: column;
    position: sticky;
    top: 7.5rem;
    inset-inline: auto;
    bottom: auto;
    align-self: start;
    width: auto;
    max-height: calc(100dvh - 12rem);
    margin: 0.75rem;
    box-shadow: none;
  }
}
</style>
