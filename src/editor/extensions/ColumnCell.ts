import { mergeAttributes, Node } from '@tiptap/core'

export interface ColumnCellAttrs {
  bgColor: string
  textColor: string
  borderColor: string
  paddingX: number
  paddingY: number
  radius: number
}

export const ColumnCell = Node.create({
  name: 'columnCell',
  content: 'block+',
  isolating: true,
  defining: true,

  addAttributes() {
    return {
      bgColor: {
        default: 'transparent',
        parseHTML: (el) => el.getAttribute('data-bg') || el.style.backgroundColor || 'transparent',
        renderHTML: (attrs) => (attrs.bgColor && attrs.bgColor !== 'transparent' ? { 'data-bg': attrs.bgColor } : {}),
      },
      textColor: {
        default: '',
        parseHTML: (el) => el.getAttribute('data-color') || el.style.color || '',
        renderHTML: (attrs) => (attrs.textColor ? { 'data-color': attrs.textColor } : {}),
      },
      borderColor: {
        default: 'transparent',
        parseHTML: (el) => el.getAttribute('data-border-color') || el.style.borderColor || 'transparent',
        renderHTML: (attrs) => (attrs.borderColor && attrs.borderColor !== 'transparent' ? { 'data-border-color': attrs.borderColor } : {}),
      },
      paddingX: {
        default: 12,
        parseHTML: (el) => Number(el.getAttribute('data-px')) || 12,
        renderHTML: (attrs) => ({ 'data-px': String(attrs.paddingX ?? 12) }),
      },
      paddingY: {
        default: 12,
        parseHTML: (el) => Number(el.getAttribute('data-py')) || 12,
        renderHTML: (attrs) => ({ 'data-py': String(attrs.paddingY ?? 12) }),
      },
      radius: {
        default: 10,
        parseHTML: (el) => Number(el.getAttribute('data-radius')) || 10,
        renderHTML: (attrs) => ({ 'data-radius': String(attrs.radius ?? 10) }),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-type="column-cell"]' }]
  },

  renderHTML({ node, HTMLAttributes }) {
    const a = node.attrs as ColumnCellAttrs
    const styles: string[] = [
      `padding: ${Number(a.paddingY ?? 12)}px ${Number(a.paddingX ?? 12)}px`,
      `border-radius: ${Number(a.radius ?? 10)}px`,
      'min-width: 0',
      'box-sizing: border-box',
    ]
    if (a.bgColor && a.bgColor !== 'transparent') {
      styles.push(`background-color: ${a.bgColor}`)
    }
    if (a.textColor) {
      styles.push(`color: ${a.textColor}`)
    }
    if (a.borderColor && a.borderColor !== 'transparent') {
      styles.push(`border: 1px solid ${a.borderColor}`)
    }
    return [
      'div',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'column-cell',
        class: 'cv-column-cell',
        style: styles.join('; '),
      }),
      0,
    ]
  },
})
