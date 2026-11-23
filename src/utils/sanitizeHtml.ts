const ALLOWED_TAGS = new Set([
  'P',
  'BR',
  'STRONG',
  'B',
  'EM',
  'I',
  'U',
  'S',
  'STRIKE',
  'CODE',
  'SPAN',
  'MARK',
  'A',
  'UL',
  'OL',
  'LI',
])

const ALLOWED_ATTRS = new Set(['style', 'href', 'target', 'rel', 'class', 'data-color', 'data-tag'])

export function sanitizeRichHtml(raw: string): string {
  if (!raw) return ''
  if (typeof window === 'undefined' || typeof DOMParser === 'undefined') {
    return raw
  }

  const doc = new DOMParser().parseFromString(raw, 'text/html')

  function cleanNode(node: Node): void {
    const children = Array.from(node.childNodes)
    for (const child of children) {
      if (child.nodeType === Node.ELEMENT_NODE) {
        const el = child as HTMLElement
        if (!ALLOWED_TAGS.has(el.tagName)) {
          // Unwrap disallowed tag while keeping text/safe children
          cleanNode(el)
          const frag = doc.createDocumentFragment()
          while (el.firstChild) {
            frag.appendChild(el.firstChild)
          }
          node.replaceChild(frag, el)
          continue
        }

        // Filter attributes
        for (const attr of Array.from(el.attributes)) {
          if (!ALLOWED_ATTRS.has(attr.name.toLowerCase())) {
            el.removeAttribute(attr.name)
          }
        }

        if (el.tagName === 'A') {
          const href = el.getAttribute('href') || ''
          if (/^\s*javascript:/i.test(href)) {
            el.removeAttribute('href')
          }
          if (el.getAttribute('target') === '_blank') {
            el.setAttribute('rel', 'noopener noreferrer')
          }
        }

        cleanNode(el)
      } else if (child.nodeType === Node.COMMENT_NODE) {
        node.removeChild(child)
      }
    }
  }

  cleanNode(doc.body)
  return doc.body.innerHTML
}
