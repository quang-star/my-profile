<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import {
  Bold,
  Code,
  Eraser,
  Highlighter,
  Italic,
  Link2,
  List,
  ListOrdered,
  Palette,
  Plus,
  Strikethrough,
  Tag,
  Underline,
} from '@lucide/vue'

const props = defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorRef = ref<HTMLDivElement | null>(null)
const showSource = ref(false)
const showInlineTagPopover = ref(false)
const inlineTagLabel = ref('')
const inlineTagColor = ref<'emerald' | 'indigo' | 'amber' | 'sky' | 'rose' | 'slate'>('emerald')
const textColor = ref('#0f172a')
const highlightColor = ref('#fef08a')

const TAG_COLOR_OPTIONS = [
  { id: 'emerald', label: 'Xanh lá', dot: '#10b981' },
  { id: 'indigo', label: 'Chàm', dot: '#6366f1' },
  { id: 'amber', label: 'Vàng cam', dot: '#f59e0b' },
  { id: 'sky', label: 'Xanh dương', dot: '#0ea5e9' },
  { id: 'rose', label: 'Hồng đỏ', dot: '#f43f5e' },
  { id: 'slate', label: 'Xám', dot: '#64748b' },
] as const

let savedRange: Range | null = null

function saveSelection() {
  const sel = window.getSelection()
  if (sel && sel.rangeCount > 0 && editorRef.value?.contains(sel.anchorNode)) {
    savedRange = sel.getRangeAt(0).cloneRange()
  }
}

function restoreSelection() {
  if (!savedRange || !editorRef.value) return
  editorRef.value.focus()
  const sel = window.getSelection()
  if (sel) {
    sel.removeAllRanges()
    sel.addRange(savedRange)
  }
}

function syncFromDom() {
  if (!editorRef.value) return
  emit('update:modelValue', editorRef.value.innerHTML)
}

function exec(command: string, value?: string) {
  restoreSelection()
  document.execCommand(command, false, value)
  syncFromDom()
  saveSelection()
}

function applyTextColor(color: string) {
  textColor.value = color
  exec('foreColor', color)
}

function applyHighlight(color: string) {
  highlightColor.value = color
  exec('hiliteColor', color)
}

function insertLink() {
  saveSelection()
  const url = window.prompt('Nhập URL liên kết (https://... hoặc #contact):', 'https://')
  if (!url) return
  restoreSelection()
  const sel = window.getSelection()
  if (sel && sel.isCollapsed) {
    exec('insertHTML', `<a href="${escapeAttr(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(url)}</a>`)
  } else {
    exec('createLink', url)
  }
}

function insertCodeSpan() {
  restoreSelection()
  const sel = window.getSelection()
  const selected = sel?.toString().trim() || 'code'
  exec('insertHTML', `<code>${escapeHtml(selected)}</code>&nbsp;`)
}

function openInlineTagDialog() {
  saveSelection()
  const sel = window.getSelection()
  inlineTagLabel.value = sel?.toString().trim() || ''
  showInlineTagPopover.value = !showInlineTagPopover.value
}

