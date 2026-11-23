import { computed, ref } from 'vue'
import { DEFAULT_NODE_TREE } from '@/data/defaultNodes'
import { nodeApi } from '@/services/nodeApi'
import type { BlockChanges, BlockState, ContentNode, NodeAttrs, NodeReorderItem, SaveNodePayload } from '@/types/node'

const nodesTree = ref<ContentNode[]>(structuredClone(DEFAULT_NODE_TREE))
const nodesLoading = ref(false)
const nodesLoaded = ref(false)
const nodesError = ref<string | null>(null)
const tagEditorOpen = ref(false)
const selectedNodeId = ref<string | null>(null)
const canvasNodeId = ref<string | null>(null)
const draggedNodeId = ref<string | null>(null)
const dragPointer = ref<{ x: number; y: number } | null>(null)
const dropTarget = ref<{ id: string; placement: NodePlacement; edge: string } | null>(null)
const autoEditNodeId = ref<string | null>(null)
const resizingNodeId = ref<string | null>(null)
const activeTextId = ref<string | null>(null)
const visualSaving = ref(false)
const visualError = ref('')
const visualSaved = ref(false)
const undoStack = ref<BlockChanges[]>([])
const redoStack = ref<BlockChanges[]>([])
const workspacePanel = ref<'layers' | 'inspector' | null>(null)
const inspectorDraft = ref<{ id: string; attrs: NodeAttrs } | null>(null)
let historyIds: Set<string> | null = null

type NodePlacement = 'before' | 'after' | 'inside'

function flattenTree(tree: ContentNode[]): ContentNode[] {
  const result: ContentNode[] = []
  function walk(list: ContentNode[]) {
    for (const item of list) {
      result.push(item)
      if (item.children?.length) {
        walk(item.children)
      }
    }
  }
  walk(tree)
  return result
}

function findNodeById(tree: ContentNode[], id: string | null): ContentNode | null {
  if (!id) return null
  for (const node of tree) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findNodeById(node.children, id)
      if (found) return found
    }
  }
  return null
}

function findSiblings(tree: ContentNode[], parentId: string | null): ContentNode[] {
  if (!parentId) return tree
  const parent = findNodeById(tree, parentId)
  return parent ? parent.children : []
}

const flatNodes = computed(() => flattenTree(nodesTree.value))
const nodesById = computed(() => new Map(flatNodes.value.map((node) => [node.id, node])))

function blockState({ children: _children, ...node }: ContentNode): BlockState {
  return JSON.parse(JSON.stringify(node)) as BlockState
}

function trackHistory(ids: Iterable<string>) {
  for (const id of ids) historyIds?.add(id)
}

function clearHistory() {
  undoStack.value = []
  redoStack.value = []
  visualSaved.value = false
}

