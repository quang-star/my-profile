import { computed, ref } from 'vue'
import type { CanvasDevice, NodeAttrs } from '@/types/node'

export const canvasDevices = [
  { id: 'desktop', vi: 'Máy tính', en: 'Desktop', width: 1120 },
  { id: 'tablet', vi: 'Máy tính bảng', en: 'Tablet', width: 820 },
  { id: 'phone', vi: 'Điện thoại', en: 'Phone', width: 390 },
] as const

const devicePreview = ref<CanvasDevice>('desktop')
const viewportWidth = ref(typeof window === 'undefined' ? 1120 : window.innerWidth)
const editorActive = ref(false)
const canvasWidth = computed(() => editorActive.value
  ? canvasDevices.find(({ id }) => id === devicePreview.value)!.width
  : viewportWidth.value)
const canvasDevice = computed<CanvasDevice>(() => {
  if (editorActive.value) return devicePreview.value
  if (viewportWidth.value >= 1024) return 'desktop'
  return viewportWidth.value >= 768 ? 'tablet' : 'phone'
})

function defaultColumns(value: number, width: number) {
  if (value === 12) return width >= 1024 ? 12 : 1
  if (value === 4) return width >= 1024 ? 4 : width >= 640 ? 2 : 1
  return width >= 768 ? value : 1
}

function deviceAttrs(attrs: NodeAttrs, isGrid: boolean, includeOverrides = true): NodeAttrs {
  const { responsive, ...base } = attrs
  if (isGrid) base.cols = defaultColumns(Number(base.cols) || 2, canvasWidth.value)
  return includeOverrides && canvasDevice.value !== 'desktop'
    ? { ...base, ...responsive?.[canvasDevice.value] }
    : base
}

function saveDeviceAttrs(original: NodeAttrs, draft: NodeAttrs, isGrid: boolean): NodeAttrs {
  if (canvasDevice.value === 'desktop') {
    return { ...draft, responsive: original.responsive }
  }
  const device = canvasDevice.value
  const inherited = deviceAttrs(original, isGrid, false)
  const keys = new Set([...Object.keys(draft), ...Object.keys(inherited)])
  const overrides = Object.fromEntries([...keys]
    .filter((key) => draft[key] !== inherited[key])
    .map((key) => [key, draft[key] ?? null]))
  const responsive = { ...original.responsive }
  if (Object.keys(overrides).length) responsive[device] = overrides
  else delete responsive[device]
  const next = { ...original }
  if (Object.keys(responsive).length) next.responsive = responsive
  else delete next.responsive
  return next
}

export function useCanvasLayout() {
  return { devicePreview, viewportWidth, editorActive, canvasWidth, canvasDevice, deviceAttrs, saveDeviceAttrs }
}
