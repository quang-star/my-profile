import { ref } from 'vue'
import type { Editor } from '@tiptap/core'

export const selectedBlockPos = ref<number | null>(null)
export const inspectorRequestCount = ref(0)

export function requestOpenBlockSettings(pos: number | null) {
  selectedBlockPos.value = pos
  inspectorRequestCount.value++
}

export function moveBlockNode(editor: Editor, getPos: () => number | undefined, direction: -1 | 1): boolean {
  const pos = getPos()
  if (typeof pos !== 'number') return false
  const { state, view } = editor
  const resolved = state.doc.resolve(pos)
  const parent = resolved.parent
  const index = resolved.index()
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= parent.childCount) return false

  const node = parent.child(index)
  const neighbor = parent.child(targetIndex)
  const tr = state.tr

  if (direction === -1) {
    const neighborPos = pos - neighbor.nodeSize
    tr.delete(pos, pos + node.nodeSize)
    tr.insert(neighborPos, node)
  } else {
    const afterNeighborPos = pos + node.nodeSize + neighbor.nodeSize
    tr.insert(afterNeighborPos, node)
    tr.delete(pos, pos + node.nodeSize)
  }

  view.dispatch(tr.scrollIntoView())
  return true
}

export function duplicateBlockNode(editor: Editor, getPos: () => number | undefined): boolean {
  const pos = getPos()
  if (typeof pos !== 'number') return false
  const { state, view } = editor
  const node = state.doc.nodeAt(pos)
  if (!node) return false
  const copy = node.type.create(JSON.parse(JSON.stringify(node.attrs)), node.content, node.marks)
  const tr = state.tr.insert(pos + node.nodeSize, copy)
  view.dispatch(tr.scrollIntoView())
  return true
}

export function deleteBlockNode(editor: Editor, getPos: () => number | undefined): boolean {
  const pos = getPos()
  if (typeof pos !== 'number') return false
  const { state, view } = editor
  const node = state.doc.nodeAt(pos)
  if (!node) return false
  const tr = state.tr.delete(pos, pos + node.nodeSize)
  view.dispatch(tr.scrollIntoView())
  if (selectedBlockPos.value === pos) selectedBlockPos.value = null
  return true
}

export function moveTableRow(editor: Editor, rowPos: number, direction: -1 | 1): boolean {
  const { state, view } = editor
  if (rowPos < 0 || rowPos >= state.doc.content.size) return false
  const resolved = state.doc.resolve(rowPos)
  const table = resolved.parent
  if (table.type.name !== 'table') return false

  const index = resolved.index()
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= table.childCount) return false

  const row = table.child(index)
  const neighbor = table.child(targetIndex)
  const tr = state.tr

  if (direction === -1) {
    const neighborPos = rowPos - neighbor.nodeSize
    tr.delete(rowPos, rowPos + row.nodeSize)
    tr.insert(neighborPos, row)
  } else {
    const afterNeighborPos = rowPos + row.nodeSize + neighbor.nodeSize
    tr.insert(afterNeighborPos, row)
    tr.delete(rowPos, rowPos + row.nodeSize)
  }

  view.dispatch(tr.scrollIntoView())
  return true
}

export function insertTableRowBelow(editor: Editor, rowPos: number): boolean {
  const { state, view } = editor
  if (rowPos < 0 || rowPos >= state.doc.content.size) return false
  const row = state.doc.nodeAt(rowPos)
  if (!row || row.type.name !== 'tableRow') return false

  const emptyCells = []
  for (let i = 0; i < row.childCount; i++) {
    const cell = row.child(i)
    const emptyCell = cell.type.createAndFill(cell.attrs)
    if (emptyCell) emptyCells.push(emptyCell)
  }
  if (!emptyCells.length) return false

  const newRow = row.type.create(row.attrs, emptyCells)
  const insertAt = rowPos + row.nodeSize
  const tr = state.tr.insert(insertAt, newRow)
  view.dispatch(tr.scrollIntoView())
  return true
}

export function deleteTableRowAt(editor: Editor, rowPos: number): boolean {
  const { state, view } = editor
  if (rowPos < 0 || rowPos >= state.doc.content.size) return false
  const resolved = state.doc.resolve(rowPos)
  const table = resolved.parent
  const row = state.doc.nodeAt(rowPos)
  if (!row || row.type.name !== 'tableRow' || table.type.name !== 'table') return false

  if (table.childCount <= 1) {
    const tablePos = resolved.before()
    const tr = state.tr.delete(tablePos, tablePos + table.nodeSize)
    view.dispatch(tr.scrollIntoView())
    return true
  }

  const tr = state.tr.delete(rowPos, rowPos + row.nodeSize)
  view.dispatch(tr.scrollIntoView())
  return true
}

export interface OutlineBlockItem {
  pos: number
  index: number
  type: string
  badge: string
  label: string
}

const OUTLINE_TYPE_BADGES: Record<string, string> = {
  heading: 'H',
  paragraph: 'P',
  boxBlock: 'Box',
  columnsBlock: 'Cột',
  table: 'Bảng',
  bulletList: 'List',
  orderedList: 'List',
  skillBarBlock: 'Skill',
  badgeListBlock: 'Tags',
  linkButtonBlock: 'CTA',
  horizontalRule: 'Line',
  image: 'Ảnh',
}

