<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import {
  AlignCenter,
  AlignJustify,
  AlignLeft,
  AlignRight,
  Bold,
  ChevronDown,
  Group,
  Highlighter,
  Image as ImageIcon,
  Italic,
  Link2,
  List,
  ListOrdered,
  Minus,
  Plus,
  Redo2,
  RemoveFormatting,
  Strikethrough,
  Table2,
  Trash2,
  Underline,
  Undo2,
  Ungroup,
} from '@lucide/vue'
import MediaLibraryModal from '@/components/pages/MediaLibraryModal.vue'
import { useI18n } from '@/composables/useI18n'
import { DOC_BLOCK_PRESETS } from '@/editor/documentPresets'
import { unwrapBoxNode, wrapSelectionInBox } from '@/editor/editorState'
import {
  type TableBorderStyle,
  type TableCellPadding,
  getActiveTableInfo,
  resetActiveTableColumnWidths,
  selectActiveTableColumn,
  selectActiveTableRow,
  updateActiveTableAttrs,
} from '@/editor/extensions/StyledTable'
import { fileApi } from '@/services/api'
import { optimizeImageFile } from '@/utils/imageOptimize'
import { FONT_OPTIONS } from '@/utils/pageTheme'

const props = defineProps<{
  editor: Editor | null
}>()

const { isVi } = useI18n()

const openPopover = ref<'textColor' | 'highlight' | 'link' | 'image' | 'table' | 'cellBg' | 'insert' | null>(null)
const linkUrl = ref('')
const imageUrl = ref('')
const uploadingImage = ref(false)
const imageInputRef = ref<HTMLInputElement | null>(null)
const mediaOpen = ref(false)
const ribbonRef = ref<HTMLElement | null>(null)

const TEXT_SWATCHES = ['#18181b', '#52525b', '#ffffff', '#00b14f', '#1d4ed8', '#0f766e', '#be123c', '#b45309', '#7c3aed']
const HIGHLIGHT_SWATCHES = ['#fef08a', '#bbf7d0', '#bfdbfe', '#fecdd3', '#ddd6fe', '#fed7aa']
const CELL_BG_SWATCHES = ['#f8fafc', '#f1f5f9', '#ecfdf5', '#eff6ff', '#fef3c7', '#ffe4e6', '#18181b']
const FONT_SIZE_LIST = ['11px', '12px', '13px', '14px', '15px', '16px', '18px', '20px', '24px', '28px', '32px', '36px']
const LINE_HEIGHT_LIST = ['1.0', '1.15', '1.3', '1.5', '1.75', '2.0']

const currentBlockType = computed(() => {
  const ed = props.editor
  if (!ed) return 'p'
  if (ed.isActive('heading', { level: 1 })) return 'h1'
  if (ed.isActive('heading', { level: 2 })) return 'h2'
  if (ed.isActive('heading', { level: 3 })) return 'h3'
  return 'p'
})

const currentFontSize = computed(() => {
  const raw = props.editor?.getAttributes('textStyle')?.fontSize
  return typeof raw === 'string' && raw ? raw : '14px'
})

const currentFontFamily = computed(() => {
  const raw = props.editor?.getAttributes('textStyle')?.fontFamily
  return typeof raw === 'string' ? raw : ''
})

const currentColor = computed(() => props.editor?.getAttributes('textStyle')?.color || '#18181b')

const currentParagraphLineHeight = computed(() => {
  const ed = props.editor
  if (!ed) return ''
  const raw = ed.isActive('heading')
    ? ed.getAttributes('heading')?.lineHeight
    : ed.getAttributes('paragraph')?.lineHeight
  return typeof raw === 'string' && raw ? raw : ''
})

const activeTableInfo = computed(() => {
  const ed = props.editor
  if (!ed) return null
  // Touch ed.state.selection reactively via isActive
  void ed.isActive('table')
  return getActiveTableInfo(ed)
})

function setCellBackground(color: string | null) {
  const ed = props.editor
  if (!ed) return
  ed.chain().focus().setCellAttribute('backgroundColor', color).run()
  openPopover.value = null
}

