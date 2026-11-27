import type { Editor } from '@tiptap/core'
import Table from '@tiptap/extension-table'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import type { Node as ProseMirrorNode } from '@tiptap/pm/model'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { CellSelection, TableMap, TableView } from '@tiptap/pm/tables'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

export type TableBorderStyle = 'none' | 'horizontal' | 'grid'
export type TableCellPadding = 'compact' | 'normal' | 'relaxed'

export interface StyledTableAttrs {
  borderStyle: TableBorderStyle
  borderColor: string
  cellPadding: TableCellPadding
  striped: boolean
}

export class StyledTableView extends TableView {
  constructor(node: ProseMirrorNode, cellMinWidth: number) {
    super(node, cellMinWidth)
    this.syncTableAttrs(node)
  }

  override update(node: ProseMirrorNode): boolean {
    const ok = super.update(node)
    if (ok) {
      this.syncTableAttrs(node)
    }
    return ok
  }

  private syncTableAttrs(node: ProseMirrorNode) {
    this.table.classList.add('cv-doc-table')
    this.table.setAttribute('data-table-style', String(node.attrs.borderStyle || 'none'))
    this.table.setAttribute('data-cell-padding', String(node.attrs.cellPadding || 'normal'))
    this.table.setAttribute('data-striped', node.attrs.striped ? 'true' : 'false')
    const borderColor = typeof node.attrs.borderColor === 'string' ? node.attrs.borderColor.trim() : ''
    if (borderColor) {
      this.table.setAttribute('data-border-color', borderColor)
      this.table.style.setProperty('--cv-table-border', borderColor)
    } else {
      this.table.removeAttribute('data-border-color')
      this.table.style.removeProperty('--cv-table-border')
    }
  }
}

const tableActiveHighlightKey = new PluginKey('cvTableActiveHighlight')

function findInnermostTableAndCell(state: Editor['state']) {
  const { $from } = state.selection
  let tablePos: number | null = null
  let tableNode: ProseMirrorNode | null = null
  let cellPos: number | null = null
  let cellNode: ProseMirrorNode | null = null

  for (let d = $from.depth; d >= 1; d--) {
    const n = $from.node(d)
    if (cellPos === null && (n.type.name === 'tableCell' || n.type.name === 'tableHeader')) {
      cellPos = $from.before(d)
      cellNode = n
    }
    if (n.type.name === 'table') {
      tablePos = $from.before(d)
      tableNode = n
      break
    }
  }

  // Fallback for CellSelection where $from sits at cell position
  if (tablePos !== null && cellPos === null && state.selection instanceof CellSelection) {
    cellPos = state.selection.$anchorCell.pos
    cellNode = state.doc.nodeAt(cellPos)
  }

  if (tablePos === null || !tableNode) return null
  return { tablePos, tableNode, cellPos, cellNode }
}

export const StyledTable = Table.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      borderStyle: {
        default: 'none',
        parseHTML: (el) => el.getAttribute('data-table-style') || 'none',
        renderHTML: (attrs) => ({ 'data-table-style': attrs.borderStyle || 'none' }),
      },
      borderColor: {
        default: '',
        parseHTML: (el) => el.getAttribute('data-border-color') || '',
        renderHTML: (attrs) =>
          attrs.borderColor
            ? {
                'data-border-color': attrs.borderColor,
                style: `--cv-table-border: ${attrs.borderColor}`,
              }
            : {},
      },
      cellPadding: {
        default: 'normal',
        parseHTML: (el) => el.getAttribute('data-cell-padding') || 'normal',
        renderHTML: (attrs) => ({ 'data-cell-padding': attrs.cellPadding || 'normal' }),
      },
      striped: {
        default: false,
        parseHTML: (el) => el.getAttribute('data-striped') === 'true',
        renderHTML: (attrs) => ({ 'data-striped': attrs.striped ? 'true' : 'false' }),
      },
    }
  },

  addProseMirrorPlugins() {
    const parentPlugins = this.parent?.() ?? []
    const editorRef = this.editor
    const highlightPlugin = new Plugin({
      key: tableActiveHighlightKey,
      props: {
        decorations: (state) => {
          if (!editorRef.isEditable) return null
          const found = findInnermostTableAndCell(state)
          if (!found) return null

          const { tablePos, tableNode, cellPos, cellNode } = found
          const decos: Decoration[] = [
            Decoration.node(tablePos, tablePos + tableNode.nodeSize, {
              class: 'is-table-focused',
            }),
          ]

          if (cellPos !== null && cellNode) {
            const tableStart = tablePos + 1
            const map = TableMap.get(tableNode)
            const relCellPos = cellPos - tableStart
            if (relCellPos >= 0 && relCellPos < tableNode.content.size) {
              const rect = map.findCell(relCellPos)
              const colCellOffsets = new Set<number>()
              for (let col = rect.left; col < rect.right; col++) {
                for (let row = 0; row < map.height; row++) {
                  colCellOffsets.add(map.map[row * map.width + col])
                }
              }
              colCellOffsets.forEach((rel) => {
                const target = tableNode.nodeAt(rel)
                if (target) {
                  const absPos = tableStart + rel
                  decos.push(
                    Decoration.node(absPos, absPos + target.nodeSize, {
                      class: 'is-active-col',
                    }),
                  )
                }
              })
            }

            decos.push(
              Decoration.node(cellPos, cellPos + cellNode.nodeSize, {
                class: 'is-active-cell',
              }),
            )
          }

          return DecorationSet.create(state.doc, decos)
        },
      },
    })

    return [...parentPlugins, highlightPlugin]
  },
})