function insertInlineTag() {
  const label = inlineTagLabel.value.trim()
  if (!label) return
  restoreSelection()
  const html = `<span class="inline-tag" data-color="${inlineTagColor.value}">${escapeHtml(label)}</span>&nbsp;`
  document.execCommand('insertHTML', false, html)
  syncFromDom()
  inlineTagLabel.value = ''
  showInlineTagPopover.value = false
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function escapeAttr(text: string) {
  return escapeHtml(text).replace(/'/g, '&#39;')
}

watch(
  () => props.modelValue,
  async (val) => {
    if (showSource.value) return
    await nextTick()
    if (editorRef.value && editorRef.value.innerHTML !== (val || '')) {
      editorRef.value.innerHTML = val || ''
    }
  }
)

watch(showSource, async (isSrc) => {
  if (!isSrc) {
    await nextTick()
    if (editorRef.value) {
      editorRef.value.innerHTML = props.modelValue || ''
    }
  }
})

onMounted(() => {
  if (editorRef.value) {
    editorRef.value.innerHTML = props.modelValue || ''
  }
})
</script>

<template>
  <div class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <!-- Word-like Toolbar -->
    <div class="flex flex-wrap items-center gap-1 border-b border-slate-200 bg-slate-50 px-2.5 py-2">
      <button type="button" class="tool-btn" title="In đậm (Ctrl+B)" @mousedown.prevent @click="exec('bold')">
        <Bold class="h-3.5 w-3.5" />
      </button>
      <button type="button" class="tool-btn" title="In nghiêng (Ctrl+I)" @mousedown.prevent @click="exec('italic')">
        <Italic class="h-3.5 w-3.5" />
      </button>
      <button type="button" class="tool-btn" title="Gạch chân (Ctrl+U)" @mousedown.prevent @click="exec('underline')">
        <Underline class="h-3.5 w-3.5" />
      </button>
      <button type="button" class="tool-btn" title="Gạch ngang" @mousedown.prevent @click="exec('strikeThrough')">
        <Strikethrough class="h-3.5 w-3.5" />
      </button>
      <button type="button" class="tool-btn" title="Code inline" @mousedown.prevent @click="insertCodeSpan">
        <Code class="h-3.5 w-3.5" />
      </button>

      <span class="mx-1 h-4 w-px bg-slate-300" />

      <button type="button" class="tool-btn" title="Danh sách chấm" @mousedown.prevent @click="exec('insertUnorderedList')">
        <List class="h-3.5 w-3.5" />
      </button>
      <button type="button" class="tool-btn" title="Danh sách số" @mousedown.prevent @click="exec('insertOrderedList')">
        <ListOrdered class="h-3.5 w-3.5" />
      </button>

      <span class="mx-1 h-4 w-px bg-slate-300" />

      <!-- Text Color -->
      <label class="tool-btn cursor-pointer gap-1 px-1.5" title="Màu chữ" @mousedown="saveSelection">
        <Palette class="h-3.5 w-3.5" />
        <input
          type="color"
          :value="textColor"
          class="h-4 w-4 cursor-pointer border-0 bg-transparent p-0"
          @input="applyTextColor(($event.target as HTMLInputElement).value)"
        />
      </label>

      <!-- Highlight Color -->
      <label class="tool-btn cursor-pointer gap-1 px-1.5" title="Tô nền chữ" @mousedown="saveSelection">
        <Highlighter class="h-3.5 w-3.5" />
        <input
          type="color"
          :value="highlightColor"
          class="h-4 w-4 cursor-pointer border-0 bg-transparent p-0"
          @input="applyHighlight(($event.target as HTMLInputElement).value)"
        />
      </label>

      <button type="button" class="tool-btn" title="Chèn link" @mousedown.prevent @click="insertLink">
        <Link2 class="h-3.5 w-3.5" />
      </button>

      <!-- Inline Tag / Badge inside text -->
      <button
        type="button"
        class="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-100"
        title="Chèn thẻ tag/badge lồng trong văn bản"
        @mousedown.prevent
        @click="openInlineTagDialog"
      >
        <Tag class="h-3 w-3" />
        <span>+ Thẻ trong chữ</span>
      </button>

      <button type="button" class="tool-btn" title="Xóa định dạng" @mousedown.prevent @click="exec('removeFormat')">
        <Eraser class="h-3.5 w-3.5" />
      </button>

      <div class="ml-auto">
        <button
          type="button"
          class="rounded-lg px-2 py-1 font-mono text-[11px] font-semibold"
          :class="showSource ? 'bg-slate-800 text-white' : 'bg-slate-200/70 text-slate-700 hover:bg-slate-200'"
          @click="showSource = !showSource"
        >
          {{ showSource ? 'Word View' : 'HTML' }}
        </button>
      </div>
    </div>

    <!-- Inline Tag Builder Popover -->
    <div
      v-if="showInlineTagPopover"
      class="flex flex-wrap items-center gap-2 border-b border-emerald-200 bg-emerald-50/70 px-3 py-2 text-xs"
    >
      <input
        v-model="inlineTagLabel"
        type="text"
        placeholder="Nội dung thẻ (vd: Spring Boot)"
        class="min-w-[170px] flex-1 rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs focus:border-emerald-500 focus:outline-none"
        @keydown.enter.prevent="insertInlineTag"
      />
      <div class="flex items-center gap-1">
        <button
          v-for="opt in TAG_COLOR_OPTIONS"
          :key="opt.id"
          type="button"
          class="flex h-6 w-6 items-center justify-center rounded-full border-2 transition-transform"
          :class="inlineTagColor === opt.id ? 'scale-110 border-slate-900' : 'border-transparent'"
          :title="opt.label"
          @click="inlineTagColor = opt.id"
        >
          <span class="h-4 w-4 rounded-full" :style="{ backgroundColor: opt.dot }" />
        </button>
      </div>
      <button
        type="button"
        class="inline-flex items-center gap-1 rounded-lg bg-emerald-600 px-2.5 py-1 font-semibold text-white hover:bg-emerald-700"
        @click="insertInlineTag"
      >
        <Plus class="h-3.5 w-3.5" />
        Chèn
      </button>
    </div>

    <!-- Source HTML Mode -->
    <textarea
      v-if="showSource"
      :value="modelValue"
      rows="6"
      class="w-full resize-y bg-slate-950 p-3 font-mono text-xs text-emerald-300 focus:outline-none"
      @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />

    <!-- Word-like Visual Editor -->
    <div
      v-else
      ref="editorRef"
      contenteditable="true"
      class="word-editor-surface min-h-[150px] max-h-[340px] overflow-y-auto p-3.5 text-sm leading-relaxed text-slate-800 focus:outline-none"
      :data-placeholder="placeholder || 'Nhập nội dung như Word, bôi đen để đổi màu hoặc chèn thẻ...'"
      @input="syncFromDom"
      @keyup="saveSelection"
      @mouseup="saveSelection"
      @blur="saveSelection"
    />
  </div>
</template>

<style scoped>
.tool-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  padding: 0.35rem;
  color: #334155;
  transition: background-color 0.15s;
}
.tool-btn:hover {
  background-color: #e2e8f0;
  color: #0f172a;
}
.word-editor-surface:empty::before {
  content: attr(data-placeholder);
  color: #94a3b8;
  pointer-events: none;
}
</style>