function setParagraphLineHeight(val: string) {
  const ed = props.editor
  if (!ed) return
  if (!val) ed.chain().focus().unsetLineHeight().run()
  else ed.chain().focus().setLineHeight(val).run()
}

function setBlockType(val: string) {
  const ed = props.editor
  if (!ed) return
  if (val === 'p') ed.chain().focus().setParagraph().run()
  else if (val === 'h1') ed.chain().focus().toggleHeading({ level: 1 }).run()
  else if (val === 'h2') ed.chain().focus().toggleHeading({ level: 2 }).run()
  else if (val === 'h3') ed.chain().focus().toggleHeading({ level: 3 }).run()
}

function setFontFamily(stack: string) {
  const ed = props.editor
  if (!ed) return
  if (!stack) ed.chain().focus().unsetFontFamily().run()
  else ed.chain().focus().setFontFamily(stack).run()
}

function setFontSize(size: string) {
  const ed = props.editor
  if (!ed) return
  ed.chain().focus().setFontSize(size).run()
}

function stepFontSize(delta: number) {
  const num = parseInt(currentFontSize.value, 10) || 14
  const next = Math.max(10, Math.min(48, num + delta))
  setFontSize(`${next}px`)
}

function applyTextColor(color: string | null) {
  const ed = props.editor
  if (!ed) return
  if (!color) ed.chain().focus().unsetColor().run()
  else ed.chain().focus().setColor(color).run()
  openPopover.value = null
}

function applyHighlight(color: string | null) {
  const ed = props.editor
  if (!ed) return
  if (!color) ed.chain().focus().unsetHighlight().run()
  else ed.chain().focus().setHighlight({ color }).run()
  openPopover.value = null
}

function toggleLinkPopover() {
  if (openPopover.value === 'link') {
    openPopover.value = null
    return
  }
  linkUrl.value = props.editor?.getAttributes('link')?.href || 'https://'
  openPopover.value = 'link'
}

function applyLink() {
  const ed = props.editor
  if (!ed) return
  const clean = linkUrl.value.trim()
  if (!clean || clean === 'https://') {
    ed.chain().focus().unsetLink().run()
  } else {
    ed.chain().focus().setLink({ href: clean }).run()
  }
  openPopover.value = null
}

function insertImageByUrl() {
  const ed = props.editor
  const url = imageUrl.value.trim()
  if (!ed || !url) return
  ed.chain().focus().setImage({ src: url }).run()
  imageUrl.value = ''
  openPopover.value = null
}

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !props.editor) return
  uploadingImage.value = true
  try {
    const optimized = await optimizeImageFile(file)
    const res = await fileApi.uploadFile(optimized)
    props.editor.chain().focus().setImage({ src: res.url, alt: file.name }).run()
    openPopover.value = null
  } finally {
    uploadingImage.value = false
  }
}

function insertTable(rows: number, cols: number, withHeaderRow: boolean) {
  props.editor?.chain().focus().insertTable({ rows, cols, withHeaderRow }).run()
  openPopover.value = null
}

function insertBlockPreset(presetId: string) {
  const ed = props.editor
  const preset = DOC_BLOCK_PRESETS.find((p) => p.id === presetId)
  if (!ed || !preset) return
  ed.chain().focus().insertContent(preset.build(isVi.value)).run()
  openPopover.value = null
}

function handleDocClick(event: MouseEvent) {
  if (openPopover.value && ribbonRef.value && !ribbonRef.value.contains(event.target as Node)) {
    openPopover.value = null
  }
}

onMounted(() => document.addEventListener('click', handleDocClick))
onBeforeUnmount(() => document.removeEventListener('click', handleDocClick))
</script>

