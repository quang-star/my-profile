import { mergeAttributes, Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import BadgeListView from '@/editor/views/BadgeListView.vue'

export interface BadgeListAttrs {
  items: string[]
  bgColor: string
  textColor: string
  borderColor: string
  radius: number
  align: 'left' | 'center' | 'right'
  marginBottom: number
}

export const BadgeListBlock = Node.create({
  name: 'badgeListBlock',
  group: 'block',
  atom: true,

  addAttributes() {
    return {
      items: { default: ['Vue 3', 'TypeScript', 'Tailwind CSS'] },
      bgColor: { default: '' },
      textColor: { default: '' },
      borderColor: { default: '' },
      radius: { default: 999 },
      align: { default: 'left' },
      marginBottom: { default: 10 },
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-type="badge-list-block"]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'badge-list-block' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(BadgeListView)
  },
})
