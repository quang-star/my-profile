import type { Extensions } from '@tiptap/core'
import Color from '@tiptap/extension-color'
import FontFamily from '@tiptap/extension-font-family'
import Highlight from '@tiptap/extension-highlight'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import TableRow from '@tiptap/extension-table-row'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'
import Underline from '@tiptap/extension-underline'
import StarterKit from '@tiptap/starter-kit'
import { BadgeListBlock } from '@/editor/extensions/BadgeListBlock'
import { BoxBlock } from '@/editor/extensions/BoxBlock'
import { ColumnCell } from '@/editor/extensions/ColumnCell'
import { ColumnsBlock } from '@/editor/extensions/ColumnsBlock'
import { DividerBlock } from '@/editor/extensions/DividerBlock'
import { FontSize } from '@/editor/extensions/FontSize'
import { LineHeight } from '@/editor/extensions/LineHeight'
import { LinkButtonBlock } from '@/editor/extensions/LinkButtonBlock'
import { ResizableImage } from '@/editor/extensions/ResizableImage'
import { SkillBarBlock } from '@/editor/extensions/SkillBarBlock'
import {
  StyledTable,
  StyledTableCell,
  StyledTableHeader,
  StyledTableView,
} from '@/editor/extensions/StyledTable'

export function createDocumentExtensions(placeholderText = 'Bắt đầu gõ như Word hoặc chèn Khối bên trái…'): Extensions {
  return [
    StarterKit.configure({
      heading: { levels: [1, 2, 3, 4] },
      horizontalRule: false,
      dropcursor: {
        color: '#10b981',
        width: 3,
      },
    }),
    Underline,
    TextStyle,
    FontSize,
    LineHeight,
    Color,
    Highlight.configure({ multicolor: true }),
    FontFamily,
    TextAlign.configure({
      types: ['heading', 'paragraph'],
    }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        rel: 'noopener noreferrer',
      },
    }),
    ResizableImage.configure({
      allowBase64: false,
      HTMLAttributes: {
        class: 'cv-doc-image',
      },
    }),
    StyledTable.configure({
      resizable: true,
      View: StyledTableView,
      HTMLAttributes: {
        class: 'cv-doc-table',
      },
    }),
    TableRow,
    StyledTableHeader,
    StyledTableCell,
    Placeholder.configure({
      placeholder: placeholderText,
    }),
    DividerBlock,
    BoxBlock,
    ColumnCell,
    ColumnsBlock,
    SkillBarBlock,
    BadgeListBlock,
    LinkButtonBlock,
  ]
}
