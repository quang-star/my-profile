<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ArrowDown,
  ArrowUp,
  FolderPlus,
  Layers,
  Plus,
  RotateCcw,
  Save,
  Trash2,
  X,
} from '@lucide/vue'
import WordRichEditor from '@/components/nodes/WordRichEditor.vue'
import { useNodes } from '@/composables/useNodes'
import type { ContentNode, NodeAttrs, SaveNodePayload } from '@/types/node'

const {
  nodesTree,
  flatNodes,
  selectedNode,
  selectedNodeId,
  tagEditorOpen,
  createNode,
  updateNode,
  deleteNode,
  moveNodeOrder,
  resetDefaultTree,
} = useNodes()

const TAG_TYPES = [
  { value: 'section', label: 'Section (Mục lớn)' },
  { value: 'grid', label: 'Grid (Lưới cột)' },
  { value: 'card', label: 'Card (Thẻ hộp)' },
  { value: 'heading', label: 'Heading (Tiêu đề)' },
  { value: 'text', label: 'Text (Đoạn văn Word)' },
  { value: 'list', label: 'List (Danh sách)' },
  { value: 'badge-group', label: 'Badge Group (Nhóm nhãn)' },
  { value: 'badge', label: 'Badge (Nhãn / Chip)' },
  { value: 'icon', label: 'Icon (Biểu tượng)' },
  { value: 'metric', label: 'Metric (Chỉ số)' },
  { value: 'link-button', label: 'Link Button (Nút bấm)' },
  { value: 'image', label: 'Image (Hình ảnh)' },
  { value: 'divider', label: 'Divider (Đường kẻ)' },
  { value: 'contact-form', label: 'Contact Form' },
]

const COLOR_PRESETS = ['#ffffff', '#f8fafc', '#0f172a', '#ecfdf5', '#eef2ff', '#fffbeb', '#f0f9ff', '#fff1f2']

const langTab = ref<'vi' | 'en'>('vi')
const saving = ref(false)
const statusMsg = ref('')
const errorMsg = ref('')

// Local draft state for the currently selected node
const draft = reactive<{
  id: string
  parentId: string | null
  tag: string
  sortOrder: number
  contentVi: string
  contentEn: string
  attrs: NodeAttrs
}>({
  id: '',
  parentId: null,
  tag: 'text',
  sortOrder: 0,
  contentVi: '',
  contentEn: '',
  attrs: {},
})

watch(
  selectedNode,
  (node) => {
    if (!node) return
    draft.id = node.id
    draft.parentId = node.parentId
    draft.tag = node.tag
    draft.sortOrder = node.sortOrder
    draft.contentVi = node.contentVi || ''
    draft.contentEn = node.contentEn || ''
    draft.attrs = { ...(node.attrs || {}) }
    statusMsg.value = ''
    errorMsg.value = ''
  },
  { immediate: true }
)

interface FlatTreeRow {
  node: ContentNode
  depth: number
}

const treeRows = computed<FlatTreeRow[]>(() => {
  const rows: FlatTreeRow[] = []
  function walk(list: ContentNode[], depth: number) {
    for (const item of list) {
      rows.push({ node: item, depth })
      if (item.children?.length) {
        walk(item.children, depth + 1)
      }
    }
  }
  walk(nodesTree.value, 0)
  return rows
})

const isContainerTag = computed(() =>
  ['section', 'grid', 'card', 'badge-group'].includes(draft.tag)
)

const parentCandidates = computed(() =>
  flatNodes.value.filter((n) => n.id !== draft.id)
)

async function handleSave() {
  if (!draft.id) return
  saving.value = true
  statusMsg.value = ''
  errorMsg.value = ''
  try {
    const payload: SaveNodePayload = {
      id: draft.id,
      parentId: draft.parentId || null,
      tag: draft.tag,
      sortOrder: draft.sortOrder,
      attrs: { ...draft.attrs },
      contentVi: draft.contentVi,
      contentEn: draft.contentEn,
    }
    await updateNode(draft.id, payload)
    statusMsg.value = 'Đã lưu thẻ!'
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : 'Lỗi khi lưu'
  } finally {
    saving.value = false
  }
}

