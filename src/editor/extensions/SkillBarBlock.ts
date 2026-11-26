import { mergeAttributes, Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import SkillBarView from '@/editor/views/SkillBarView.vue'

export interface SkillBarAttrs {
  label: string
  level: number
  color: string
  showPercent: boolean
  marginBottom: number
}

export const SkillBarBlock = Node.create({
  name: 'skillBarBlock',
  group: 'block',
  atom: true,

  addAttributes() {
    return {
      label: { default: 'Kỹ năng chuyên môn' },
      level: { default: 85 },
      color: { default: '' },
      showPercent: { default: true },
      marginBottom: { default: 10 },
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-type="skill-bar-block"]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'skill-bar-block' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(SkillBarView)
  },
})