function addBgAttr() {
  return {
    backgroundColor: {
      default: null as string | null,
      parseHTML: (el: HTMLElement) => el.style.backgroundColor || null,
      renderHTML: (attrs: Record<string, unknown>) =>
        attrs.backgroundColor ? { style: `background-color: ${String(attrs.backgroundColor)}` } : {},
    },
  }
}

export const StyledTableCell = TableCell.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      ...addBgAttr(),
    }
  },
})

export const StyledTableHeader = TableHeader.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      ...addBgAttr(),
    }
  },
})

export interface ActiveTableInfo {
  node: ProseMirrorNode
  pos: number
  attrs: StyledTableAttrs
  cellPos: number | null
  cellBg: string
  colIndex: number
  colCount: number
  rowIndex: number
  rowCount: number
}

export function getActiveTableInfo(editor: Editor | null): ActiveTableInfo | null {
  if (!editor) return null
  const found = findInnermostTableAndCell(editor.state)
  if (!found) return null

  const { tablePos, tableNode, cellPos, cellNode } = found
  const map = TableMap.get(tableNode)
  let colIndex = 0
  let rowIndex = 0
  if (cellPos !== null) {
    const rel = cellPos - (tablePos + 1)
    if (rel >= 0 && rel < tableNode.content.size) {
      const rect = map.findCell(rel)
      colIndex = rect.left
      rowIndex = rect.top
    }
  }

  return {
    node: tableNode,
    pos: tablePos,
    attrs: {
      borderStyle: (tableNode.attrs.borderStyle as TableBorderStyle) || 'none',
      borderColor: String(tableNode.attrs.borderColor || ''),
      cellPadding: (tableNode.attrs.cellPadding as TableCellPadding) || 'normal',
      striped: Boolean(tableNode.attrs.striped),
    },
    cellPos,
    cellBg: String(cellNode?.attrs?.backgroundColor || ''),
    colIndex,
    colCount: map.width,
    rowIndex,
    rowCount: map.height,
  }
}

export function updateActiveTableAttrs(editor: Editor | null, patch: Partial<StyledTableAttrs>): boolean {
  const info = getActiveTableInfo(editor)
  if (!editor || !info) return false
  const tr = editor.state.tr.setNodeMarkup(info.pos, undefined, {
    ...info.node.attrs,
    ...patch,
  })
  editor.view.dispatch(tr)
  editor.view.focus()
  return true
}

export function selectActiveTableColumn(editor: Editor | null): boolean {
  const info = getActiveTableInfo(editor)
  if (!editor || !info) return false
  const map = TableMap.get(info.node)
  const tableStart = info.pos + 1
  const col = Math.max(0, Math.min(info.colIndex, map.width - 1))
  const firstRel = map.map[col]
  const lastRel = map.map[(map.height - 1) * map.width + col]
  const $anchor = editor.state.doc.resolve(tableStart + firstRel)
  const $head = editor.state.doc.resolve(tableStart + lastRel)
  const sel = CellSelection.colSelection($anchor, $head)
  editor.view.dispatch(editor.state.tr.setSelection(sel))
  editor.view.focus()
  return true
}

export function selectActiveTableRow(editor: Editor | null): boolean {
  const info = getActiveTableInfo(editor)
  if (!editor || !info) return false
  const map = TableMap.get(info.node)
  const tableStart = info.pos + 1
  const row = Math.max(0, Math.min(info.rowIndex, map.height - 1))
  const firstRel = map.map[row * map.width]
  const lastRel = map.map[row * map.width + (map.width - 1)]
  const $anchor = editor.state.doc.resolve(tableStart + firstRel)
  const $head = editor.state.doc.resolve(tableStart + lastRel)
  const sel = CellSelection.rowSelection($anchor, $head)
  editor.view.dispatch(editor.state.tr.setSelection(sel))
  editor.view.focus()
  return true
}

export function resetActiveTableColumnWidths(editor: Editor | null): boolean {
  const info = getActiveTableInfo(editor)
  if (!editor || !info) return false
  const tr = editor.state.tr
  let changed = false
  info.node.descendants((child, relPos) => {
    if ((child.type.name === 'tableCell' || child.type.name === 'tableHeader') && child.attrs.colwidth) {
      const absPos = info.pos + 1 + relPos
      tr.setNodeMarkup(absPos, undefined, { ...child.attrs, colwidth: null })
      changed = true
    }
    return true
  })
  if (changed) {
    editor.view.dispatch(tr)
  }
  editor.view.focus()
  return true
}