<template>
  <div
    v-if="editor"
    ref="ribbonRef"
    class="no-print sticky top-[49px] z-30 border-b border-zinc-200 bg-white/95 px-3 py-1.5 backdrop-blur-md"
    data-editor-control
  >
    <div class="mx-auto flex max-w-[1600px] flex-wrap items-center gap-1 text-xs text-zinc-700">
      <!-- Undo / Redo -->
      <button
        type="button"
        class="ribbon-btn"
        :disabled="!editor.can().undo()"
        :title="isVi ? 'Hoàn tác (Ctrl+Z)' : 'Undo'"
        @click="editor.chain().focus().undo().run()"
      >
        <Undo2 class="size-3.5" />
      </button>
      <button
        type="button"
        class="ribbon-btn"
        :disabled="!editor.can().redo()"
        :title="isVi ? 'Làm lại (Ctrl+Y)' : 'Redo'"
        @click="editor.chain().focus().redo().run()"
      >
        <Redo2 class="size-3.5" />
      </button>

      <span class="ribbon-sep" />

      <!-- Block Type -->
      <select
        :value="currentBlockType"
        class="h-7 rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-xs font-semibold text-zinc-800 outline-none focus:border-emerald-500"
        @change="setBlockType(($event.target as HTMLSelectElement).value)"
      >
        <option value="p">{{ isVi ? 'Văn bản' : 'Normal' }}</option>
        <option value="h1">{{ isVi ? 'Tiêu đề 1 (H1)' : 'Heading 1' }}</option>
        <option value="h2">{{ isVi ? 'Tiêu đề 2 (H2)' : 'Heading 2' }}</option>
        <option value="h3">{{ isVi ? 'Tiêu đề 3 (H3)' : 'Heading 3' }}</option>
      </select>

      <!-- Font Family -->
      <select
        :value="currentFontFamily"
        class="hidden h-7 max-w-32 rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-xs font-medium text-zinc-800 outline-none focus:border-emerald-500 sm:block"
        :title="isVi ? 'Phông chữ đoạn chọn' : 'Selection font'"
        @change="setFontFamily(($event.target as HTMLSelectElement).value)"
      >
        <option value="">{{ isVi ? 'Font mặc định' : 'Default font' }}</option>
        <option v-for="f in FONT_OPTIONS" :key="f.id" :value="f.cssStack">{{ f.label }}</option>
      </select>

      <!-- Font Size px -->
      <div class="flex items-center rounded-lg border border-zinc-200 bg-zinc-50">
        <button type="button" class="px-1.5 py-1 hover:bg-zinc-200/70" title="-1px" @click="stepFontSize(-1)">
          <Minus class="size-3" />
        </button>
        <select
          :value="currentFontSize"
          class="h-6 bg-transparent px-1 font-mono text-[11px] font-semibold outline-none"
          @change="setFontSize(($event.target as HTMLSelectElement).value)"
        >
          <option v-if="!FONT_SIZE_LIST.includes(currentFontSize)" :value="currentFontSize">{{ currentFontSize }}</option>
          <option v-for="sz in FONT_SIZE_LIST" :key="sz" :value="sz">{{ sz }}</option>
        </select>
        <button type="button" class="px-1.5 py-1 hover:bg-zinc-200/70" title="+1px" @click="stepFontSize(1)">
          <Plus class="size-3" />
        </button>
      </div>

      <span class="ribbon-sep" />

      <!-- B / I / U / S -->
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('bold') }" title="In đậm (Ctrl+B)" @click="editor.chain().focus().toggleBold().run()">
        <Bold class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('italic') }" title="In nghiêng (Ctrl+I)" @click="editor.chain().focus().toggleItalic().run()">
        <Italic class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('underline') }" title="Gạch chân (Ctrl+U)" @click="editor.chain().focus().toggleUnderline().run()">
        <Underline class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('strike') }" title="Gạch ngang" @click="editor.chain().focus().toggleStrike().run()">
        <Strikethrough class="size-3.5" />
      </button>

      <!-- Text Color -->
      <div class="relative">
        <button
          type="button"
          class="ribbon-btn flex-col gap-0 py-0.5"
          :title="isVi ? 'Màu chữ' : 'Text color'"
          @click.stop="openPopover = openPopover === 'textColor' ? null : 'textColor'"
        >
          <span class="text-[11px] font-extrabold leading-none">A</span>
          <span class="mt-0.5 h-1 w-3.5 rounded-full" :style="{ backgroundColor: currentColor }" />
        </button>
        <div v-if="openPopover === 'textColor'" class="ribbon-popover w-52">
          <p class="mb-1.5 text-[11px] font-bold text-zinc-600">{{ isVi ? 'Màu chữ' : 'Text color' }}</p>
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="hex in TEXT_SWATCHES"
              :key="hex"
              type="button"
              class="h-6 w-6 rounded-full border border-zinc-300 transition-transform hover:scale-110"
              :style="{ backgroundColor: hex }"
              @click="applyTextColor(hex)"
            />
            <input
              type="color"
              :value="currentColor"
              class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
              @change="applyTextColor(($event.target as HTMLInputElement).value)"
            />
          </div>
          <button type="button" class="mt-2 w-full rounded-md border border-zinc-200 py-1 text-[11px] font-semibold hover:bg-zinc-50" @click="applyTextColor(null)">
            {{ isVi ? 'Màu mặc định' : 'Reset color' }}
          </button>
        </div>
      </div>

      <!-- Highlight Color -->
      <div class="relative">
        <button
          type="button"
          class="ribbon-btn"
          :class="{ 'is-active': editor.isActive('highlight') }"
          :title="isVi ? 'Tô nền chữ' : 'Highlight'"
          @click.stop="openPopover = openPopover === 'highlight' ? null : 'highlight'"
        >
          <Highlighter class="size-3.5" />
        </button>
        <div v-if="openPopover === 'highlight'" class="ribbon-popover w-48">
          <p class="mb-1.5 text-[11px] font-bold text-zinc-600">{{ isVi ? 'Màu nền chữ' : 'Highlight' }}</p>
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="hex in HIGHLIGHT_SWATCHES"
              :key="hex"
              type="button"
              class="h-6 w-6 rounded-full border border-zinc-300 transition-transform hover:scale-110"
              :style="{ backgroundColor: hex }"
              @click="applyHighlight(hex)"
            />
          </div>
          <button type="button" class="mt-2 w-full rounded-md border border-zinc-200 py-1 text-[11px] font-semibold hover:bg-zinc-50" @click="applyHighlight(null)">
            {{ isVi ? 'Bỏ tô nền' : 'Clear highlight' }}
          </button>
        </div>
      </div>

      <button type="button" class="ribbon-btn" :title="isVi ? 'Xóa định dạng' : 'Clear formatting'" @click="editor.chain().focus().unsetAllMarks().run()">
        <RemoveFormatting class="size-3.5" />
      </button>

      <span class="ribbon-sep" />

      <!-- Text Align -->
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'left' }) }" title="Căn trái" @click="editor.chain().focus().setTextAlign('left').run()">
        <AlignLeft class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'center' }) }" title="Căn giữa" @click="editor.chain().focus().setTextAlign('center').run()">
        <AlignCenter class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'right' }) }" title="Căn phải" @click="editor.chain().focus().setTextAlign('right').run()">
        <AlignRight class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'justify' }) }" title="Căn đều 2 bên" @click="editor.chain().focus().setTextAlign('justify').run()">
        <AlignJustify class="size-3.5" />
      </button>

      <!-- Per-paragraph Line Height -->
      <select
        :value="currentParagraphLineHeight"
        class="h-7 rounded-lg border border-zinc-200 bg-zinc-50 px-1.5 font-mono text-[11px] font-semibold text-zinc-800 outline-none focus:border-emerald-500"
        :title="isVi ? 'Giãn dòng đoạn đang chọn' : 'Paragraph line spacing'"
        @change="setParagraphLineHeight(($event.target as HTMLSelectElement).value)"
      >
        <option value="">{{ isVi ? 'Giãn dòng' : 'Line' }}</option>
        <option v-if="currentParagraphLineHeight && !LINE_HEIGHT_LIST.includes(currentParagraphLineHeight)" :value="currentParagraphLineHeight">
          {{ currentParagraphLineHeight }}
        </option>
        <option v-for="lh in LINE_HEIGHT_LIST" :key="lh" :value="lh">{{ lh }}x</option>
      </select>

      <span class="ribbon-sep" />

      <!-- Lists & HR -->
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('bulletList') }" title="Danh sách gạch đầu dòng" @click="editor.chain().focus().toggleBulletList().run()">
        <List class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('orderedList') }" title="Danh sách đánh số" @click="editor.chain().focus().toggleOrderedList().run()">
        <ListOrdered class="size-3.5" />
      </button>
      <button type="button" class="ribbon-btn" title="Đường kẻ ngang" @click="editor.chain().focus().setHorizontalRule().run()">
        <Minus class="size-3.5" />
      </button>

      <!-- Link -->
      <div class="relative">
        <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('link') }" title="Chèn liên kết" @click.stop="toggleLinkPopover">
          <Link2 class="size-3.5" />
        </button>
        <form v-if="openPopover === 'link'" class="ribbon-popover w-64 space-y-2" @submit.prevent="applyLink">
          <input
            v-model="linkUrl"
            type="text"
            placeholder="https://… hoặc mailto:…"
            class="w-full rounded-lg border border-zinc-200 px-2.5 py-1.5 text-xs outline-none focus:border-emerald-500"
          />
          <div class="flex justify-end gap-1.5">
            <button type="button" class="rounded-lg border border-zinc-200 px-2.5 py-1 text-[11px]" @click="linkUrl = ''; applyLink()">
              {{ isVi ? 'Gỡ link' : 'Remove' }}
            </button>
            <button type="submit" class="rounded-lg bg-emerald-600 px-2.5 py-1 text-[11px] font-semibold text-white">
              {{ isVi ? 'Áp dụng' : 'Apply' }}
            </button>
          </div>
        </form>
      </div>

      <!-- Image -->
      <div class="relative">
        <button type="button" class="ribbon-btn" title="Chèn ảnh / Avatar" @click.stop="openPopover = openPopover === 'image' ? null : 'image'">
          <ImageIcon class="size-3.5" />
        </button>
        <div v-if="openPopover === 'image'" class="ribbon-popover w-64 space-y-2">
          <form class="flex gap-1" @submit.prevent="insertImageByUrl">
            <input
              v-model="imageUrl"
              type="text"
              placeholder="URL ảnh https://…"
              class="min-w-0 flex-1 rounded-lg border border-zinc-200 px-2 py-1 text-xs outline-none focus:border-emerald-500"
            />
            <button type="submit" class="rounded-lg bg-emerald-600 px-2.5 py-1 text-[11px] font-semibold text-white">
              OK
            </button>
          </form>
          <div class="grid grid-cols-2 gap-1.5">
            <button type="button" class="rounded-lg border border-zinc-200 py-1.5 text-[11px] font-semibold hover:bg-zinc-50" :disabled="uploadingImage" @click="imageInputRef?.click()">
              {{ uploadingImage ? 'Đang tải…' : (isVi ? 'Tải ảnh lên' : 'Upload') }}
            </button>
            <button type="button" class="rounded-lg border border-zinc-200 py-1.5 text-[11px] font-semibold hover:bg-zinc-50" @click="openPopover = null; mediaOpen = true">
              {{ isVi ? 'Kho ảnh' : 'Library' }}
            </button>
          </div>
          <input ref="imageInputRef" type="file" accept="image/*" class="sr-only" @change="handleImageUpload" />
        </div>
      </div>

      <!-- Table -->
      <div class="relative">
        <button type="button" class="ribbon-btn" :class="{ 'is-active': editor.isActive('table') }" title="Chèn bảng" @click.stop="openPopover = openPopover === 'table' ? null : 'table'">
          <Table2 class="size-3.5" />
        </button>
        <div v-if="openPopover === 'table'" class="ribbon-popover w-52 space-y-1">
          <button type="button" class="block w-full rounded-lg px-2.5 py-1.5 text-left text-xs hover:bg-zinc-100" @click="insertTable(2, 2, true)">
            {{ isVi ? 'Bảng chuẩn (2×2 có tiêu đề)' : 'Standard table (2×2)' }}
          </button>
          <button type="button" class="block w-full rounded-lg px-2.5 py-1.5 text-left text-xs hover:bg-zinc-100" @click="insertTable(1, 2, false)">
            {{ isVi ? 'Hàng 2 ô (Chức danh | Ngày)' : '2-cell row (Role | Date)' }}
          </button>
          <template v-if="editor.isActive('table')">
            <div class="my-1 border-t border-zinc-100" />
            <button type="button" class="block w-full rounded-lg px-2.5 py-1 text-left text-xs hover:bg-zinc-100" @click="editor.chain().focus().addRowAfter().run()">+ Thêm hàng</button>
            <button type="button" class="block w-full rounded-lg px-2.5 py-1 text-left text-xs hover:bg-zinc-100" @click="editor.chain().focus().addColumnAfter().run()">+ Thêm cột</button>
            <button type="button" class="block w-full rounded-lg px-2.5 py-1 text-left text-xs text-rose-600 hover:bg-rose-50" @click="editor.chain().focus().deleteTable().run()">Xóa bảng</button>
          </template>
        </div>
      </div>

      <!-- Wrap Selection in Section Box / Ungroup -->
      <button
        type="button"
        class="ribbon-btn gap-1"
        :title="isVi ? 'Gom các dòng đang chọn vào 1 Khung để kéo cả cụm' : 'Group selected blocks into a Box frame'"
        @click="wrapSelectionInBox(editor)"
      >
        <Group class="size-3.5" />
        <span class="hidden lg:inline text-[11px] font-semibold">{{ isVi ? 'Gom khung' : 'Group' }}</span>
      </button>
      <button
        type="button"
        class="ribbon-btn"
        :disabled="!editor.isActive('boxBlock')"
        :title="isVi ? 'Tách khỏi khung hiện tại' : 'Ungroup enclosing Box'"
        @click="unwrapBoxNode(editor)"
      >
        <Ungroup class="size-3.5" />
      </button>

      <span class="ribbon-sep" />

      <!-- Quick Insert Block -->
      <div class="relative">
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg bg-emerald-600 px-2.5 py-1 text-xs font-semibold text-white hover:bg-emerald-700"
          @click.stop="openPopover = openPopover === 'insert' ? null : 'insert'"
        >
          <Plus class="size-3.5" />
          <span>{{ isVi ? 'Chèn khối' : 'Insert block' }}</span>
          <ChevronDown class="size-3" />
        </button>
        <div v-if="openPopover === 'insert'" class="ribbon-popover right-0 left-auto w-64 max-h-80 overflow-y-auto space-y-0.5">
          <button
            v-for="preset in DOC_BLOCK_PRESETS"
            :key="preset.id"
            type="button"
            class="block w-full rounded-lg px-2.5 py-2 text-left hover:bg-emerald-50"
            @click="insertBlockPreset(preset.id)"
          >
            <p class="text-xs font-bold text-zinc-900">{{ isVi ? preset.vi : preset.en }}</p>
            <p class="truncate text-[11px] text-zinc-500">{{ isVi ? preset.descVi : preset.descEn }}</p>
          </button>
        </div>
      </div>
    </div>

    <!-- Contextual Table Tools Bar (Word-like Table Layout & Design bar when cursor is inside a table) -->
    <div
      v-if="activeTableInfo"
      class="mx-auto mt-1.5 flex max-w-[1600px] flex-wrap items-center gap-1.5 rounded-lg border border-emerald-200/90 bg-emerald-50/75 px-2.5 py-1 text-[11px] font-medium text-zinc-700"
    >
      <span class="inline-flex items-center gap-1 rounded bg-emerald-600 px-1.5 py-0.5 text-[10px] font-bold text-white">
        <Table2 class="size-3" />
        <span>
          {{ isVi ? `Bảng · Cột ${activeTableInfo.colIndex + 1}/${activeTableInfo.colCount}` : `Table · Col ${activeTableInfo.colIndex + 1}/${activeTableInfo.colCount}` }}
        </span>
      </span>

      <button
        type="button"
        class="table-ctx-btn"
        :title="isVi ? 'Bôi chọn cả cột đang đứng để căn lề, đổi cỡ chữ, màu hoặc tô nền hàng loạt' : 'Select entire active column'"
        @click="selectActiveTableColumn(editor)"
      >
        {{ isVi ? 'Chọn cả cột' : 'Select col' }}
      </button>
      <button
        type="button"
        class="table-ctx-btn"
        :title="isVi ? 'Bôi chọn cả hàng đang đứng' : 'Select entire active row'"
        @click="selectActiveTableRow(editor)"
      >
        {{ isVi ? 'Chọn cả hàng' : 'Select row' }}
      </button>

      <span class="ribbon-sep" />

      <!-- Border style pills -->
      <div class="inline-flex items-center rounded-md border border-zinc-200 bg-white p-0.5">
        <button
          v-for="opt in ([
            { id: 'none', vi: 'Không viền', en: 'Borderless' },
            { id: 'horizontal', vi: 'Kẻ ngang', en: 'Horizontal' },
            { id: 'grid', vi: 'Kẻ lưới', en: 'Grid' },
          ] as { id: TableBorderStyle; vi: string; en: string }[])"
          :key="opt.id"
          type="button"
          class="rounded px-1.5 py-0.5 text-[10px] font-semibold transition-colors"
          :class="activeTableInfo.attrs.borderStyle === opt.id ? 'bg-emerald-600 text-white' : 'text-zinc-600 hover:bg-zinc-100'"
          @click="updateActiveTableAttrs(editor, { borderStyle: opt.id })"
        >
          {{ isVi ? opt.vi : opt.en }}
        </button>
      </div>

      <!-- Cell padding -->
      <select
        :value="activeTableInfo.attrs.cellPadding"
        class="h-6 rounded-md border border-zinc-200 bg-white px-1.5 text-[11px] font-semibold text-zinc-700 outline-none focus:border-emerald-500"
        :title="isVi ? 'Độ thoáng các ô trong bảng' : 'Cell padding'"
        @change="updateActiveTableAttrs(editor, { cellPadding: ($event.target as HTMLSelectElement).value as TableCellPadding })"
      >
        <option value="compact">{{ isVi ? 'Ô: Gọn' : 'Pad: Compact' }}</option>
        <option value="normal">{{ isVi ? 'Ô: Vừa' : 'Pad: Normal' }}</option>
        <option value="relaxed">{{ isVi ? 'Ô: Thoáng' : 'Pad: Relaxed' }}</option>
      </select>

      <!-- Zebra striping -->
      <button
        type="button"
        class="table-ctx-btn"
        :class="{ 'is-active': activeTableInfo.attrs.striped }"
        :title="isVi ? 'Tô nền xen kẽ từng dòng cho dễ đọc' : 'Zebra striped rows'"
        @click="updateActiveTableAttrs(editor, { striped: !activeTableInfo.attrs.striped })"
      >
        {{ isVi ? 'Sọc dòng' : 'Striped' }}
      </button>

      <!-- Cell / Column Background -->
      <div class="relative">
        <button
          type="button"
          class="table-ctx-btn gap-1"
          :title="isVi ? 'Tô màu nền ô hoặc cột đang chọn' : 'Cell shading color'"
          @click.stop="openPopover = openPopover === 'cellBg' ? null : 'cellBg'"
        >
          <span
            class="inline-block size-2.5 rounded-sm border border-zinc-300"
            :style="{ backgroundColor: activeTableInfo.cellBg || '#ffffff' }"
          />
          <span>{{ isVi ? 'Màu ô' : 'Shading' }}</span>
        </button>
        <div v-if="openPopover === 'cellBg'" class="ribbon-popover w-52">
          <p class="mb-1.5 text-[11px] font-bold text-zinc-600">{{ isVi ? 'Màu nền ô / cột đang chọn' : 'Cell background' }}</p>
          <div class="flex flex-wrap items-center gap-1.5">
            <button
              v-for="hex in CELL_BG_SWATCHES"
              :key="hex"
              type="button"
              class="h-6 w-6 rounded-full border border-zinc-300 transition-transform hover:scale-110"
              :style="{ backgroundColor: hex }"
              @click="setCellBackground(hex)"
            />
            <input
              type="color"
              :value="activeTableInfo.cellBg?.startsWith('#') ? activeTableInfo.cellBg : '#f8fafc'"
              class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
              @change="setCellBackground(($event.target as HTMLInputElement).value)"
            />
          </div>
          <button
            type="button"
            class="mt-2 w-full rounded-md border border-zinc-200 py-1 text-[11px] font-semibold hover:bg-zinc-50"
            @click="setCellBackground(null)"
          >
            {{ isVi ? 'Trong suốt (Bỏ màu ô)' : 'Clear cell fill' }}
          </button>
        </div>
      </div>

      <span class="ribbon-sep" />

      <!-- Row / Column Structure actions -->
      <button type="button" class="table-ctx-btn" @click="editor.chain().focus().addRowAfter().run()">
        + {{ isVi ? 'Hàng' : 'Row' }}
      </button>
      <button type="button" class="table-ctx-btn" @click="editor.chain().focus().deleteRow().run()">
        - {{ isVi ? 'Hàng' : 'Row' }}
      </button>
      <button type="button" class="table-ctx-btn" @click="editor.chain().focus().addColumnAfter().run()">
        + {{ isVi ? 'Cột' : 'Col' }}
      </button>
      <button type="button" class="table-ctx-btn" @click="editor.chain().focus().deleteColumn().run()">
        - {{ isVi ? 'Cột' : 'Col' }}
      </button>
      <button
        type="button"
        class="table-ctx-btn"
        :title="isVi ? 'Bật/tắt hàng đầu làm hàng tiêu đề đậm' : 'Toggle header row'"
        @click="editor.chain().focus().toggleHeaderRow().run()"
      >
        {{ isVi ? 'Tiêu đề' : 'Header' }}
      </button>
      <button
        v-if="editor.can().mergeCells()"
        type="button"
        class="table-ctx-btn"
        @click="editor.chain().focus().mergeCells().run()"
      >
        {{ isVi ? 'Gộp ô' : 'Merge' }}
      </button>
      <button
        v-if="editor.can().splitCell()"
        type="button"
        class="table-ctx-btn"
        @click="editor.chain().focus().splitCell().run()"
      >
        {{ isVi ? 'Tách ô' : 'Split' }}
      </button>
      <button
        type="button"
        class="table-ctx-btn"
        :title="isVi ? 'Đặt lại độ rộng cột tự động sau khi kéo tay' : 'Auto-fit column widths'"
        @click="resetActiveTableColumnWidths(editor)"
      >
        {{ isVi ? 'Tự căn cột' : 'Auto-fit' }}
      </button>
      <button
        type="button"
        class="table-ctx-btn text-rose-600 hover:bg-rose-50 hover:text-rose-700"
        :title="isVi ? 'Xóa toàn bộ bảng này' : 'Delete table'"
        @click="editor.chain().focus().deleteTable().run()"
      >
        <Trash2 class="size-3" />
      </button>
    </div>

    <MediaLibraryModal
      :open="mediaOpen"
      accept="image"
      @close="mediaOpen = false"
      @select="editor?.chain().focus().setImage({ src: $event }).run(); mediaOpen = false"
    />
  </div>
