import { toCanvas } from 'html-to-image'
import { jsPDF } from 'jspdf'

export interface PdfExportOptions {
  element: HTMLElement
  fileName: string
  backgroundColor?: string
}

const A4_WIDTH_MM = 210
const A4_HEIGHT_MM = 297

function sanitizePdfFilename(raw: string): string {
  const base = raw
    .trim()
    .replace(/\.pdf$/i, '')
    .replace(/[^a-zA-Z0-9._-]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return `${base || 'cv-export'}.pdf`
}

function nextAnimationFrame(): Promise<void> {
  return new Promise((resolve) => requestAnimationFrame(() => resolve()))
}

function parseColorRgb(raw: string): [number, number, number] {
  const clean = raw.trim().toLowerCase()
  if (/^#[0-9a-f]{6}$/i.test(clean)) {
    return [
      parseInt(clean.slice(1, 3), 16),
      parseInt(clean.slice(3, 5), 16),
      parseInt(clean.slice(5, 7), 16),
    ]
  }
  if (/^#[0-9a-f]{3}$/i.test(clean)) {
    return [
      parseInt(clean[1]! + clean[1]!, 16),
      parseInt(clean[2]! + clean[2]!, 16),
      parseInt(clean[3]! + clean[3]!, 16),
    ]
  }
  const rgbMatch = clean.match(/^rgba?\(\s*(\d+)\s*[,\s]\s*(\d+)\s*[,\s]\s*(\d+)/)
  if (rgbMatch) {
    return [
      Math.min(255, Math.max(0, Number(rgbMatch[1]))),
      Math.min(255, Math.max(0, Number(rgbMatch[2]))),
      Math.min(255, Math.max(0, Number(rgbMatch[3]))),
    ]
  }
  return [255, 255, 255]
}

/**
 * Scans upward from targetY to find a horizontal whitespace band so multi-page PDF breaks
 * do not slice across a line of text.
 */
function findSafeBreakY(
  canvas: HTMLCanvasElement,
  targetY: number,
  bgRgb: [number, number, number],
  searchUpPx = 160,
): number {
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  if (!ctx) return targetY

  const minY = Math.max(10, targetY - searchUpPx)
  const maxY = Math.min(canvas.height - 1, targetY)
  if (maxY <= minY) return targetY

  const stripHeight = maxY - minY + 1
  const imgData = ctx.getImageData(0, minY, canvas.width, stripHeight).data
  const stepX = 4

  for (let y = maxY; y >= minY; y--) {
    const rowOffset = (y - minY) * canvas.width * 4
    let sampled = 0
    let cleanPixels = 0

    for (let x = 0; x < canvas.width; x += stepX) {
      const idx = rowOffset + x * 4
      const r = imgData[idx] ?? 255
      const g = imgData[idx + 1] ?? 255
      const b = imgData[idx + 2] ?? 255
      const diff = Math.abs(r - bgRgb[0]) + Math.abs(g - bgRgb[1]) + Math.abs(b - bgRgb[2])
      sampled++
      if (diff <= 30) {
        cleanPixels++
      }
    }

    if (sampled > 0 && cleanPixels / sampled >= 0.99) {
      return y
    }
  }

  return targetY
}

interface CapturedLinkRect {
  x: number
  y: number
  w: number
  h: number
  url: string
}

function collectSafeLinkRects(element: HTMLElement): { sheetWidth: number; sheetHeight: number; links: CapturedLinkRect[] } {
  const sheetRect = element.getBoundingClientRect()
  const anchors = element.querySelectorAll<HTMLAnchorElement>('a[href]')
  const links: CapturedLinkRect[] = []

  anchors.forEach((a) => {
    const rawHref = (a.getAttribute('href') || '').trim()
    if (!/^(https?:\/\/|mailto:|tel:)/i.test(rawHref)) return
    const rects = a.getClientRects()
    for (let i = 0; i < rects.length; i++) {
      const r = rects[i]!
      if (r.width <= 1 || r.height <= 1) continue
      links.push({
        x: r.left - sheetRect.left,
        y: r.top - sheetRect.top,
        w: r.width,
        h: r.height,
        url: rawHref,
      })
    }
  })

  return {
    sheetWidth: Math.max(1, sheetRect.width),
    sheetHeight: Math.max(1, sheetRect.height),
    links,
  }
}

export async function exportElementToPdf(options: PdfExportOptions): Promise<void> {
  const { element, fileName, backgroundColor } = options
  const bg = backgroundColor && backgroundColor !== 'transparent' ? backgroundColor : '#ffffff'
  const bgRgb = parseColorRgb(bg)

  if (document.activeElement instanceof HTMLElement) {
    document.activeElement.blur()
  }

  const zoomWrap = element.closest('.cv-sheet-zoom-wrap') as HTMLElement | null
  const prevZoom = zoomWrap ? zoomWrap.style.zoom : ''

  element.classList.add('is-pdf-exporting')
  if (zoomWrap) {
    zoomWrap.style.zoom = '1'
  }

  try {
    await nextAnimationFrame()
    await nextAnimationFrame()

    const captured = collectSafeLinkRects(element)

    const renderOpts = {
      pixelRatio: 2,
      backgroundColor: bg,
      cacheBust: true,
      filter: (node: HTMLElement) => !(node instanceof HTMLElement && node.classList.contains('no-print')),
      style: {
        boxShadow: 'none',
        borderRadius: '0px',
        border: 'none',
      },
    }

    let canvas: HTMLCanvasElement
    try {
      canvas = await toCanvas(element, renderOpts)
    } catch {
      canvas = await toCanvas(element, { ...renderOpts, skipFonts: true })
    }

    const scaleX = canvas.width / captured.sheetWidth
    const scaleY = canvas.height / captured.sheetHeight
    const canvasLinks: CapturedLinkRect[] = captured.links.map((l) => ({
      x: l.x * scaleX,
      y: l.y * scaleY,
      w: l.w * scaleX,
      h: l.h * scaleY,
      url: l.url,
    }))

    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
      compress: true,
    })

    const pxPerMm = canvas.width / A4_WIDTH_MM
    const totalHeightMm = canvas.height / pxPerMm

    if (totalHeightMm <= A4_HEIGHT_MM + 4) {
      const targetHeightMm = Math.min(A4_HEIGHT_MM, totalHeightMm)
      const targetWidthMm = totalHeightMm > A4_HEIGHT_MM
        ? (A4_WIDTH_MM * A4_HEIGHT_MM) / totalHeightMm
        : A4_WIDTH_MM
      const offsetX = (A4_WIDTH_MM - targetWidthMm) / 2
      pdf.addImage(
        canvas.toDataURL('image/jpeg', 0.95),
        'JPEG',
        offsetX,
        0,
        targetWidthMm,
        targetHeightMm,
        undefined,
        'FAST',
      )
      canvasLinks.forEach((link) => {
        const xMm = offsetX + (link.x / canvas.width) * targetWidthMm
        const yMm = (link.y / canvas.height) * targetHeightMm
        const wMm = (link.w / canvas.width) * targetWidthMm
        const hMm = (link.h / canvas.height) * targetHeightMm
        if (wMm > 0.5 && hMm > 0.5) {
          pdf.link(xMm, yMm, wMm, hMm, { url: link.url })
        }
      })
    } else {
      let yPx = 0
      let pageIndex = 0

      while (yPx < canvas.height) {
        const remainingPx = canvas.height - yPx
        if (pageIndex > 0 && remainingPx < 14) break

        const topMarginMm = pageIndex > 0 ? 6 : 0
        const maxContentHeightMm = A4_HEIGHT_MM - topMarginMm - 6
        const maxSlicePx = Math.max(80, Math.floor(maxContentHeightMm * pxPerMm))

        let endY = Math.min(canvas.height, yPx + maxSlicePx)
        if (endY < canvas.height) {
          const safeY = findSafeBreakY(canvas, endY, bgRgb, 160)
          if (safeY > yPx + 60) {
            endY = safeY
          }
        }

        const sliceHeightPx = Math.max(1, endY - yPx)
        const sliceCanvas = document.createElement('canvas')
        sliceCanvas.width = canvas.width
        sliceCanvas.height = sliceHeightPx

        const ctx = sliceCanvas.getContext('2d')
        if (ctx) {
          ctx.fillStyle = bg
          ctx.fillRect(0, 0, sliceCanvas.width, sliceCanvas.height)
          ctx.drawImage(
            canvas,
            0,
            yPx,
            canvas.width,
            sliceHeightPx,
            0,
            0,
            canvas.width,
            sliceHeightPx,
          )
        }

        if (pageIndex > 0) {
          pdf.addPage('a4', 'portrait')
        }
        pdf.setFillColor(bgRgb[0], bgRgb[1], bgRgb[2])
        pdf.rect(0, 0, A4_WIDTH_MM, A4_HEIGHT_MM, 'F')

        const sliceHeightMm = sliceHeightPx / pxPerMm
        pdf.addImage(
          sliceCanvas.toDataURL('image/jpeg', 0.95),
          'JPEG',
          0,
          topMarginMm,
          A4_WIDTH_MM,
          sliceHeightMm,
          undefined,
          'FAST',
        )

        canvasLinks.forEach((link) => {
          if (link.y + link.h <= yPx || link.y >= endY) return
          const topPx = Math.max(yPx, link.y)
          const bottomPx = Math.min(endY, link.y + link.h)
          if (bottomPx - topPx <= 2) return
          pdf.link(
            link.x / pxPerMm,
            topMarginMm + (topPx - yPx) / pxPerMm,
            link.w / pxPerMm,
            (bottomPx - topPx) / pxPerMm,
            { url: link.url },
          )
        })

        yPx = endY
        pageIndex++
      }
    }

    pdf.save(sanitizePdfFilename(fileName))
  } finally {
    element.classList.remove('is-pdf-exporting')
    if (zoomWrap) {
      zoomWrap.style.zoom = prevZoom
    }
  }
}
