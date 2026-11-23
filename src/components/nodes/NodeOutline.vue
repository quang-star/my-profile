<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChevronDown, ChevronRight, GripVertical, Layers, Search } from '@lucide/vue'
import NodeInsertMenu from './NodeInsertMenu.vue'
import { useI18n } from '@/composables/useI18n'
import { useNodes } from '@/composables/useNodes'
import type { ContentNode } from '@/types/node'

const { isVi } = useI18n()
const {
  nodesTree, nodesById, canvasNodeId, draggedNodeId, dropTarget, inspectorDraft, activeTextId, visualSaving,
  resizingNodeId, workspacePanel, canMoveNode, moveNode, runVisualChange,
} = useNodes()
const locked = computed(() => !!inspectorDraft.value || !!activeTextId.value || visualSaving.value || !!resizingNodeId.value)
const closed = ref(new Set<string>())
const known = new Set<string>()
const query = ref('')
const nav = ref<HTMLElement>()
let outlineDrag = false

function searchText(value: string) {
  return value.toLocaleLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/g, 'd')
}

watch(nodesTree, (tree) => {
  function visit(nodes: ContentNode[], depth: number) {
    for (const node of nodes) {
      if (!known.has(node.id) && depth >= 1) closed.value.add(node.id)
      known.add(node.id)
      visit(node.children, depth + 1)
    }
  }
  visit(tree, 0)
}, { immediate: true })

function label(node: ContentNode) {
  const text = (isVi.value ? node.contentVi || node.contentEn : node.contentEn || node.contentVi)
    .replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim()
  return node.attrs.canvasLabel || node.attrs.anchor || text.slice(0, 48) || node.id
}

const rows = computed(() => {
  const result: { node: ContentNode; depth: number; name: string }[] = []
  const search = searchText(query.value.trim())
  function visit(nodes: ContentNode[], depth: number) {
    for (const node of nodes) {
      const name = label(node)
      if (!search || searchText(`${name} ${node.tag} ${node.id}`).includes(search)) result.push({ node, depth, name })
      if (search || !closed.value.has(node.id)) visit(node.children, depth + 1)
    }
  }
  visit(nodesTree.value, 0)
  return result
})

watch(canvasNodeId, (id) => {
  let node = id ? nodesById.value.get(id) : undefined
  while (node?.parentId) {
    closed.value.delete(node.parentId)
    node = nodesById.value.get(node.parentId)
  }
}, { immediate: true })

function toggle(id: string) {
  if (closed.value.has(id)) closed.value.delete(id)
  else closed.value.add(id)
}

function select(id: string) {
  if (locked.value) return
  canvasNodeId.value = id
  if (window.innerWidth < 1280) workspacePanel.value = null
  requestAnimationFrame(() => {
    const element = document.querySelector<HTMLElement>(`[data-node-id="${CSS.escape(id)}"]`)
    if (element) element.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'instant' })
  })
}

function startDrag(id: string, event: DragEvent) {
  if (locked.value || !event.dataTransfer) {
    event.preventDefault()
    return
  }
  outlineDrag = true
  canvasNodeId.value = id
  draggedNodeId.value = id
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', id)
}

function endDrag() {
  if (!outlineDrag) return
  outlineDrag = false
  draggedNodeId.value = null
  dropTarget.value = null
}

function hover(id: string, event: DragEvent) {
  if (!outlineDrag || !draggedNodeId.value) return
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const fraction = (event.clientY - bounds.top) / Math.max(1, bounds.height)
  const edge = fraction < 0.5 ? 'before' : 'after'
  const placement = fraction > 0.25 && fraction < 0.75 && canMoveNode(draggedNodeId.value, id, 'inside')
    ? 'inside' : edge
  if (!canMoveNode(draggedNodeId.value, id, placement, true)) {
    dropTarget.value = null
    return
  }
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
  dropTarget.value = { id, placement, edge: placement === 'inside' ? 'inside' : edge === 'before' ? 'top' : 'bottom' }
}