</template>

<style scoped>
.ribbon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 1.75rem;
  min-width: 1.75rem;
  padding: 0 0.35rem;
  border-radius: 0.45rem;
  color: #3f3f46;
  transition: background-color 120ms, color 120ms;
}
.ribbon-btn:hover:not(:disabled) {
  background-color: #f4f4f5;
  color: #09090b;
}
.ribbon-btn.is-active {
  background-color: #ecfdf5;
  color: #047857;
}
.ribbon-btn:disabled {
  opacity: 0.35;
}
.ribbon-sep {
  width: 1px;
  height: 1.25rem;
  margin: 0 0.2rem;
  background-color: #e4e4e7;
}
.ribbon-popover {
  position: absolute;
  top: calc(100% + 0.35rem);
  left: 0;
  z-index: 50;
  border: 1px solid #e4e4e7;
  border-radius: 0.85rem;
  background: #ffffff;
  padding: 0.6rem;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14);
}
.table-ctx-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 1.5rem;
  padding: 0 0.45rem;
  border-radius: 0.375rem;
  border: 1px solid #e4e4e7;
  background-color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  color: #3f3f46;
  transition: background-color 120ms, border-color 120ms, color 120ms;
}
.table-ctx-btn:hover {
  background-color: #f4f4f5;
  color: #09090b;
}
.table-ctx-btn.is-active {
  border-color: #10b981;
  background-color: #d1fae5;
  color: #065f46;
}
</style>
