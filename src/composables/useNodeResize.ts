import { onBeforeUnmount, ref, type ShallowRef } from 'vue'
import { useNodes } from '@/composables/useNodes'
import type { ContentNode, NodeAttrs } from '@/types/node'

export const resizeHandles = [
  { edge: 'w', vi: 'Cạnh trái', en: 'Left edge' },
  { edge: 'e', vi: 'Cạnh phải', en: 'Right edge' },
  { edge: 'nw', vi: 'Góc trên trái', en: 'Top left corner' },
  { edge: 'ne', vi: 'Góc trên phải', en: 'Top right corner' },
  { edge: 'sw', vi: 'Góc dưới trái', en: 'Bottom left corner' },
  { edge: 'se', vi: 'Góc dưới phải', en: 'Bottom right corner' },
] as const

type ResizeEdge = typeof resizeHandles[number]['edge']

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}

export function useNodeResize(
  getNode: () => ContentNode,
  shell: Readonly<ShallowRef<HTMLElement | null>>,
  saveAttrs: (attrs: NodeAttrs) => Promise<void>,
) {
  const { resizingNodeId, runVisualChange } = useNodes()
  const resizePreview = ref<NodeAttrs | null>(null)
  let pointerId: number | null = null
  let activeHandle: HTMLElement | null = null

  function measure() {
    const element = shell.value
    const parent = element?.parentElement
    if (!element || !parent) return
    const bounds = element.getBoundingClientRect()
    const parentBounds = parent.getBoundingClientRect()
    const style = getComputedStyle(parent)
    const leftInset = parent.clientLeft + (parseFloat(style.paddingLeft) || 0)
    const rightInset = parseFloat(style.paddingRight) || 0
    const width = Math.max(1, parent.clientWidth - leftInset - rightInset)
    const left = parentBounds.left + leftInset
    const tracks = style.display === 'grid' ? style.gridTemplateColumns.split(' ').length : 0
    const columnGap = parseFloat(style.columnGap) || 0
    const track = (width + columnGap) / Math.max(tracks, 1)
    const first = clamp(Math.round((bounds.left - left) / track), 0, Math.max(tracks - 1, 0))
    const span = clamp(Math.round((bounds.width + columnGap) / track), 1, Math.max(tracks - first, 1))

    let upperLimit = parentBounds.top + parent.clientTop
    for (const sibling of parent.children) {
      if (sibling === element || !(sibling instanceof HTMLElement) || !sibling.dataset.nodeId) continue
      const rect = sibling.getBoundingClientRect()
      if (rect.bottom <= bounds.top) upperLimit = Math.max(upperLimit, rect.bottom)
    }

    const minHeight = element.style.minHeight
    const alignSelf = element.style.alignSelf
    element.style.minHeight = '0'
    if (tracks) element.style.alignSelf = 'start'
    const contentHeight = element.getBoundingClientRect().height
    element.style.minHeight = minHeight
    element.style.alignSelf = alignSelf

    return { bounds, width, left, tracks, track, first, span, upperLimit, contentHeight }
  }

  type Geometry = NonNullable<ReturnType<typeof measure>>

  function resizedAttrs(geometry: Geometry, edge: ResizeEdge, dx: number, dy: number): NodeAttrs {
    const { bounds, width, left, tracks, track, first, span, upperLimit, contentHeight } = geometry
    const attrs = { ...getNode().attrs }
    if (dx !== 0 && tracks > 1) {
      const delta = Math.round(dx / track)
      const end = first + span
      const start = edge.includes('w') ? clamp(first + delta, 0, end - 1) : first
      const newEnd = edge.includes('e') ? clamp(end + delta, start + 1, tracks) : end
      if (start !== first || newEnd !== end) {
        attrs.colStart = start + 1
        attrs.colSpan = newEnd - start
      }
    } else if (dx !== 0 && !tracks) {
      const start = clamp(bounds.left - left, 0, width)
      const end = clamp(bounds.right - left, start, width)
      const minimum = Math.min(width, 28)
      const newStart = edge.includes('w') ? clamp(start + dx, 0, end - minimum) : start
      const newEnd = edge.includes('e') ? clamp(end + dx, newStart + minimum, width) : end
      if (Math.abs(newStart - start) >= 1 || Math.abs(newEnd - end) >= 1) {
        attrs.blockOffset = Math.round(newStart / width * 1000) / 10
        attrs.blockWidth = Math.round((newEnd - newStart) / width * 1000) / 10
      }
    }

    if (dy !== 0 && (edge.includes('n') || edge.includes('s'))) {
      const top = edge.includes('n')
      const minimum = Math.max(24, Math.ceil(contentHeight))
      const maximum = top ? bounds.bottom - upperLimit : Math.max(1200, bounds.height)
      if (maximum >= minimum) {
        const height = Math.round(clamp(bounds.height + (top ? -dy : dy), minimum, maximum))
        if (Math.abs(height - bounds.height) >= 1) {
          attrs.minHeight = height
          if (top) attrs.blockTop = Math.round((attrs.blockTop ?? 0) + bounds.height - height)
        }
      }
    }
    return attrs
  }

  let gesture: { geometry: Geometry; edge: ResizeEdge; x: number; y: number } | null = null

  function startResize(event: PointerEvent, edge: ResizeEdge) {
    if (event.button !== 0 || resizingNodeId.value) return
    const geometry = measure()
    if (!geometry) return
    event.preventDefault()
    activeHandle = event.currentTarget as HTMLElement
    pointerId = event.pointerId
    activeHandle.setPointerCapture(pointerId)
    resizingNodeId.value = getNode().id
    gesture = { geometry, edge, x: event.clientX, y: event.clientY }
  }

  function previewResize(event: PointerEvent) {
    if (!gesture || pointerId !== event.pointerId) return
    const { geometry, edge, x, y } = gesture
    resizePreview.value = resizedAttrs(geometry, edge, event.clientX - x, event.clientY - y)
  }

  function endGesture() {
    if (!gesture) return
    const handle = activeHandle
    const pointer = pointerId
    gesture = null
    pointerId = null
    activeHandle = null
    resizingNodeId.value = null
    if (pointer !== null && handle?.hasPointerCapture(pointer)) handle.releasePointerCapture(pointer)
  }

  function cancelResize() {
    if (!gesture) return
    resizePreview.value = null
    endGesture()
  }

  async function finishResize(event: PointerEvent) {
    if (!gesture || pointerId !== event.pointerId) return
    previewResize(event)
    const attrs = resizePreview.value
    endGesture()
    if (attrs && JSON.stringify(attrs) !== JSON.stringify(getNode().attrs)) {
      await runVisualChange(() => saveAttrs(attrs))
    }
    resizePreview.value = null
  }

  async function resizeWithKeys(event: KeyboardEvent, edge: ResizeEdge) {
    const directions: Record<string, [number, number]> = {
      ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1],
    }
    const direction = directions[event.key]
    if (!direction) return
    event.preventDefault()
    const geometry = measure()
    if (!geometry) return
    const attrs = resizedAttrs(geometry, edge, direction[0] * (geometry.tracks ? geometry.track : 12), direction[1] * 12)
    if (JSON.stringify(attrs) !== JSON.stringify(getNode().attrs)) {
      await runVisualChange(() => saveAttrs(attrs))
    }
  }

  onBeforeUnmount(cancelResize)
  return { resizePreview, startResize, previewResize, finishResize, cancelResize, resizeWithKeys }
}
