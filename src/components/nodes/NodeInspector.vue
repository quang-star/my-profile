<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Copy, Pencil, Settings2 } from '@lucide/vue'
import NodeCanvasSettings from './NodeCanvasSettings.vue'
import NodeInsertMenu from './NodeInsertMenu.vue'
import { canvasDevices, useCanvasLayout } from '@/composables/useCanvasLayout'
import { useI18n } from '@/composables/useI18n'
import { useNodes } from '@/composables/useNodes'
import type { NodeAttrs } from '@/types/node'

const { isVi } = useI18n()
const { canvasDevice, deviceAttrs, saveDeviceAttrs } = useCanvasLayout()
const {
  nodesById, flatNodes, canvasNodeId, inspectorDraft, activeTextId, autoEditNodeId, draggedNodeId, resizingNodeId,
  visualSaving, runVisualChange, saveNodeAttrs, duplicateCanvasNode, openEditorFor, workspacePanel, canMoveNode, moveNode,
} = useNodes()
const selected = computed(() => canvasNodeId.value ? nodesById.value.get(canvasNodeId.value) : undefined)
const parent = computed(() => selected.value?.parentId ? nodesById.value.get(selected.value.parentId) : undefined)
const locked = computed(() => visualSaving.value || !!activeTextId.value || !!resizingNodeId.value || !!draggedNodeId.value)
const draft = ref<NodeAttrs>({})
const baseline = ref('')
const destination = ref('')
const variant = computed(() => canvasDevice.value !== 'desktop')
const deviceName = computed(() => {
  const item = canvasDevices.find(({ id }) => id === canvasDevice.value)!
  return isVi.value ? item.vi : item.en
})
const hasOverride = computed(() => canvasDevice.value !== 'desktop' && !!selected.value?.attrs.responsive?.[canvasDevice.value])
const canEditText = computed(() => ['heading', 'text', 'list', 'badge', 'metric', 'link-button'].includes(selected.value?.tag ?? ''))
const canInsert = computed(() => ['section', 'grid', 'card', 'badge-group'].includes(selected.value?.tag ?? ''))
const destinations = computed(() => selected.value && !inspectorDraft.value
  ? flatNodes.value.filter((node) => canMoveNode(selected.value!.id, node.id, 'inside'))
  : [])

function restoreDraft() {
  const node = selected.value
  const attrs = node ? deviceAttrs(node.attrs, node.tag === 'grid') : {}
  baseline.value = JSON.stringify(attrs)
  draft.value = attrs
  inspectorDraft.value = null
}

watch([selected, canvasDevice, () => selected.value?.attrs], () => {
  restoreDraft()
  destination.value = ''
}, { immediate: true })

watch(draft, (attrs) => {
  const node = selected.value
  inspectorDraft.value = node && JSON.stringify(attrs) !== baseline.value ? { id: node.id, attrs: { ...attrs } } : null
}, { deep: true, flush: 'sync' })

async function save() {
  const node = selected.value
  if (!node || !inspectorDraft.value || locked.value) return
  const attrs = saveDeviceAttrs(node.attrs, draft.value, node.tag === 'grid')
  if (await runVisualChange(() => saveNodeAttrs(node.id, attrs))) restoreDraft()
}

function editText() {
  if (!selected.value) return
  if (window.innerWidth < 1280) workspacePanel.value = null
  autoEditNodeId.value = selected.value.id
}

async function moveToDestination() {
  const node = selected.value
  const target = destination.value
  if (node && target && await runVisualChange(() => moveNode(node.id, target, 'inside'))) destination.value = ''
}
</script>

<template>
  <div class="grid gap-3" :aria-label="isVi ? 'Thiết lập khối' : 'Block inspector'">
    <h2 class="flex items-center gap-2 text-sm font-bold text-slate-900"><Settings2 class="size-4" /> {{ isVi ? 'Thiết lập khối' : 'Block inspector' }}</h2>
    <p v-if="!selected" class="text-xs leading-relaxed text-slate-500">{{ isVi ? 'Chọn một khối trên trang hoặc trong cây để chỉnh.' : 'Select a block on the page or in the layer tree.' }}</p>
    <template v-else>
      <p class="break-all text-xs font-medium text-slate-600">{{ selected.attrs.canvasLabel || selected.id }} · {{ deviceName }}</p>
      <p v-if="variant" class="text-xs leading-relaxed text-slate-500">
        {{ isVi ? 'Thiết lập ở đây áp dụng cho màn hình này. Nội dung và thứ tự dùng chung.' : 'These settings apply to this device. Content and order are shared.' }}
      </p>
      <div class="flex flex-wrap items-center gap-1.5 border-b border-slate-200 pb-3">
        <button v-if="canEditText" type="button" class="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs hover:bg-slate-50 disabled:opacity-40" :disabled="locked || !!inspectorDraft" @click="editText">
          <Pencil class="size-3" /> {{ isVi ? 'Sửa chữ' : 'Edit text' }}
        </button>
        <button type="button" class="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs hover:bg-slate-50 disabled:opacity-40" :disabled="locked || !!inspectorDraft" @click="duplicateCanvasNode(selected.id)">
          <Copy class="size-3" /> {{ isVi ? 'Nhân bản' : 'Duplicate' }}
        </button>
        <NodeInsertMenu v-if="canInsert && !locked && !inspectorDraft" :parent-id="selected.id" :parent-tag="selected.tag" :label="isVi ? 'Thêm vào khối' : 'Add inside'" />
      </div>
      <NodeCanvasSettings
        v-model="draft"
        :tag="selected.tag"
        :grid-columns="parent?.tag === 'grid' ? deviceAttrs(parent.attrs, true).cols : undefined"
        :saving="locked"
        :dirty="!!inspectorDraft"
        :variant="variant"
        :has-override="hasOverride"
        @save="save"
        @cancel="restoreDraft"
        @inherit="draft = deviceAttrs(selected.attrs, selected.tag === 'grid', false)"
        @advanced="openEditorFor(selected.id)"
      />
      <div v-if="destinations.length" class="border-t border-slate-200 pt-3">
        <label class="grid gap-1 text-xs">
          {{ isVi ? 'Chuyển khối vào' : 'Move block into' }}
          <select v-model="destination" :disabled="locked" class="min-w-0 rounded border border-slate-300 p-1.5">
            <option value="">{{ isVi ? 'Chọn khối đích' : 'Select destination' }}</option>
            <option v-for="node in destinations" :key="node.id" :value="node.id">{{ node.attrs.canvasLabel || node.attrs.anchor || node.id }}</option>
          </select>
        </label>
        <button type="button" :disabled="!destination || locked" class="mt-2 rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold disabled:opacity-40" @click="moveToDestination">
          {{ isVi ? 'Di chuyển' : 'Move' }}
        </button>
      </div>
    </template>
  </div>
</template>
