import { mergeAttributes, Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import ColumnsBlockView from '@/editor/views/ColumnsBlockView.vue'

export interface ColumnsBlockAttrs {
  widths: number[]
  gap: number
  marginTop: number
  marginBottom: number
  stackMobile: boolean
}

export const ColumnsBlock = Node.create({
  name: 'columnsBlock',
  group: 'block',
  content: 'columnCell{2,6}',
  defining: true,
  isolating: true,
  draggable: true,

  addAttributes() {
    return {
      widths: { default: [50, 50] },
      gap: { default: 20 },
      marginTop: { default: 0 },
      marginBottom: { default: 16 },
      stackMobile: { default: true },
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-type="columns-block"]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'columns-block' }), 0]
  },

  addNodeView() {
    return VueNodeViewRenderer(ColumnsBlockView)
  },
})
