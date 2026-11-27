import { mergeAttributes, Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import LinkButtonView from '@/editor/views/LinkButtonView.vue'

export interface LinkButtonItem {
  label: string
  href: string
  variant?: 'primary' | 'outline'
}

export interface LinkButtonBlockAttrs {
  items: LinkButtonItem[]
  align: 'left' | 'center' | 'right'
  marginBottom: number
}

export const LinkButtonBlock = Node.create({
  name: 'linkButtonBlock',
  group: 'block',
  atom: true,

  addAttributes() {
    return {
      items: {
        default: [
          { label: 'Liên hệ Email', href: 'mailto:you@example.com', variant: 'primary' },
          { label: 'GitHub', href: 'https://github.com', variant: 'outline' },
        ],
      },
      align: { default: 'left' },
      marginBottom: { default: 10 },
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-type="link-button-block"]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'link-button-block' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(LinkButtonView)
  },
})
