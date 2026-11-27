import { mergeAttributes, Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import BoxBlockView from '@/editor/views/BoxBlockView.vue'

export type BoxBorderSide = 'all' | 'left' | 'bottom' | 'top'
export type BoxShadowLevel = 'none' | 'sm' | 'md'
export type BoxAlign = 'left' | 'center' | 'right'

export interface BoxBlockAttrs {
  bgColor: string
  textColor: string
  borderColor: string
  borderWidth: number
  borderSide: BoxBorderSide
  radius: number
  shadow: BoxShadowLevel
  paddingX: number
  paddingY: number
  marginTop: number
  marginBottom: number
  widthPct: number
  align: BoxAlign
}

export const BoxBlock = Node.create({
  name: 'boxBlock',
  group: 'block',
  content: 'block+',
  defining: true,
  isolating: true,
  draggable: true,

  addAttributes() {
    return {
      bgColor: { default: '#ffffff' },
      textColor: { default: '' },
      borderColor: { default: '#e4e4e7' },
      borderWidth: { default: 1 },
      borderSide: { default: 'all' },
      radius: { default: 12 },
      shadow: { default: 'none' },
      paddingX: { default: 20 },
      paddingY: { default: 16 },
      marginTop: { default: 0 },
      marginBottom: { default: 14 },
      widthPct: { default: 100 },
      align: { default: 'left' },
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-type="box-block"]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'box-block' }), 0]
  },

  addNodeView() {
    return VueNodeViewRenderer(BoxBlockView)
  },
})