export function getDocumentOutline(editor: Editor): OutlineBlockItem[] {
  const items: OutlineBlockItem[] = []
  editor.state.doc.forEach((node, offset, index) => {
    const type = node.type.name
    const badge = OUTLINE_TYPE_BADGES[type] || type
    const rawText = node.textContent.replace(/\s+/g, ' ').trim()
    let label = rawText
    if (!label) {
      if (type === 'skillBarBlock') label = String(node.attrs?.label || 'Skill bar')
      else if (type === 'badgeListBlock') label = Array.isArray(node.attrs?.items) ? node.attrs.items.slice(0, 4).join(', ') : 'Tags'
      else if (type === 'linkButtonBlock') label = Array.isArray(node.attrs?.items) ? node.attrs.items.map((i: { label?: string }) => i.label).filter(Boolean).join(', ') : 'Links'
      else if (type === 'horizontalRule') label = 'Đường kẻ ngang'
      else if (type === 'image') label = String(node.attrs?.alt || 'Hình ảnh')
      else label = `(${badge})`
    }
    if (label.length > 44) label = `${label.slice(0, 41)}…`
    items.push({ pos: offset, index, type, badge, label })
  })
  return items
}

export function wrapSelectionInBox(editor: Editor): boolean {
  const { state, view } = editor
  const boxType = state.schema.nodes.boxBlock
  if (!boxType) return false

  let range = state.selection.$from.blockRange(state.selection.$to)
  if (!range) return false

  const containerParents = new Set(['doc', 'boxBlock', 'columnCell'])
  if (!containerParents.has(range.parent.type.name)) {
    const $from = state.selection.$from
    const $to = state.selection.$to
    for (let d = range.depth - 1; d >= 0; d--) {
      if (containerParents.has($from.node(d).type.name)) {
        const startPos = $from.before(d + 1)
        const endPos = Math.min(state.doc.content.size, $to.after(Math.min($to.depth, d + 1)))
        range = state.doc.resolve(startPos).blockRange(state.doc.resolve(Math.max(startPos, endPos - 1))) ?? range
        break
      }
    }
  }

  try {
    const tr = state.tr.wrap(range, [
      {
        type: boxType,
        attrs: {
          bgColor: 'transparent',
          textColor: '',
          borderColor: '#e4e4e7',
          borderWidth: 0,
          borderSide: 'all',
          radius: 10,
          shadow: 'none',
          paddingX: 6,
          paddingY: 6,
          marginTop: 0,
          marginBottom: 12,
          widthPct: 100,
          align: 'left',
        },
      },
    ])
    view.dispatch(tr.scrollIntoView())
    return true
  } catch {
    return false
  }
}

export function unwrapBoxNode(editor: Editor, explicitPos?: number): boolean {
  const { state, view } = editor
  let targetPos: number | null = null

  if (typeof explicitPos === 'number' && explicitPos >= 0 && explicitPos < state.doc.content.size) {
    const candidate = state.doc.nodeAt(explicitPos)
    if (candidate?.type.name === 'boxBlock') {
      targetPos = explicitPos
    }
  }

  if (targetPos === null) {
    const $from = state.selection.$from
    for (let d = $from.depth; d >= 1; d--) {
      if ($from.node(d).type.name === 'boxBlock') {
        targetPos = $from.before(d)
        break
      }
    }
  }

  if (targetPos === null) return false
  const boxNode = state.doc.nodeAt(targetPos)
  if (!boxNode || boxNode.type.name !== 'boxBlock' || boxNode.childCount === 0) return false

  try {
    const tr = state.tr.replaceWith(targetPos, targetPos + boxNode.nodeSize, boxNode.content)
    view.dispatch(tr.scrollIntoView())
    if (selectedBlockPos.value === targetPos) selectedBlockPos.value = null
    return true
  } catch {
    return false
  }
}

export function reorderTopLevelBlock(editor: Editor, fromIndex: number, toIndex: number): boolean {
  const { state, view } = editor
  const doc = state.doc
  if (
    fromIndex === toIndex ||
    fromIndex < 0 ||
    toIndex < 0 ||
    fromIndex >= doc.childCount ||
    toIndex >= doc.childCount
  ) {
    return false
  }

  let fromPos = 0
  for (let i = 0; i < fromIndex; i++) {
    fromPos += doc.child(i).nodeSize
  }
  const movingNode = doc.child(fromIndex)

  const tr = state.tr.delete(fromPos, fromPos + movingNode.nodeSize)
  const postDeleteDoc = tr.doc

  let insertPos = 0
  const clampedTarget = Math.max(0, Math.min(toIndex, postDeleteDoc.childCount))
  for (let i = 0; i < clampedTarget; i++) {
    insertPos += postDeleteDoc.child(i).nodeSize
  }

  tr.insert(insertPos, movingNode)
  view.dispatch(tr.scrollIntoView())
  return true
}
