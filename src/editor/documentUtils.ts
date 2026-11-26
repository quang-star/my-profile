import type { JSONContent } from '@tiptap/core'
import type { BlockState, ContentNode } from '@/types/node'

export function isDocumentTree(nodes: ContentNode[]): boolean {
  return nodes.some((node) => node.tag === 'document')
}

function textNode(text: string, marks?: JSONContent['marks']): JSONContent {
  return marks?.length ? { type: 'text', text, marks } : { type: 'text', text }
}

function htmlToInlineContent(rawHtml: string): JSONContent[] {
  const clean = rawHtml.trim()
  if (!clean) return []
  if (typeof DOMParser === 'undefined') {
    const plain = clean.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
    return plain ? [textNode(plain)] : []
  }

  const doc = new DOMParser().parseFromString(clean, 'text/html')
  const out: JSONContent[] = []

  function walk(node: Node, activeMarks: NonNullable<JSONContent['marks']>) {
    if (node.nodeType === Node.TEXT_NODE) {
      const txt = node.textContent || ''
      if (txt) out.push(textNode(txt, activeMarks))
      return
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return
    const el = node as HTMLElement
    const tag = el.tagName.toLowerCase()
    if (tag === 'br') {
      out.push({ type: 'hardBreak' })
      return
    }
    const nextMarks = [...activeMarks]
    if (tag === 'strong' || tag === 'b') nextMarks.push({ type: 'bold' })
    else if (tag === 'em' || tag === 'i') nextMarks.push({ type: 'italic' })
    else if (tag === 'u') nextMarks.push({ type: 'underline' })
    else if (tag === 's' || tag === 'strike') nextMarks.push({ type: 'strike' })
    else if (tag === 'code') nextMarks.push({ type: 'code' })
    else if (tag === 'a') {
      const href = el.getAttribute('href')?.trim()
      if (href && /^(https?:\/\/|mailto:|tel:|#)/i.test(href)) {
        nextMarks.push({ type: 'link', attrs: { href } })
      }
    }

    const styleColor = el.style?.color?.trim()
    const styleBg = el.style?.backgroundColor?.trim()
    const styleFontSize = el.style?.fontSize?.trim()
    if (styleColor || styleFontSize) {
      nextMarks.push({
        type: 'textStyle',
        attrs: {
          ...(styleColor ? { color: styleColor } : {}),
          ...(styleFontSize ? { fontSize: styleFontSize } : {}),
        },
      })
    }
    if (styleBg) {
      nextMarks.push({ type: 'highlight', attrs: { color: styleBg } })
    }

    for (const child of Array.from(el.childNodes)) {
      walk(child, nextMarks)
    }
  }

  for (const child of Array.from(doc.body.childNodes)) {
    walk(child, [])
  }
  return out
}

export function htmlToTiptapBlocks(rawHtml: string, color?: string, fontSizePx?: number): JSONContent[] {
  const clean = rawHtml.trim()
  if (!clean) return [{ type: 'paragraph' }]
  if (typeof DOMParser === 'undefined') {
    const plain = clean.replace(/<[^>]+>/g, ' ').trim()
    return [{ type: 'paragraph', content: plain ? [textNode(plain)] : undefined }]
  }

  const doc = new DOMParser().parseFromString(clean, 'text/html')
  const blocks: JSONContent[] = []

  const textStyleMark =
    color || fontSizePx
      ? [{ type: 'textStyle', attrs: { ...(color ? { color } : {}), ...(fontSizePx ? { fontSize: `${fontSizePx}px` } : {}) } }]
      : undefined

  function pushParagraphFrom(el: Node) {
    const html = el instanceof HTMLElement ? el.innerHTML : el.textContent || ''
    const inline = htmlToInlineContent(html)
    if (!inline.length) return
    const styled = textStyleMark
      ? inline.map((item) =>
          item.type === 'text'
            ? { ...item, marks: [...(textStyleMark || []), ...(item.marks || [])] }
            : item,
        )
      : inline
    blocks.push({ type: 'paragraph', content: styled })
  }

  for (const child of Array.from(doc.body.childNodes)) {
    if (child.nodeType === Node.TEXT_NODE) {
      const t = child.textContent?.trim()
      if (t) {
        blocks.push({ type: 'paragraph', content: [textNode(t, textStyleMark)] })
      }
      continue
    }
    if (child.nodeType !== Node.ELEMENT_NODE) continue
    const el = child as HTMLElement
    const tag = el.tagName.toLowerCase()
    if (tag === 'ul' || tag === 'ol') {
      const items = Array.from(el.querySelectorAll(':scope > li'))
        .map((li) => {
          const inline = htmlToInlineContent(li.innerHTML)
          if (!inline.length) return null
          return { type: 'listItem', content: [{ type: 'paragraph', content: inline }] }
        })
        .filter((x): x is NonNullable<typeof x> => !!x)
      if (items.length) {
        blocks.push({ type: tag === 'ol' ? 'orderedList' : 'bulletList', content: items })
      }
    } else {
      pushParagraphFrom(el)
    }
  }

  return blocks.length ? blocks : [{ type: 'paragraph' }]
}

function legacyNodeToTiptap(node: ContentNode, locale: 'vi' | 'en'): JSONContent[] {
  if (node.attrs?.hidden) return []
  const raw = locale === 'vi'
    ? node.contentVi || node.contentEn || ''
    : node.contentEn || node.contentVi || ''
  const plain = raw.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  const a = node.attrs || {}

  if (node.tag === 'section') {
    const inner = node.children.flatMap((c) => legacyNodeToTiptap(c, locale))
    if (!inner.length) return []
    if (a.bgColor && a.bgColor !== 'transparent') {
      return [{
        type: 'boxBlock',
        attrs: {
          bgColor: String(a.bgColor),
          textColor: a.textColor ? String(a.textColor) : '',
          borderColor: a.borderColor ? String(a.borderColor) : 'transparent',
          borderWidth: a.borderColor ? 1 : 0,
          radius: 14,
          paddingX: 20,
          paddingY: 16,
          marginBottom: 16,
        },
        content: inner,
      }]
    }
    return inner
  }

  if (node.tag === 'grid') {
    const cols = Math.min(3, Math.max(2, Number(a.cols) || 2))
    const children = node.children.slice(0, cols)
    if (children.length < 2) {
      return node.children.flatMap((c) => legacyNodeToTiptap(c, locale))
    }
    const widths = Array.isArray(a.colWidths) && a.colWidths.length === children.length
      ? a.colWidths.map((w) => Number(w) || 50)
      : Array.from({ length: children.length }, () => Math.round(100 / children.length))
    return [{
      type: 'columnsBlock',
      attrs: { widths, gap: Math.max(8, (Number(a.gap) || 4) * 4), marginBottom: 16, stackMobile: true },
      content: children.map((child) => {
        const inner = legacyNodeToTiptap(child, locale)
        return {
          type: 'columnCell',
          attrs: {
            bgColor: child.tag === 'card' && child.attrs?.bgColor ? String(child.attrs.bgColor) : 'transparent',
            textColor: child.attrs?.textColor ? String(child.attrs.textColor) : '',
            borderColor: child.tag === 'card' && child.attrs?.borderColor ? String(child.attrs.borderColor) : 'transparent',
            paddingX: 12,
            paddingY: 12,
            radius: 12,
          },
          content: inner.length ? inner : [{ type: 'paragraph' }],
        }
      }),
    }]
  }

  if (node.tag === 'card') {
    const inner = node.children.flatMap((c) => legacyNodeToTiptap(c, locale))
    if (!inner.length) return []
    if (a.bgColor === 'transparent' && (!a.borderColor || a.borderColor === 'transparent')) {
      return inner
    }
    return [{
      type: 'boxBlock',
      attrs: {
        bgColor: String(a.bgColor || '#ffffff'),
        textColor: a.textColor ? String(a.textColor) : '',
        borderColor: String(a.borderColor || '#e4e4e7'),
        borderWidth: a.borderWidth !== undefined ? Number(a.borderWidth) : 1,
        radius: 14,
        paddingX: 18,
        paddingY: 14,
        marginBottom: 14,
      },
      content: inner,
    }]
  }

  if (node.tag === 'heading') {
    const level = a.level === 'h1' ? 1 : a.level === 'h3' ? 3 : a.level === 'h4' ? 4 : 2
    const color = a.textColor ? String(a.textColor) : undefined
    return [{
      type: 'heading',
      attrs: { level, ...(a.align ? { textAlign: a.align } : {}) },
      content: plain ? [textNode(plain, color ? [{ type: 'textStyle', attrs: { color } }] : undefined)] : [],
    }]
  }

  if (node.tag === 'text' || node.tag === 'list') {
    return htmlToTiptapBlocks(raw, a.textColor ? String(a.textColor) : undefined)
  }

  if (node.tag === 'badge-group') {
    const tags = node.children
      .map((ch) => (locale === 'vi' ? ch.contentVi || ch.contentEn : ch.contentEn || ch.contentVi))
      .map((t) => t.replace(/<[^>]+>/g, '').trim())
      .filter(Boolean)
    if (!tags.length) return []
    const first = node.children[0]?.attrs
    return [{
      type: 'badgeListBlock',
      attrs: {
        items: tags,
        bgColor: first?.bgColor ? String(first.bgColor) : '',
        textColor: first?.textColor ? String(first.textColor) : '',
        borderColor: first?.borderColor ? String(first.borderColor) : '',
      },
    }]
  }

  if (node.tag === 'badge' && plain) {
    return [{
      type: 'badgeListBlock',
      attrs: {
        items: [plain],
        bgColor: a.bgColor ? String(a.bgColor) : '',
        textColor: a.textColor ? String(a.textColor) : '',
        borderColor: a.borderColor ? String(a.borderColor) : '',
      },
    }]
  }

  if (node.tag === 'link-button' && plain) {
    return [{
      type: 'linkButtonBlock',
      attrs: {
        items: [{ label: plain, href: String(a.href || '#'), variant: 'primary' }],
      },
    }]
  }

  if (node.tag === 'metric') {
    const val = String(a.value || '0')
    return [{
      type: 'paragraph',
      content: [
        textNode(`${val} `, [{ type: 'bold' }, ...(a.valueColor ? [{ type: 'textStyle', attrs: { color: String(a.valueColor), fontSize: '20px' } }] : [])]),
        ...(plain ? [textNode(plain)] : []),
      ],
    }]
  }

  if (node.tag === 'image' && a.src) {
    return [{ type: 'image', attrs: { src: String(a.src), alt: String(a.alt || '') } }]
  }

  if (node.tag === 'divider') {
    return [{ type: 'horizontalRule' }]
  }

  return []
}

export function legacyTreeToTiptapDoc(nodes: ContentNode[], locale: 'vi' | 'en'): JSONContent {
  const content = nodes.flatMap((n) => legacyNodeToTiptap(n, locale))
  return {
    type: 'doc',
    content: content.length ? content : [{ type: 'paragraph' }],
  }
}

export function createStarterDocument(locale: 'vi' | 'en'): JSONContent {
  const isVi = locale === 'vi'
  return {
    type: 'doc',
    content: [
      {
        type: 'boxBlock',
        attrs: {
          bgColor: '#0f172a',
          textColor: '#f8fafc',
          borderColor: 'transparent',
          borderWidth: 0,
          radius: 14,
          shadow: 'sm',
          paddingX: 24,
          paddingY: 22,
          marginTop: 0,
          marginBottom: 18,
          widthPct: 100,
          align: 'left',
        },
        content: [
          {
            type: 'heading',
            attrs: { level: 1 },
            content: [
              textNode(isVi ? 'Nguyễn Văn A' : 'Alex Nguyen', [{ type: 'textStyle', attrs: { color: '#ffffff', fontSize: '28px' } }]),
            ],
          },
          {
            type: 'paragraph',
            content: [
              textNode(
                isVi ? 'Full-stack Software Engineer · 3+ năm kinh nghiệm' : 'Full-stack Software Engineer · 3+ years experience',
                [{ type: 'bold' }, { type: 'textStyle', attrs: { color: '#38bdf8', fontSize: '15px' } }],
              ),
            ],
          },
          {
            type: 'paragraph',
            content: [
              textNode(
                isVi
                  ? 'Email: hello@example.com  ·  SĐT: 0912 345 678  ·  Hà Nội, Việt Nam'
                  : 'Email: hello@example.com  ·  Phone: +84 912 345 678  ·  Hanoi, Vietnam',
                [{ type: 'textStyle', attrs: { color: '#cbd5e1', fontSize: '13px' } }],
              ),
            ],
          },
        ],
      },
      {
        type: 'columnsBlock',
        attrs: { widths: [34, 66], gap: 20, marginTop: 0, marginBottom: 16, stackMobile: true },
        content: [
          {
            type: 'columnCell',
            attrs: { bgColor: '#f8fafc', textColor: '', borderColor: '#e2e8f0', paddingX: 16, paddingY: 16, radius: 12 },
            content: [
              {
                type: 'heading',
                attrs: { level: 3 },
                content: [textNode(isVi ? 'KỸ NĂNG CHÍNH' : 'CORE SKILLS')],
              },
              {
                type: 'badgeListBlock',
                attrs: { items: ['TypeScript', 'Vue 3', 'Java', 'Spring Boot', 'PostgreSQL', 'Docker'], marginBottom: 12 },
              },
              {
                type: 'skillBarBlock',
                attrs: { label: 'Frontend (Vue / React)', level: 90, showPercent: true, marginBottom: 10 },
              },
              {
                type: 'skillBarBlock',
                attrs: { label: 'Backend (Java / Node.js)', level: 85, showPercent: true, marginBottom: 14 },
              },
              {
                type: 'heading',
                attrs: { level: 3 },
                content: [textNode(isVi ? 'HỌC VẤN' : 'EDUCATION')],
              },
              {
                type: 'paragraph',
                content: [
                  textNode(isVi ? 'Đại học Bách Khoa\n' : 'University of Technology\n', [{ type: 'bold' }]),
                  textNode(isVi ? 'Kỹ thuật Phần mềm · 2019 — 2023' : 'Software Engineering · 2019 — 2023'),
                ],
              },
            ],
          },
          {
            type: 'columnCell',
            attrs: { bgColor: 'transparent', textColor: '', borderColor: 'transparent', paddingX: 6, paddingY: 4, radius: 10 },
            content: [
              {
                type: 'heading',
                attrs: { level: 2 },
                content: [textNode(isVi ? 'MỤC TIÊU NGHỀ NGHIỆP' : 'SUMMARY')],
              },
              {
                type: 'paragraph',
                content: [
                  textNode(
                    isVi
                      ? 'Bạn có thể gõ tự do như Word ở mọi vị trí, chỉnh cỡ chữ px, màu sắc, căn lề trái/giữa/phải/đều hoặc chèn thêm Khung, Cột, Bảng từ thanh công cụ bên trên.'
                      : 'Edit freely like Word anywhere on the page, adjust px font size, colors, alignment, or insert nested Boxes, Columns and Tables from the ribbon above.',
                  ),
                ],
              },
              {
                type: 'heading',
                attrs: { level: 2 },
                content: [textNode(isVi ? 'KINH NGHIỆM LÀM VIỆC' : 'EXPERIENCE')],
              },
              {
                type: 'paragraph',
                content: [
                  textNode(isVi ? 'Senior Frontend Developer · Công ty ABC' : 'Senior Frontend Developer · ABC Corp', [{ type: 'bold' }]),
                  textNode(isVi ? ' (03/2023 — Hiện tại)' : ' (03/2023 — Present)'),
                ],
              },
              {
                type: 'bulletList',
                content: [
                  {
                    type: 'listItem',
                    content: [{
                      type: 'paragraph',
                      content: [textNode(isVi ? 'Thiết kế kiến trúc giao diện và tối ưu tốc độ tải trang giảm 40%.' : 'Architected UI components and improved page load speed by 40%.')],
                    }],
                  },
                  {
                    type: 'listItem',
                    content: [{
                      type: 'paragraph',
                      content: [textNode(isVi ? 'Phối hợp chặt chẽ với Backend xây dựng API RESTful và hệ thống phân quyền.' : 'Collaborated with backend engineers on RESTful APIs and RBAC.')],
                    }],
                  },
                ],
              },
              {
                type: 'linkButtonBlock',
                attrs: {
                  items: [
                    { label: isVi ? 'Xem Portfolio' : 'View Portfolio', href: 'https://github.com', variant: 'primary' },
                    { label: 'LinkedIn', href: 'https://linkedin.com', variant: 'outline' },
                  ],
                  align: 'left',
                  marginBottom: 8,
                },
              },
            ],
          },
        ],
      },
    ],
  }
}

export function extractDocumentState(nodes: ContentNode[]): {
  docNodeId: string | null
  docVi: JSONContent
  docEn: JSONContent
  isLegacy: boolean
} {
  const docNode = nodes.find((n) => n.tag === 'document')
  if (docNode) {
    const attrs = docNode.attrs as Record<string, unknown>
    const docVi = (attrs.docVi && typeof attrs.docVi === 'object' ? attrs.docVi : null) as JSONContent | null
    const docEn = (attrs.docEn && typeof attrs.docEn === 'object' ? attrs.docEn : null) as JSONContent | null
    return {
      docNodeId: docNode.id,
      docVi: docVi ?? createStarterDocument('vi'),
      docEn: docEn ?? docVi ?? createStarterDocument('en'),
      isLegacy: false,
    }
  }

  if (nodes.length > 0) {
    return {
      docNodeId: null,
      docVi: legacyTreeToTiptapDoc(nodes, 'vi'),
      docEn: legacyTreeToTiptapDoc(nodes, 'en'),
      isLegacy: true,
    }
  }

  return {
    docNodeId: null,
    docVi: createStarterDocument('vi'),
    docEn: createStarterDocument('en'),
    isLegacy: false,
  }
}

export function extractPlainTextSummary(doc: JSONContent, maxChars = 4000): string {
  const parts: string[] = []
  function walk(node: JSONContent) {
    if (node.text) parts.push(node.text)
    if (Array.isArray(node.content)) {
      for (const child of node.content) {
        walk(child)
        if (parts.join(' ').length >= maxChars) break
      }
    }
  }
  walk(doc)
  return parts.join(' ').replace(/\s+/g, ' ').trim().slice(0, maxChars)
}

export function buildDocumentBlockState(
  docNodeId: string | null,
  docVi: JSONContent,
  docEn: JSONContent,
): BlockState {
  const id = docNodeId || `doc-${crypto.randomUUID().slice(0, 12)}`
  return {
    id,
    parentId: null,
    tag: 'document',
    sortOrder: 0,
    attrs: {
      version: 2,
      docVi,
      docEn,
    },
    contentVi: extractPlainTextSummary(docVi),
    contentEn: extractPlainTextSummary(docEn),
  }
}