async function drop(id: string, event: DragEvent) {
  const source = draggedNodeId.value
  const target = dropTarget.value
  if (!outlineDrag || !source || target?.id !== id) return
  event.preventDefault()
  endDrag()
  if (await runVisualChange(() => moveNode(source, id, target.placement, true))) {
    if (target.placement === 'inside') closed.value.delete(id)
  }
}

function clearDrop(event: DragEvent) {
  if (outlineDrag && (event.relatedTarget === null ||
    (event.relatedTarget instanceof Node && !nav.value?.contains(event.relatedTarget)))) dropTarget.value = null
}
</script>

<template>
  <nav ref="nav" :aria-label="isVi ? 'Cây khối' : 'Layers'" class="flex min-h-0 flex-col gap-3" @dragleave="clearDrop">
    <div class="flex items-center justify-between gap-2">
      <h2 class="flex items-center gap-2 text-sm font-bold text-slate-900"><Layers class="size-4" /> {{ isVi ? 'Cây khối' : 'Layers' }}</h2>
      <NodeInsertMenu v-if="!locked" :parent-id="null" :label="isVi ? 'Thêm mục' : 'Add root block'" />
    </div>
    <label class="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-2 py-1.5">
      <Search class="size-3.5 text-slate-500" />
      <input v-model="query" type="search" :aria-label="isVi ? 'Tìm khối' : 'Search layers'" :placeholder="isVi ? 'Tìm khối…' : 'Search layers…'" class="w-full min-w-0 bg-transparent text-xs outline-none" />
    </label>
    <p v-if="!rows.length" class="text-xs text-slate-500">{{ isVi ? 'Không có khối phù hợp.' : 'No matching blocks.' }}</p>
    <ul v-else class="min-h-0 overflow-y-auto py-1">
      <li
        v-for="{ node, depth, name } in rows"
        :key="node.id"
        class="relative flex min-w-0 items-center gap-0.5 rounded-md border-y border-transparent hover:bg-slate-50"
        :class="[
          canvasNodeId === node.id ? 'bg-emerald-50' : '',
          draggedNodeId === node.id ? 'bg-slate-100 opacity-50 ring-1 ring-dashed ring-slate-400' : '',
          dropTarget?.id === node.id && dropTarget.placement === 'before' ? 'outline-row-before' : '',
          dropTarget?.id === node.id && dropTarget.placement === 'after' ? 'outline-row-after' : '',
          dropTarget?.id === node.id && dropTarget.placement === 'inside' ? 'bg-emerald-50/90 ring-2 ring-inset ring-emerald-500' : '',
        ]"
        :style="{ paddingLeft: Math.min(54, depth * 10) + 'px' }"
        @dragover="hover(node.id, $event)"
        @drop="drop(node.id, $event)"
      >
        <button v-if="node.children.length && !query" type="button" :aria-expanded="!closed.has(node.id)" :aria-label="(isVi ? 'Mở hoặc thu gọn ' : 'Expand or collapse ') + name" class="shrink-0 rounded p-1 text-slate-500 hover:bg-slate-100" @click="toggle(node.id)">
          <ChevronRight v-if="closed.has(node.id)" class="size-3" />
          <ChevronDown v-else class="size-3" />
        </button>
        <span v-else class="w-5 shrink-0" />
        <button type="button" :aria-current="canvasNodeId === node.id ? 'true' : undefined" :data-layer-id="node.id" :title="name" :disabled="locked" class="min-w-0 flex-1 truncate py-1.5 text-left text-xs text-slate-800 disabled:opacity-60" @click="select(node.id)">
          {{ name }} <span class="ml-1 text-[10px] text-slate-500">{{ node.tag }}</span>
        </button>
        <button type="button" :draggable="!locked" :disabled="locked" :aria-label="(isVi ? 'Di chuyển ' : 'Drag ') + name" class="shrink-0 cursor-grab p-1 text-slate-500 disabled:opacity-40" @dragstart="startDrag(node.id, $event)" @dragend="endDrag">
          <GripVertical class="pointer-events-none size-3.5" />
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.outline-row-before::before,
.outline-row-after::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 2.5px;
  border-radius: 9999px;
  background: #059669;
  pointer-events: none;
  z-index: 5;
}
.outline-row-before::before { top: -1px; }
.outline-row-after::after { bottom: -1px; }
</style>
