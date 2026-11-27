import Image from '@tiptap/extension-image'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import ImageBlockView from '@/editor/views/ImageBlockView.vue'

export const ResizableImage = Image.extend({
  draggable: true,

  addAttributes() {
    return {
      ...this.parent?.(),
      width: {
        default: null,
        parseHTML: (element) => {
          const styleWidth = element.style?.width
          if (styleWidth) return styleWidth
          const attrWidth = element.getAttribute('width')
          if (!attrWidth) return null
          return /^\d+$/.test(attrWidth) ? `${attrWidth}px` : attrWidth
        },
        renderHTML: (attributes) => {
          if (!attributes.width) return {}
          return { style: `width: ${attributes.width}` }
        },
      },
      align: {
        default: 'left',
        parseHTML: (element) => element.getAttribute('data-align') || 'left',
        renderHTML: (attributes) => {
          if (!attributes.align || attributes.align === 'left') return {}
          return { 'data-align': attributes.align }
        },
      },
    }
  },

  addNodeView() {
    return VueNodeViewRenderer(ImageBlockView)
  },
})