async function handleAddChild(parentId: string | null, defaultTag = 'text') {
  saving.value = true
  errorMsg.value = ''
  try {
    await createNode({
      parentId,
      tag: defaultTag,
      attrs:
        defaultTag === 'card'
          ? { bgColor: '#ffffff', borderColor: '#e2e8f0', rounded: '2xl', padding: 'md' }
          : defaultTag === 'badge'
            ? { bgColor: '#ecfdf5', textColor: '#047857', borderColor: '#a7f3d0' }
            : defaultTag === 'grid'
              ? { cols: 2, gap: 4 }
              : {},
      contentVi: defaultTag === 'heading' ? 'Tiêu đề mới' : defaultTag === 'badge' ? 'Nhãn mới' : '<p>Nội dung mới...</p>',
      contentEn: defaultTag === 'heading' ? 'New Heading' : defaultTag === 'badge' ? 'New Badge' : '<p>New content...</p>',
    })
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : 'Không thể tạo thẻ'
  } finally {
    saving.value = false
  }
}

async function handleDelete(node: ContentNode) {
  if (!window.confirm(`Xóa thẻ "${node.id}" (${node.tag}) và toàn bộ thẻ con bên trong?`)) return
  saving.value = true
  errorMsg.value = ''
  try {
    await deleteNode(node.id)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : 'Không thể xóa'
  } finally {
    saving.value = false
  }
}

async function handleReset() {
  if (!window.confirm('Khôi phục toàn bộ cây thẻ về dữ liệu mẫu ban đầu?')) return
  saving.value = true
  errorMsg.value = ''
  try {
    await resetDefaultTree()
    statusMsg.value = 'Đã khôi phục mẫu!'
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : 'Lỗi khôi phục'
  } finally {
    saving.value = false
  }
}

