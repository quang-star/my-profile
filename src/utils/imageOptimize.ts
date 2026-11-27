/**
 * Optimize a user-selected image on the client before uploading.
 * - Keeps animated GIFs and non-image files (e.g. PDFs) untouched.
 * - Scales oversized images proportionally so max(width, height) <= maxDimension.
 * - Re-encodes to WebP when smaller or scaled down, falling back to the original file on any error.
 */
export async function optimizeImageFile(
  file: File,
  maxDimension = 1600,
  quality = 0.82,
): Promise<File> {
  if (!file.type.startsWith('image/') || file.type === 'image/gif' || file.type === 'image/svg+xml') {
    return file
  }
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return file
  }

  let objectUrl: string | null = null
  try {
    objectUrl = URL.createObjectURL(file)
    const img = await loadImage(objectUrl)
    const width = img.naturalWidth || img.width
    const height = img.naturalHeight || img.height
    if (!width || !height) {
      return file
    }

    const needsResize = width > maxDimension || height > maxDimension
    const scale = needsResize ? Math.min(maxDimension / width, maxDimension / height) : 1
    const targetWidth = Math.max(1, Math.round(width * scale))
    const targetHeight = Math.max(1, Math.round(height * scale))

    const canvas = document.createElement('canvas')
    canvas.width = targetWidth
    canvas.height = targetHeight
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return file
    }
    ctx.drawImage(img, 0, 0, targetWidth, targetHeight)

    const blob = await canvasToBlob(canvas, 'image/webp', quality)
    if (!blob) {
      return file
    }

    if (blob.size < file.size || needsResize) {
      const baseName = file.name.replace(/\.[^.]+$/, '') || 'image'
      return new File([blob], `${baseName}.webp`, {
        type: 'image/webp',
        lastModified: file.lastModified,
      })
    }

    return file
  } catch {
    return file
  } finally {
    if (objectUrl) {
      URL.revokeObjectURL(objectUrl)
    }
  }
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('Failed to decode image'))
    img.src = src
  })
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob | null> {
  return new Promise((resolve) => {
    try {
      canvas.toBlob((blob) => resolve(blob), type, quality)
    } catch {
      resolve(null)
    }
  })
}
