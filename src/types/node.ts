export type NodeTag =
  | 'section'
  | 'grid'
  | 'card'
  | 'heading'
  | 'text'
  | 'badge-group'
  | 'badge'
  | 'icon'
  | 'metric'
  | 'link-button'
  | 'image'
  | 'list'
  | 'divider'
  | 'contact-form'

export type CanvasDevice = 'desktop' | 'tablet' | 'phone'

export interface NodeAttrs {
  anchor?: string
  level?: 'h1' | 'h2' | 'h3' | 'h4'
  cols?: number
  colSpan?: number
  colStart?: number
  blockWidth?: number
  blockOffset?: number
  minHeight?: number
  blockTop?: number
  gap?: number
  padding?: 'none' | 'sm' | 'md' | 'lg'
  rounded?: 'none' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | 'full'
  fontSize?: 'xs' | 'sm' | 'base' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl'
  align?: 'left' | 'center' | 'right'
  bgColor?: string
  textColor?: string
  borderColor?: string
  dotColor?: string
  value?: string
  valueColor?: string
  labelColor?: string
  accentColor?: string
  href?: string
  target?: '_self' | '_blank'
  icon?: string
  iconSize?: number
  iconColor?: string
  canvasLabel?: string
  hidden?: boolean
  stackDirection?: 'row' | 'column'
  stackWrap?: boolean
  stackJustify?: 'start' | 'center' | 'end' | 'space-between'
  responsive?: Partial<Record<'tablet' | 'phone', NodeAttrs>>
  src?: string
  alt?: string
  width?: string
  [key: string]: unknown
}

export interface ContentNode {
  id: string
  parentId: string | null
  tag: NodeTag | string
  sortOrder: number
  attrs: NodeAttrs
  contentVi: string
  contentEn: string
  children: ContentNode[]
}

export interface SaveNodePayload {
  id?: string
  parentId: string | null
  tag: string
  sortOrder?: number
  beforeId?: string
  attrs: NodeAttrs
  contentVi: string
  contentEn: string
}

export interface NodeReorderItem {
  id: string
  parentId: string | null
  sortOrder: number
  clearGridPosition?: boolean
}

export type BlockState = Omit<ContentNode, 'children'>

export interface BlockChanges {
  expected: BlockState[]
  replacement: BlockState[]
}