function getPreviewLabel(node: ContentNode): string {
  const raw = (node.contentVi || node.contentEn || '').replace(/<[^>]+>/g, '').trim()
  if (raw) return raw.slice(0, 26) + (raw.length > 26 ? '…' : '')
  return node.id
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="tagEditorOpen"
      class="no-print fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-2 backdrop-blur-sm sm:p-4"
    >
      <div class="flex h-[92vh] w-full max-w-7xl flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <!-- Top Bar -->
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-slate-900 px-4 py-3 text-white">
          <div class="flex items-center gap-2.5">
            <Layers class="h-5 w-5 text-emerald-400" />
            <div>
              <h2 class="text-sm font-bold">Trình soạn thảo Thẻ &amp; Nội dung (Word + Tag Tree)</h2>
              <p class="text-[11px] text-slate-400">
                Chọn thẻ bên trái → Soạn chữ/màu/thẻ lồng ở giữa → Chỉnh thuộc tính cha &amp; style bên phải
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-xl border border-slate-700 px-3 py-1.5 text-xs font-semibold text-slate-300 hover:bg-slate-800 hover:text-white"
              :disabled="saving"
              @click="handleReset"
            >
              <RotateCcw class="h-3.5 w-3.5" />
              Khôi phục mẫu
            </button>
            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-xl bg-emerald-500 px-4 py-1.5 text-xs font-bold text-slate-950 hover:bg-emerald-400 disabled:opacity-50"
              :disabled="saving || !draft.id"
              @click="handleSave"
            >
              <Save class="h-3.5 w-3.5" />
              {{ saving ? 'Đang lưu...' : 'Lưu thẻ' }}
            </button>
            <button
              type="button"
              class="rounded-xl p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
              @click="tagEditorOpen = false"
            >
              <X class="h-5 w-5" />
            </button>
          </div>
        </div>

        <!-- 3-Column Workspace -->
        <div class="grid min-h-0 flex-1 grid-cols-1 lg:grid-cols-12">
          <!-- LEFT: Tag Tree -->
          <div class="flex min-h-0 flex-col border-b border-slate-200 bg-slate-50 lg:col-span-3 lg:border-b-0 lg:border-r">
            <div class="flex items-center justify-between border-b border-slate-200 px-3 py-2.5">
              <span class="text-xs font-bold uppercase tracking-wider text-slate-600">Cây thẻ ({{ treeRows.length }})</span>
              <button
                type="button"
                class="inline-flex items-center gap-1 rounded-lg bg-slate-900 px-2.5 py-1 text-[11px] font-semibold text-white hover:bg-emerald-600"
                @click="handleAddChild(null, 'section')"
              >
                <FolderPlus class="h-3 w-3" />
                + Section
              </button>
            </div>

            <div class="flex-1 space-y-0.5 overflow-y-auto p-2">
              <div
                v-for="{ node, depth } in treeRows"
                :key="node.id"
                class="group flex items-center gap-1 rounded-xl px-2 py-1.5 text-xs transition-colors"
                :class="
                  selectedNodeId === node.id
                    ? 'bg-emerald-100/90 font-semibold text-emerald-950 ring-1 ring-emerald-300'
                    : 'text-slate-700 hover:bg-slate-200/70'
                "
                :style="{ paddingLeft: `${depth * 14 + 8}px` }"
                @click="selectedNodeId = node.id"
              >
                <span class="shrink-0 rounded bg-slate-200/80 px-1.5 py-0.5 font-mono text-[10px] text-slate-700">
                  {{ node.tag }}
                </span>
                <span class="min-w-0 flex-1 truncate">{{ getPreviewLabel(node) }}</span>

                <div class="hidden shrink-0 items-center gap-0.5 group-hover:flex">
                  <button
                    type="button"
                    class="rounded p-0.5 hover:bg-white"
                    title="Thêm thẻ con vào thẻ này"
                    @click.stop="handleAddChild(node.id, node.tag === 'section' ? 'grid' : node.tag === 'grid' ? 'card' : 'text')"
                  >
                    <Plus class="h-3 w-3 text-emerald-600" />
                  </button>
                  <button type="button" class="rounded p-0.5 hover:bg-white" title="Lên" @click.stop="moveNodeOrder(node, -1)">
                    <ArrowUp class="h-3 w-3" />
                  </button>
                  <button type="button" class="rounded p-0.5 hover:bg-white" title="Xuống" @click.stop="moveNodeOrder(node, 1)">
                    <ArrowDown class="h-3 w-3" />
                  </button>
                  <button type="button" class="rounded p-0.5 hover:bg-rose-100" title="Xóa" @click.stop="handleDelete(node)">
                    <Trash2 class="h-3 w-3 text-rose-600" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- CENTER: Word-like Content Editor -->
          <div class="flex min-h-0 flex-col overflow-y-auto p-4 lg:col-span-5">
            <div v-if="selectedNode" class="space-y-4">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <span class="rounded-lg bg-slate-100 px-2.5 py-1 font-mono text-xs font-bold text-slate-700">
                    #{{ draft.id }}
                  </span>
                  <span v-if="statusMsg" class="text-xs font-semibold text-emerald-600">{{ statusMsg }}</span>
                  <span v-if="errorMsg" class="text-xs font-semibold text-rose-600">{{ errorMsg }}</span>
                </div>

                <!-- Language Switch -->
                <div class="inline-flex rounded-xl border border-slate-200 bg-slate-100 p-0.5 text-xs">
                  <button
                    type="button"
                    class="rounded-lg px-3 py-1 font-semibold transition-colors"
                    :class="langTab === 'vi' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600'"
                    @click="langTab = 'vi'"
                  >
                    Tiếng Việt
                  </button>
                  <button
                    type="button"
                    class="rounded-lg px-3 py-1 font-semibold transition-colors"
                    :class="langTab === 'en' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600'"
                    @click="langTab = 'en'"
                  >
                    English
                  </button>
                </div>
              </div>

              <!-- Quick add child for containers -->
              <div
                v-if="isContainerTag"
                class="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4 text-xs text-slate-600"
              >
                <p class="font-semibold text-slate-800">
                  Thẻ <code>{{ draft.tag }}</code> là thẻ chứa (Container).
                </p>
                <p class="mt-1">Bạn có thể thêm nhanh các thẻ con bên trong thẻ này:</p>
                <div class="mt-3 flex flex-wrap gap-2">
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'grid')">+ Grid</button>
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'card')">+ Card</button>
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'heading')">+ Heading</button>
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'text')">+ Đoạn văn Word</button>
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'badge-group')">+ Nhóm Badge</button>
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'badge')">+ Badge</button>
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'metric')">+ Metric</button>
                  <button type="button" class="quick-chip" @click="handleAddChild(draft.id, 'link-button')">+ Nút Link</button>
                </div>
              </div>

              <!-- Word Rich Editor -->
              <div v-else class="space-y-2">
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-500">
                  Nội dung ({{ langTab === 'vi' ? 'Tiếng Việt' : 'English' }}) — Hỗ trợ tô màu &amp; lồng thẻ
                </label>
                <WordRichEditor
                  v-if="langTab === 'vi'"
                  v-model="draft.contentVi"
                  placeholder="Gõ nội dung như Word, bôi đen để đổi màu hoặc bấm '+ Thẻ trong chữ'..."
                />
                <WordRichEditor
                  v-else
                  v-model="draft.contentEn"
                  placeholder="Type content like Word, highlight to colorize or insert inline tags..."
                />
              </div>
            </div>
          </div>

          <!-- RIGHT: Node Attributes Inspector -->
          <div class="min-h-0 overflow-y-auto border-t border-slate-200 bg-slate-50/70 p-4 lg:col-span-4 lg:border-l lg:border-t-0">
            <h3 class="mb-3 text-xs font-bold uppercase tracking-wider text-slate-600">
              Thuộc tính thẻ &amp; Màu sắc
            </h3>

            <div v-if="selectedNode" class="space-y-3.5 text-xs">
              <!-- Tag Type & Parent -->
              <div class="grid grid-cols-2 gap-2.5">
                <div>
                  <label class="attr-label">Loại thẻ (tag)</label>
                  <select v-model="draft.tag" class="attr-input">
                    <option v-for="t in TAG_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
                  </select>
                </div>
                <div>
                  <label class="attr-label">Thẻ cha (parent_id)</label>
                  <select v-model="draft.parentId" class="attr-input">
                    <option :value="null">(Root — Cấp cao nhất)</option>
                    <option v-for="p in parentCandidates" :key="p.id" :value="p.id">
                      {{ p.tag }} · {{ p.id }}
                    </option>
                  </select>
                </div>
              </div>

              <!-- Colors -->
              <div class="rounded-2xl border border-slate-200 bg-white p-3 space-y-2.5">
                <div class="font-bold text-slate-700">Màu sắc</div>
                <div class="grid grid-cols-3 gap-2">
                  <div>
                    <label class="attr-label">Màu nền</label>
                    <div class="flex items-center gap-1.5">
                      <input v-model="draft.attrs.bgColor" type="color" class="h-7 w-7 cursor-pointer rounded border" />
                      <input v-model="draft.attrs.bgColor" type="text" placeholder="#ffffff" class="attr-input" />
                    </div>
                  </div>
                  <div>
                    <label class="attr-label">Màu chữ</label>
                    <div class="flex items-center gap-1.5">
                      <input v-model="draft.attrs.textColor" type="color" class="h-7 w-7 cursor-pointer rounded border" />
                      <input v-model="draft.attrs.textColor" type="text" placeholder="#0f172a" class="attr-input" />
                    </div>
                  </div>
                  <div>
                    <label class="attr-label">Màu viền</label>
                    <div class="flex items-center gap-1.5">
                      <input v-model="draft.attrs.borderColor" type="color" class="h-7 w-7 cursor-pointer rounded border" />
                      <input v-model="draft.attrs.borderColor" type="text" placeholder="#e2e8f0" class="attr-input" />
                    </div>
                  </div>
                </div>

                <!-- Quick Background Presets -->
                <div class="flex flex-wrap items-center gap-1.5 pt-1">
                  <span class="text-[11px] text-slate-500">Nền nhanh:</span>
                  <button
                    v-for="c in COLOR_PRESETS"
                    :key="c"
                    type="button"
                    class="h-5 w-5 rounded-full border border-slate-300"
                    :style="{ backgroundColor: c }"
                    @click="draft.attrs.bgColor = c"
                  />
                  <button
                    type="button"
                    class="rounded border border-slate-200 px-1.5 py-0.5 text-[10px] text-slate-600 hover:bg-slate-100"
                    @click="delete draft.attrs.bgColor; delete draft.attrs.textColor; delete draft.attrs.borderColor"
                  >
                    Reset màu
                  </button>
                </div>
              </div>

              <!-- Layout Attributes -->
              <div class="rounded-2xl border border-slate-200 bg-white p-3 space-y-2.5">
                <div class="font-bold text-slate-700">Bố cục &amp; Kích thước</div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="attr-label">Số cột (cho Grid)</label>
                    <select v-model.number="draft.attrs.cols" class="attr-input">
                      <option :value="undefined">Mặc định</option>
                      <option :value="1">1 cột</option>
                      <option :value="2">2 cột</option>
                      <option :value="3">3 cột</option>
                      <option :value="4">4 cột</option>
                      <option :value="12">12 cột (Bento)</option>
                    </select>
                  </div>
                  <div>
                    <label class="attr-label">Chiếm cột (colSpan)</label>
                    <select v-model.number="draft.attrs.colSpan" class="attr-input">
                      <option :value="undefined">Tự động</option>
                      <option :value="4">4 / 12</option>
                      <option :value="6">6 / 12</option>
                      <option :value="8">8 / 12</option>
                      <option :value="12">12 / 12 (Full)</option>
                    </select>
                  </div>
                  <div>
                    <label class="attr-label">Cỡ chữ (fontSize)</label>
                    <select v-model="draft.attrs.fontSize" class="attr-input">
                      <option value="">Mặc định</option>
                      <option value="xs">xs</option>
                      <option value="sm">sm</option>
                      <option value="base">base</option>
                      <option value="lg">lg</option>
                      <option value="xl">xl</option>
                      <option value="2xl">2xl</option>
                      <option value="3xl">3xl</option>
                      <option value="4xl">4xl</option>
                    </select>
                  </div>
                  <div>
                    <label class="attr-label">Bo góc (rounded)</label>
                    <select v-model="draft.attrs.rounded" class="attr-input">
                      <option value="">Mặc định</option>
                      <option value="none">Vuông</option>
                      <option value="lg">lg</option>
                      <option value="xl">xl</option>
                      <option value="2xl">2xl</option>
                      <option value="3xl">3xl</option>
                    </select>
                  </div>
                </div>
              </div>

              <!-- Tag-specific attrs -->
              <div class="rounded-2xl border border-slate-200 bg-white p-3 space-y-2.5">
                <div class="font-bold text-slate-700">Thuộc tính riêng</div>
                <div v-if="draft.tag === 'heading'">
                  <label class="attr-label">Cấp tiêu đề</label>
                  <select v-model="draft.attrs.level" class="attr-input">
                    <option value="h1">H1</option>
                    <option value="h2">H2</option>
                    <option value="h3">H3</option>
                    <option value="h4">H4</option>
                  </select>
                </div>
                <div v-if="draft.tag === 'metric'" class="grid grid-cols-2 gap-2">
                  <div>
                    <label class="attr-label">Giá trị số (value)</label>
                    <input v-model="draft.attrs.value" type="text" placeholder="6+" class="attr-input" />
                  </div>
                  <div>
                    <label class="attr-label">Màu số</label>
                    <input v-model="draft.attrs.valueColor" type="color" class="h-8 w-full cursor-pointer rounded border" />
                  </div>
                </div>
                <div v-if="draft.tag === 'link-button'" class="space-y-2">
                  <div>
                    <label class="attr-label">Đường dẫn (href)</label>
                    <input v-model="draft.attrs.href" type="text" placeholder="https://..." class="attr-input" />
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    <div>
                      <label class="attr-label">Icon</label>
                      <select v-model="draft.attrs.icon" class="attr-input">
                        <option value="">Mặc định</option>
                        <option value="github">GitHub</option>
                        <option value="mail">Mail</option>
                        <option value="sparkles">Sparkles</option>
                      </select>
                    </div>
                    <div>
                      <label class="attr-label">Mở tab</label>
                      <select v-model="draft.attrs.target" class="attr-input">
                        <option value="_self">Cùng tab</option>
                        <option value="_blank">Tab mới</option>
                      </select>
                    </div>
                  </div>
                </div>
                <div v-if="draft.tag === 'image'">
                  <label class="attr-label">URL Ảnh (src)</label>
                  <input v-model="draft.attrs.src" type="text" placeholder="https://... hoặc /api/files/..." class="attr-input" />
                </div>
                <div v-if="draft.tag === 'section'">
                  <label class="attr-label">Anchor ID (vd: about, skills, contact)</label>
                  <input v-model="draft.attrs.anchor" type="text" placeholder="about" class="attr-input" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.attr-label {
  display: block;
  margin-bottom: 0.25rem;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}
.attr-input {
  width: 100%;
  border-radius: 0.625rem;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  padding: 0.375rem 0.5rem;
  font-size: 12px;
  color: #0f172a;
}
.attr-input:focus {
  outline: none;
  border-color: #10b981;
}
.quick-chip {
  border-radius: 0.625rem;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  padding: 0.35rem 0.65rem;
  font-size: 11px;
  font-weight: 600;
  color: #334155;
}
.quick-chip:hover {
  border-color: #10b981;
  background-color: #ecfdf5;
  color: #047857;
}
</style>
