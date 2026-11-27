import { Extension } from '@tiptap/core'

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    fontSize: {
      setFontSize: (fontSize: string) => ReturnType
      unsetFontSize: () => ReturnType
    }
  }
}

export const FontSize = Extension.create({
  name: 'fontSize',

  addOptions() {
    return {
      types: ['textStyle'],
    }
  },

  addGlobalAttributes() {
    return [
      {
        types: this.options.types,
        attributes: {
          fontSize: {
            default: null,
            parseHTML: (element) => {
              const dataAttr = element.getAttribute('data-font-size')
              if (dataAttr) return dataAttr
              const raw = element.style.fontSize?.replace(/['"]+/g, '') || ''
              if (!raw) return null
              const calcMatch = raw.match(/calc\(\s*([0-9.]+px)\s*\*/i)
              if (calcMatch?.[1]) return calcMatch[1]
              return raw
            },
            renderHTML: (attributes) => {
              if (!attributes.fontSize) return {}
              const val = String(attributes.fontSize).trim()
              if (/^[0-9.]+px$/i.test(val)) {
                return {
                  'data-font-size': val,
                  style: `font-size: calc(${val} * var(--cv-font-scale, 1))`,
                }
              }
              return { style: `font-size: ${val}` }
            },
          },
        },
      },
    ]
  },

  addCommands() {
    return {
      setFontSize:
        (fontSize: string) =>
        ({ chain }) =>
          chain().setMark('textStyle', { fontSize }).run(),
      unsetFontSize:
        () =>
        ({ chain }) =>
          chain().setMark('textStyle', { fontSize: null }).removeEmptyTextStyle().run(),
    }
  },
})