export function useNodes() {
  const selectedNode = computed(() => findNodeById(nodesTree.value, selectedNodeId.value))

  async function initNodes() {
    if (nodesLoaded.value || nodesLoading.value) return
    await reloadNodes()
  }

  async function reloadNodes() {
    nodesLoading.value = true
    nodesError.value = null
    try {
      const tree = await nodeApi.getTree()
      if (Array.isArray(tree)) {
        nodesTree.value = tree
        if (!selectedNodeId.value || !findNodeById(tree, selectedNodeId.value)) {
          selectedNodeId.value = tree[0]?.id ?? null
        }
      }
      nodesLoaded.value = true
    } catch (err) {
      nodesError.value = err instanceof Error ? err.message : 'Failed to load nodes'
      if (!selectedNodeId.value && nodesTree.value.length > 0) {
        selectedNodeId.value = nodesTree.value[0].id
      }
    } finally {
      nodesLoading.value = false
    }
  }

  function openEditorFor(nodeId?: string | null) {
    if (nodeId) {
      selectedNodeId.value = nodeId
    } else if (!selectedNodeId.value && nodesTree.value.length > 0) {
      selectedNodeId.value = nodesTree.value[0].id
    }
    tagEditorOpen.value = true
  }

  async function createNode(payload: SaveNodePayload): Promise<ContentNode> {
    const created = await nodeApi.createNode(payload)
    await reloadNodes()
    selectedNodeId.value = created.id
    clearHistory()
    return created
  }

  async function updateNode(id: string, payload: SaveNodePayload): Promise<ContentNode> {
    const updated = await nodeApi.updateNode(id, payload)
    await reloadNodes()
    selectedNodeId.value = updated.id
    clearHistory()
    return updated
  }

  async function saveNodeText(id: string, changes: Partial<Pick<ContentNode, 'contentVi' | 'contentEn'>>) {
    await saveCanvasNode(id, changes)
  }

  async function saveNodeAttrs(id: string, attrs: NodeAttrs) {
    await saveCanvasNode(id, { attrs })
  }

  async function saveCanvasNode(id: string, changes: Partial<Pick<ContentNode, 'contentVi' | 'contentEn' | 'attrs'>>) {
    const node = findNodeById(nodesTree.value, id)
    if (!node) throw new Error('Content block no longer exists')
    const { parentId, tag, sortOrder, attrs, contentVi, contentEn } = node
    const updated = await nodeApi.updateNode(id, {
      parentId, tag, sortOrder, attrs, contentVi, contentEn, ...changes,
    })
    trackHistory([id])
    node.contentVi = updated.contentVi
    node.contentEn = updated.contentEn
    node.attrs = updated.attrs
  }

  async function insertCanvasNode(payload: SaveNodePayload, editText: boolean) {
    let created: ContentNode | undefined
    const saved = await runVisualChange(async () => {
      const siblings = findSiblings(nodesTree.value, payload.parentId)
      const before = payload.beforeId ? siblings.findIndex((node) => node.id === payload.beforeId) : siblings.length
      if (before < 0) throw new Error('Insert position is no longer available')
      created = await nodeApi.createNode(payload)
      siblings.splice(before, 0, created)
      trackHistory(payload.beforeId ? siblings.map((node) => node.id) : [created.id])
      if (payload.beforeId) siblings.forEach((node, index) => { node.sortOrder = index })
    })
    if (!saved || !created) return
    canvasNodeId.value = created.id
    if (editText) autoEditNodeId.value = created.id
  }

  async function deleteNode(id: string): Promise<void> {
    const node = nodesById.value.get(id)
    await nodeApi.deleteNode(id)
    if (!node) return
    trackHistory(flattenTree([node]).map((item) => item.id))

    const siblings = findSiblings(nodesTree.value, node.parentId)
    const index = siblings.findIndex((item) => item.id === id)
    if (index >= 0) siblings.splice(index, 1)
    const nextSelection = siblings[index]?.id ?? siblings[index - 1]?.id ?? node.parentId
    if (canvasNodeId.value === id || findNodeById(node.children, canvasNodeId.value)) {
      canvasNodeId.value = nextSelection
    }
    if (selectedNodeId.value === id || findNodeById(node.children, selectedNodeId.value)) {
      selectedNodeId.value = nextSelection ?? nodesTree.value[0]?.id ?? null
    }
    if (!visualSaving.value) clearHistory()
  }

  async function moveNodeOrder(node: ContentNode, direction: -1 | 1): Promise<void> {
    const siblings = findSiblings(nodesTree.value, node.parentId)
    const idx = siblings.findIndex((s) => s.id === node.id)
    const target = siblings[idx + direction]
    if (idx < 0 || !target) return
    await moveNode(node.id, target.id, direction === -1 ? 'before' : 'after')
  }

  function canMoveNode(sourceId: string, targetId: string, placement: NodePlacement, fromOutline = false) {
    const source = nodesById.value.get(sourceId)
    const target = nodesById.value.get(targetId)
    if (!source || !target || sourceId === targetId || findNodeById(source.children, targetId)) return false
    const parentId = placement === 'inside' ? target.id : target.parentId
    if (source.parentId === parentId) return placement !== 'inside'
    const currentParent = findNodeById(nodesTree.value, source.parentId)
    const newParent = findNodeById(nodesTree.value, parentId)
    if (!newParent) return fromOutline && parentId === null
    if (!fromOutline && placement !== 'inside' && currentParent?.tag !== newParent.tag) return false
    if (newParent.tag === 'badge-group') return source.tag === 'badge'
    if (['card', 'grid', 'section'].includes(newParent.tag)) return source.tag !== 'section'
    return currentParent?.tag === newParent.tag
  }

  async function moveNode(sourceId: string, targetId: string, placement: NodePlacement, fromOutline = false) {
    if (!canMoveNode(sourceId, targetId, placement, fromOutline)) return
    const source = nodesById.value.get(sourceId)!
    const target = nodesById.value.get(targetId)!
    const parentId = placement === 'inside' ? target.id : target.parentId
    const destination = findSiblings(nodesTree.value, parentId)
    const reordered = destination.filter((node) => node.id !== sourceId)
    const targetIndex = reordered.findIndex((node) => node.id === targetId)
    const index = placement === 'inside' ? reordered.length : targetIndex + Number(placement === 'after')
    reordered.splice(index, 0, source)

    const gridPosition = !!source.attrs.colStart || Object.values(source.attrs.responsive ?? {}).some((attrs) => !!attrs?.colStart)
    if (!gridPosition && reordered.every((node, i) => node.id === destination[i]?.id)) return
    const items: NodeReorderItem[] = reordered.map((node, sortOrder) => ({
      id: node.id, parentId, sortOrder,
      clearGridPosition: node.id === sourceId && gridPosition,
    }))
    nodesTree.value = await nodeApi.reorderNodes(items)
    trackHistory(items.map((item) => item.id))
    if (!visualSaving.value) clearHistory()
  }

  async function duplicateCanvasNode(id: string) {
    const source = nodesById.value.get(id)
    if (!source) return
    const siblings = findSiblings(nodesTree.value, source.parentId)
    const index = siblings.findIndex((node) => node.id === id) + 1
    const expected: BlockState[] = []
    const replacement: BlockState[] = []
    const copies = new Map<string, string>()
    const descendants = flattenTree([source])
    for (const node of descendants) {
      const random = crypto.getRandomValues(new Uint32Array(3))
      copies.set(node.id, `node-${Array.from(random, (value) => value.toString(36)).join('-')}`)
    }
    const rootId = copies.get(id)!
    for (const node of descendants) {
      const copy = blockState(node)
      copy.id = copies.get(node.id)!
      if (node.id === id) {
        copy.sortOrder = index
        delete copy.attrs.colStart
        for (const variant of Object.values(copy.attrs.responsive ?? {})) {
          if (variant) delete variant.colStart
        }
      } else {
        copy.parentId = copies.get(node.parentId!)!
      }
      replacement.push(copy)
    }
    siblings.forEach((node, position) => {
      const order = position < index ? position : position + 1
      if (order === node.sortOrder) return
      const state = blockState(node)
      expected.push(state)
      replacement.push({ ...state, sortOrder: order })
    })
    if (await runVisualChange(async () => {
      nodesTree.value = await nodeApi.applyChanges({ expected, replacement })
      trackHistory(replacement.map((node) => node.id))
    })) canvasNodeId.value = rootId
  }

  async function runVisualChange(change: () => Promise<void>, remember = true) {
    if (visualSaving.value) return false
    const before = remember ? new Map(flatNodes.value.map((node) => [node.id, blockState(node)])) : null
    historyIds = remember ? new Set() : null
    visualSaving.value = true
    visualError.value = ''
    try {
      await change()
      if (before && historyIds?.size) {
        const expected: BlockState[] = []
        const replacement: BlockState[] = []
        for (const id of historyIds) {
          const old = before.get(id)
          const current = nodesById.value.get(id)
          const next = current ? blockState(current) : undefined
          if (JSON.stringify(old) === JSON.stringify(next)) continue
          if (old) expected.push(old)
          if (next) replacement.push(next)
        }
        if (expected.length || replacement.length) {
          undoStack.value = [...undoStack.value.slice(-29), { expected, replacement }]
          redoStack.value = []
          visualSaved.value = true
        }
      }
      return true
    } catch (err) {
      visualError.value = err instanceof Error ? err.message : String(err)
      return false
    } finally {
      visualSaving.value = false
      historyIds = null
    }
  }

  async function stepHistory(direction: 'undo' | 'redo') {
    const from = direction === 'undo' ? undoStack : redoStack
    const to = direction === 'undo' ? redoStack : undoStack
    const entry = from.value.at(-1)
    if (!entry || activeTextId.value || resizingNodeId.value || draggedNodeId.value || inspectorDraft.value) return
    const request = direction === 'undo'
      ? { expected: entry.replacement, replacement: entry.expected }
      : entry
    if (await runVisualChange(async () => {
      nodesTree.value = await nodeApi.applyChanges(request)
      if (canvasNodeId.value && !nodesById.value.has(canvasNodeId.value)) canvasNodeId.value = null
    }, false)) {
      from.value.pop()
      to.value.push(entry)
      visualSaved.value = true
    }
  }

  async function resetDefaultTree(): Promise<void> {
    nodesTree.value = await nodeApi.resetDefaultTree()
    selectedNodeId.value = nodesTree.value[0]?.id ?? null
    clearHistory()
  }

  return {
    nodesTree,
    flatNodes,
    nodesById,
    selectedNode,
    selectedNodeId,
    tagEditorOpen,
    nodesLoading,
    nodesError,
    initNodes,
    reloadNodes,
    openEditorFor,
    createNode,
    updateNode,
    deleteNode,
    moveNodeOrder,
    moveNode,
    canMoveNode,
    saveNodeText,
    saveNodeAttrs,
    insertCanvasNode,
    duplicateCanvasNode,
    undoStack,
    redoStack,
    stepHistory,
    clearHistory,
    visualSaved,
    workspacePanel,
    inspectorDraft,
    canvasNodeId,
    draggedNodeId,
    dragPointer,
    dropTarget,
    autoEditNodeId,
    resizingNodeId,
    activeTextId,
    visualSaving,
    visualError,
    runVisualChange,
    resetDefaultTree,
  }
}
