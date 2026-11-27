import { mergeAttributes, Node, nodeInputRule } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import DividerView from '@/editor/views/DividerView.vue'

export interface DividerAttrs {
  color: string
  thickness: number
  lineStyle: 'solid' | 'dashed' | 'dotted'
  widthPct: number
  align: 'left' | 'center' | 'right'
  marginTop: number
  marginBottom: number
}

export const DividerBlock = Node.create({
  name: 'horizontalRule',
  group: 'block',
  atom: true,
  selectable: true,
  draggable: true,

  addAttributes() {
    return {
      color: { default: '' },
      thickness: { default: 1 },
      lineStyle: { default: 'solid' },
      widthPct: { default: 100 },
      align: { default: 'left' },
      marginTop: { default: 2 },
      marginBottom: { default: 6 },
    }
  },

  parseHTML() {
    return [{ tag: 'hr' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['hr', mergeAttributes(HTMLAttributes)]
  },

  addCommands() {
    return {
      setHorizontalRule:
        () =>
        ({ chain }) =>
          chain().insertContent({ type: this.name }).run(),
    }
  },

  addInputRules() {
    return [
      nodeInputRule({
        find: /^(?:---|___\s|\*\*\*\s)$/,
        type: this.type,
      }),
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(DividerView)
  },
})
