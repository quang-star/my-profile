import type { BlockChanges, ContentNode, NodeReorderItem, SaveNodePayload } from '@/types/node'
import { getStoredToken } from '@/services/api'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

async function nodeRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken()
  const headers = new Headers(options.headers)

  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let errorMessage = `HTTP ${response.status}`
    try {
      const data = (await response.json()) as { error?: string; message?: string }
      errorMessage = data.error || data.message || errorMessage
    } catch {
      // Ignore non-JSON body
    }
    throw new Error(errorMessage)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const nodeApi = {
  getTree: () => nodeRequest<ContentNode[]>('/api/nodes'),

  createNode: (payload: SaveNodePayload) =>
    nodeRequest<ContentNode>('/api/admin/nodes', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateNode: (id: string, payload: SaveNodePayload) =>
    nodeRequest<ContentNode>(`/api/admin/nodes/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deleteNode: (id: string) =>
    nodeRequest<void>(`/api/admin/nodes/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),

  reorderNodes: (items: NodeReorderItem[]) =>
    nodeRequest<ContentNode[]>('/api/admin/nodes/reorder', {
      method: 'PUT',
      body: JSON.stringify({ items }),
    }),

  applyChanges: (changes: BlockChanges) =>
    nodeRequest<ContentNode[]>('/api/admin/nodes/changes', {
      method: 'PUT',
      body: JSON.stringify(changes),
    }),

  resetDefaultTree: () =>
    nodeRequest<ContentNode[]>('/api/admin/nodes/reset', {
      method: 'POST',
    }),
}
