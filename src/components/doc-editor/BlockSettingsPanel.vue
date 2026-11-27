<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Node as ProseMirrorNode } from '@tiptap/pm/model'
import type { Editor } from '@tiptap/vue-3'
import { ArrowDown, ArrowUp, Copy, Plus, Trash2, X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { DOC_BLOCK_PRESETS } from '@/editor/documentPresets'
import { deleteBlockNode, duplicateBlockNode, moveBlockNode, selectedBlockPos } from '@/editor/editorState'
import type { BadgeListAttrs } from '@/editor/extensions/BadgeListBlock'
import type { BoxBlockAttrs } from '@/editor/extensions/BoxBlock'
import type { ColumnCellAttrs } from '@/editor/extensions/ColumnCell'
import type { ColumnsBlockAttrs } from '@/editor/extensions/ColumnsBlock'
import type { DividerAttrs } from '@/editor/extensions/DividerBlock'
import type { LinkButtonBlockAttrs, LinkButtonItem } from '@/editor/extensions/LinkButtonBlock'
import type { SkillBarAttrs } from '@/editor/extensions/SkillBarBlock'
import {
  type StyledTableAttrs,
  type TableBorderStyle,
  type TableCellPadding,
  getActiveTableInfo,
  resetActiveTableColumnWidths,
  selectActiveTableColumn,
  selectActiveTableRow,
} from '@/editor/extensions/StyledTable'

const props = defineProps<{
  editor: Editor | null
}>()

const { isVi } = useI18n()
const newBadgeInput = ref('')

const TARGET_TYPES = new Set([
  'boxBlock',
  'columnsBlock',
  'table',
  'horizontalRule',
  'skillBarBlock',
  'badgeListBlock',
  'linkButtonBlock',
])

interface ActiveBlockInfo {
  node: ProseMirrorNode
  pos: number
}

const activeBlock = computed<ActiveBlockInfo | null>(() => {
  const ed = props.editor
  if (!ed) return null

  const { state } = ed
  const explicitPos = selectedBlockPos.value
  if (typeof explicitPos === 'number' && explicitPos >= 0 && explicitPos < state.doc.content.size) {
    const node = state.doc.nodeAt(explicitPos)
    if (node && TARGET_TYPES.has(node.type.name)) {
      return { node, pos: explicitPos }
    }
  }

  const sel = state.selection
  if ('node' in sel && sel.node && typeof sel.node === 'object' && 'type' in sel.node) {
    const pmNode = sel.node as ProseMirrorNode
    if (TARGET_TYPES.has(pmNode.type.name)) {
      return { node: pmNode, pos: sel.from }
    }
  }

  const $from = sel.$from
  for (let d = $from.depth; d >= 1; d--) {
    const n = $from.node(d)
    if (TARGET_TYPES.has(n.type.name)) {
      return { node: n, pos: $from.before(d) }
    }
  }
  return null
})

const activeColumnCell = computed(() => {
  const ed = props.editor
  if (!ed) return null
  const $from = ed.state.selection.$from
  for (let d = $from.depth; d >= 1; d--) {
    const n = $from.node(d)
    if (n.type.name === 'columnCell') {
      return {
        node: n,
        pos: $from.before(d),
        index: $from.index(d - 1),
      }
    }
  }
  return null
})

function updateNodeAttrsAt(pos: number, patch: Record<string, unknown>) {
  const ed = props.editor
  if (!ed) return
  const node = ed.state.doc.nodeAt(pos)
  if (!node) return
  const tr = ed.state.tr.setNodeMarkup(pos, undefined, { ...node.attrs, ...patch })
  ed.view.dispatch(tr)
}

function updateActiveAttrs(patch: Record<string, unknown>) {
  if (!activeBlock.value) return
  updateNodeAttrsAt(activeBlock.value.pos, patch)
}

function updateCellAttrs(pos: number, patch: Record<string, unknown>) {
  updateNodeAttrsAt(pos, patch)
}

function setColumnsWidths(widths: number[]) {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'columnsBlock') return
  const ed = props.editor!
  const pos = activeBlock.value.pos
  const node = activeBlock.value.node
  const currentCount = node.childCount

  if (widths.length === currentCount) {
    updateNodeAttrsAt(pos, { widths })
    return
  }

  // Add or remove a columnCell cleanly
  const schema = ed.state.schema
  const cellType = schema.nodes.columnCell
  const paraType = schema.nodes.paragraph
  if (!cellType || !paraType) return

  const children = []
  for (let i = 0; i < widths.length; i++) {
    if (i < currentCount) {
      children.push(node.child(i))
    } else {
      children.push(cellType.create({ bgColor: '#ffffff', borderColor: '#e4e4e7', paddingX: 14, paddingY: 14, radius: 12 }, paraType.create()))
    }
  }
  const updated = node.type.create({ ...node.attrs, widths }, children)
  const tr = ed.state.tr.replaceWith(pos, pos + node.nodeSize, updated)
  ed.view.dispatch(tr)
}

function updateTwoColRatio(firstColPct: number) {
  const clamped = Math.max(20, Math.min(80, Math.round(firstColPct)))
  setColumnsWidths([clamped, 100 - clamped])
}

function setEqualColumns(count: number) {
  const n = Math.max(2, Math.min(6, Math.round(count)))
  const base = Math.floor(100 / n)
  const widths = Array.from({ length: n }, () => base)
  widths[n - 1] = 100 - base * (n - 1)
  setColumnsWidths(widths)
}

function addColumnToActive() {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'columnsBlock') return
  const current = activeBlock.value.node.childCount
  if (current >= 6) return
  setEqualColumns(current + 1)
}

function removeColumnFromActive() {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'columnsBlock') return
  const current = activeBlock.value.node.childCount
  if (current <= 2) return
  setEqualColumns(current - 1)
}

function addBadgeToActive() {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'badgeListBlock') return
  const val = newBadgeInput.value.trim()
  if (!val) return
  const attrs = activeBlock.value.node.attrs as BadgeListAttrs
  updateActiveAttrs({ items: [...(attrs.items || []), val] })
  newBadgeInput.value = ''
}

function removeBadgeAt(idx: number) {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'badgeListBlock') return
  const attrs = activeBlock.value.node.attrs as BadgeListAttrs
  const next = [...(attrs.items || [])]
  next.splice(idx, 1)
  updateActiveAttrs({ items: next })
}

function addLinkItem() {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'linkButtonBlock') return
  const attrs = activeBlock.value.node.attrs as LinkButtonBlockAttrs
  const next: LinkButtonItem[] = [
    ...(attrs.items || []),
    { label: isVi.value ? 'Liên kết mới' : 'New link', href: 'https://', variant: 'outline' },
  ]
  updateActiveAttrs({ items: next })
}

function updateLinkItem(idx: number, patch: Partial<LinkButtonItem>) {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'linkButtonBlock') return
  const attrs = activeBlock.value.node.attrs as LinkButtonBlockAttrs
  const next = (attrs.items || []).map((item, i) => (i === idx ? { ...item, ...patch } : item))
  updateActiveAttrs({ items: next })
}

function removeLinkItem(idx: number) {
  if (!activeBlock.value || activeBlock.value.node.type.name !== 'linkButtonBlock') return
  const attrs = activeBlock.value.node.attrs as LinkButtonBlockAttrs
  const next = (attrs.items || []).filter((_, i) => i !== idx)
  updateActiveAttrs({ items: next })
}

function insertPreset(id: string) {
  const preset = DOC_BLOCK_PRESETS.find((p) => p.id === id)
  if (!preset || !props.editor) return
  props.editor.chain().focus().insertContent(preset.build(isVi.value)).run()
}

function blockTitle(name: string): string {
  const map: Record<string, string> = {
    boxBlock: isVi.value ? 'Khung nội dung (Box / Card)' : 'Content Box / Card',
    columnsBlock: isVi.value ? 'Khối Chia cột' : 'Columns Block',
    table: isVi.value ? 'Bảng dữ liệu (Table)' : 'Data Table',
    horizontalRule: isVi.value ? 'Đường kẻ ngang (Divider)' : 'Horizontal Divider',
    skillBarBlock: isVi.value ? 'Thanh kỹ năng (%)' : 'Skill Bar',
    badgeListBlock: isVi.value ? 'Nhóm nhãn kỹ năng' : 'Badge List',
    linkButtonBlock: isVi.value ? 'Nút liên kết' : 'Link Buttons',
  }
  return map[name] || name
}

const activeTableCellBg = computed(() => getActiveTableInfo(props.editor)?.cellBg || '')
const TABLE_CELL_SWATCHES = ['#f8fafc', '#f1f5f9', '#ecfdf5', '#eff6ff', '#fef3c7', '#ffe4e6', '#18181b']

const BOX_BG_SWATCHES = ['transparent', '#ffffff', '#f8fafc', '#f0fdf4', '#eff6ff', '#0f172a', '#18181b']
const DIVIDER_COLOR_SWATCHES = ['#111827', '#71717a', '#d4d4d8', '#00b14f', '#1d4ed8', '#be123c']
</script>

<template>
  <div class="space-y-4 text-xs text-zinc-700">
    <!-- No block active -->
    <div v-if="!activeBlock" class="space-y-3">
      <div class="rounded-xl border border-dashed border-zinc-200 bg-zinc-50 p-3 text-center">
        <p class="font-semibold text-zinc-800">
          {{ isVi ? 'Chưa chọn Khối nào' : 'No block selected' }}
        </p>
        <p class="mt-1 text-[11px] leading-relaxed text-zinc-500">
          {{ isVi ? 'Đặt con trỏ vào bên trong 1 Khung, Cột, Thanh kỹ năng hoặc bấm nút "Cài đặt" trên khối để chỉnh màu, căn lề, viền.' : 'Place cursor inside a Box, Columns or Skill block to customize padding, borders & colors.' }}
        </p>
      </div>

      <div class="space-y-1.5">
        <span class="block text-[11px] font-bold uppercase tracking-wider text-zinc-500">
          {{ isVi ? 'Chèn nhanh tại con trỏ' : 'Quick insert' }}
        </span>
        <button
          v-for="id in ['box-card', 'banner-header', 'cols-topcv', 'skill-bar', 'badge-list']"
          :key="id"
          type="button"
          class="flex w-full items-center justify-between rounded-xl border border-zinc-200 bg-white px-3 py-2 text-left font-semibold text-zinc-800 hover:border-emerald-300 hover:bg-emerald-50/40"
          @click="insertPreset(id)"
        >
          <span>{{ isVi ? DOC_BLOCK_PRESETS.find((p) => p.id === id)?.vi : DOC_BLOCK_PRESETS.find((p) => p.id === id)?.en }}</span>
          <Plus class="size-3.5 text-emerald-700" />
        </button>
      </div>
    </div>

    <!-- Active block controls -->
    <template v-else>
      <div class="flex items-center justify-between gap-2 rounded-xl border border-emerald-200 bg-emerald-50/60 px-3 py-2">
        <span class="truncate font-bold text-emerald-950">{{ blockTitle(activeBlock.node.type.name) }}</span>
        <div class="flex shrink-0 items-center gap-0.5">
          <button
            type="button"
            class="rounded p-1 hover:bg-white"
            title="Đưa lên"
            @click="editor && moveBlockNode(editor, () => activeBlock!.pos, -1)"
          >
            <ArrowUp class="size-3.5" />
          </button>
          <button
            type="button"
            class="rounded p-1 hover:bg-white"
            title="Đưa xuống"
            @click="editor && moveBlockNode(editor, () => activeBlock!.pos, 1)"
          >
            <ArrowDown class="size-3.5" />
          </button>
          <button
            type="button"
            class="rounded p-1 hover:bg-white"
            title="Nhân bản"
            @click="editor && duplicateBlockNode(editor, () => activeBlock!.pos)"
          >
            <Copy class="size-3.5" />
          </button>
          <button
            type="button"
            class="rounded p-1 text-rose-600 hover:bg-rose-100"
            title="Xóa khối"
            @click="editor && deleteBlockNode(editor, () => activeBlock!.pos)"
          >
            <Trash2 class="size-3.5" />
          </button>
        </div>
      </div>

      <!-- BOX BLOCK SETTINGS -->
      <template v-if="activeBlock.node.type.name === 'boxBlock'">
        <div class="space-y-3">
          <div class="space-y-1.5">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Màu nền khung' : 'Box background' }}</span>
            <div class="flex flex-wrap items-center gap-1.5">
              <button
                v-for="sw in BOX_BG_SWATCHES"
                :key="sw"
                type="button"
                class="h-6 w-6 rounded-full border transition-transform hover:scale-110"
                :class="(activeBlock.node.attrs as BoxBlockAttrs).bgColor === sw ? 'border-zinc-900 ring-2 ring-emerald-400' : 'border-zinc-300'"
                :style="{ background: sw === 'transparent' ? 'repeating-conic-gradient(#e4e4e7 0% 25%, #fff 0% 50%) 50% / 8px 8px' : sw }"
                :title="sw"
                @click="updateActiveAttrs({ bgColor: sw })"
              />
              <input
                type="color"
                :value="(activeBlock.node.attrs as BoxBlockAttrs).bgColor?.startsWith('#') ? (activeBlock.node.attrs as BoxBlockAttrs).bgColor : '#ffffff'"
                class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
                @input="updateActiveAttrs({ bgColor: ($event.target as HTMLInputElement).value })"
              />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Màu chữ khung' : 'Text color' }}</span>
              <div class="flex items-center gap-1.5">
                <input
                  type="color"
                  :value="(activeBlock.node.attrs as BoxBlockAttrs).textColor || '#18181b'"
                  class="h-7 w-8 cursor-pointer rounded border border-zinc-200 p-0"
                  @input="updateActiveAttrs({ textColor: ($event.target as HTMLInputElement).value })"
                />
                <button
                  type="button"
                  class="rounded-md border border-zinc-200 px-2 py-1 text-[11px] hover:bg-zinc-50"
                  @click="updateActiveAttrs({ textColor: '' })"
                >
                  Auto
                </button>
              </div>
            </label>

            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Màu viền' : 'Border color' }}</span>
              <input
                type="color"
                :value="(activeBlock.node.attrs as BoxBlockAttrs).borderColor?.startsWith('#') ? (activeBlock.node.attrs as BoxBlockAttrs).borderColor : '#e4e4e7'"
                class="h-7 w-full cursor-pointer rounded border border-zinc-200 p-0"
                @input="updateActiveAttrs({ borderColor: ($event.target as HTMLInputElement).value })"
              />
            </label>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Kiểu viền' : 'Border side' }}</span>
              <select
                :value="(activeBlock.node.attrs as BoxBlockAttrs).borderSide || 'all'"
                class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5"
                @change="updateActiveAttrs({ borderSide: ($event.target as HTMLSelectElement).value })"
              >
                <option value="all">{{ isVi ? '4 cạnh' : 'All sides' }}</option>
                <option value="left">{{ isVi ? 'Viền trái' : 'Left accent' }}</option>
                <option value="bottom">{{ isVi ? 'Gạch dưới' : 'Bottom' }}</option>
                <option value="top">{{ isVi ? 'Viền trên' : 'Top' }}</option>
              </select>
            </label>
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Độ dày viền (px)' : 'Border px' }}</span>
              <input
                type="number"
                min="0"
                max="12"
                :value="(activeBlock.node.attrs as BoxBlockAttrs).borderWidth ?? 1"
                class="rounded-lg border border-zinc-200 px-2 py-1.5"
                @input="updateActiveAttrs({ borderWidth: Number(($event.target as HTMLInputElement).value) })"
              />
            </label>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Bo góc (px)' : 'Radius' }}</span>
              <input
                type="number"
                min="0"
                max="36"
                :value="(activeBlock.node.attrs as BoxBlockAttrs).radius ?? 12"
                class="rounded-lg border border-zinc-200 px-2 py-1.5"
                @input="updateActiveAttrs({ radius: Number(($event.target as HTMLInputElement).value) })"
              />
            </label>
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Đổ bóng' : 'Shadow' }}</span>
              <select
                :value="(activeBlock.node.attrs as BoxBlockAttrs).shadow || 'none'"
                class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5"
                @change="updateActiveAttrs({ shadow: ($event.target as HTMLSelectElement).value })"
              >
                <option value="none">{{ isVi ? 'Không' : 'None' }}</option>
                <option value="sm">{{ isVi ? 'Nhẹ' : 'Soft' }}</option>
                <option value="md">{{ isVi ? 'Nổi' : 'Medium' }}</option>
              </select>
            </label>
          </div>

          <div class="space-y-2 border-t border-zinc-100 pt-3">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Căn lề & Kích thước khối' : 'Spacing & Size' }}</span>
            <div class="grid grid-cols-2 gap-2">
              <label class="grid gap-1">
                <span>{{ isVi ? 'Lề trong ngang (px)' : 'Padding X' }}</span>
                <input type="number" min="0" max="64" :value="(activeBlock.node.attrs as BoxBlockAttrs).paddingX ?? 20" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateActiveAttrs({ paddingX: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span>{{ isVi ? 'Lề trong dọc (px)' : 'Padding Y' }}</span>
                <input type="number" min="0" max="64" :value="(activeBlock.node.attrs as BoxBlockAttrs).paddingY ?? 16" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateActiveAttrs({ paddingY: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span>{{ isVi ? 'Cách trên (px)' : 'Margin top' }}</span>
                <input type="number" min="0" max="64" :value="(activeBlock.node.attrs as BoxBlockAttrs).marginTop ?? 0" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateActiveAttrs({ marginTop: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span>{{ isVi ? 'Cách dưới (px)' : 'Margin bottom' }}</span>
                <input type="number" min="0" max="64" :value="(activeBlock.node.attrs as BoxBlockAttrs).marginBottom ?? 14" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateActiveAttrs({ marginBottom: Number(($event.target as HTMLInputElement).value) })" />
              </label>
            </div>

            <div class="grid grid-cols-2 gap-2">
              <label class="grid gap-1">
                <span>{{ isVi ? 'Rộng (%)' : 'Width (%)' }}</span>
                <input type="number" min="25" max="100" step="5" :value="(activeBlock.node.attrs as BoxBlockAttrs).widthPct ?? 100" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateActiveAttrs({ widthPct: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span>{{ isVi ? 'Căn khối' : 'Align box' }}</span>
                <select :value="(activeBlock.node.attrs as BoxBlockAttrs).align || 'left'" class="rounded-lg border border-zinc-200 bg-white px-2 py-1" @change="updateActiveAttrs({ align: ($event.target as HTMLSelectElement).value })">
                  <option value="left">{{ isVi ? 'Trái' : 'Left' }}</option>
                  <option value="center">{{ isVi ? 'Giữa' : 'Center' }}</option>
                  <option value="right">{{ isVi ? 'Phải' : 'Right' }}</option>
                </select>
              </label>
            </div>
          </div>
        </div>
      </template>

      <!-- COLUMNS BLOCK SETTINGS -->
      <template v-else-if="activeBlock.node.type.name === 'columnsBlock'">
        <div class="space-y-3">
          <div class="space-y-1.5">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Tỷ lệ & Số cột' : 'Columns & Ratio' }}</span>
            <div class="grid grid-cols-3 gap-1.5">
              <button type="button" class="rounded-lg border border-zinc-200 py-1.5 font-semibold hover:bg-zinc-50" @click="setColumnsWidths([34, 66])">34 / 66</button>
              <button type="button" class="rounded-lg border border-zinc-200 py-1.5 font-semibold hover:bg-zinc-50" @click="setColumnsWidths([50, 50])">50 / 50</button>
              <button type="button" class="rounded-lg border border-zinc-200 py-1.5 font-semibold hover:bg-zinc-50" @click="setColumnsWidths([66, 34])">66 / 34</button>
            </div>
            <div class="grid grid-cols-2 gap-1.5">
              <button type="button" class="rounded-lg border border-zinc-200 py-1.5 font-semibold hover:bg-zinc-50" @click="setEqualColumns(3)">
                {{ isVi ? '3 cột đều' : '3 Equal' }}
              </button>
              <button type="button" class="rounded-lg border border-zinc-200 py-1.5 font-semibold hover:bg-zinc-50" @click="setEqualColumns(4)">
                {{ isVi ? '4 cột đều' : '4 Equal' }}
              </button>
            </div>
            <div class="flex items-center justify-between gap-2 rounded-xl border border-zinc-200 bg-zinc-50 px-2.5 py-1.5">
              <span class="font-mono text-[11px] font-bold text-zinc-700">
                {{ activeBlock.node.childCount }} {{ isVi ? 'cột (2–6)' : 'cols (2–6)' }}
              </span>
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  class="rounded-md border border-zinc-200 bg-white px-2 py-0.5 text-[11px] font-semibold hover:bg-zinc-100 disabled:opacity-40"
                  :disabled="activeBlock.node.childCount <= 2"
                  @click="removeColumnFromActive"
                >
                  {{ isVi ? '- Bớt cột' : '- Remove' }}
                </button>
                <button
                  type="button"
                  class="rounded-md border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-40"
                  :disabled="activeBlock.node.childCount >= 6"
                  @click="addColumnToActive"
                >
                  {{ isVi ? '+ Thêm cột' : '+ Add col' }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="activeBlock.node.childCount === 2" class="space-y-1">
            <div class="flex justify-between font-semibold">
              <span>{{ isVi ? 'Kéo tỷ lệ cột trái' : 'Left column %' }}</span>
              <span class="font-mono text-emerald-700">{{ Math.round((activeBlock.node.attrs as ColumnsBlockAttrs).widths?.[0] ?? 50) }}%</span>
            </div>
            <input
              type="range"
              min="20"
              max="80"
              step="1"
              :value="(activeBlock.node.attrs as ColumnsBlockAttrs).widths?.[0] ?? 50"
              class="w-full accent-emerald-600"
              @input="updateTwoColRatio(Number(($event.target as HTMLInputElement).value))"
            />
          </div>

          <div class="grid grid-cols-2 gap-2">
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Khoảng cách cột (px)' : 'Gap (px)' }}</span>
              <input type="number" min="0" max="64" :value="(activeBlock.node.attrs as ColumnsBlockAttrs).gap ?? 20" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateActiveAttrs({ gap: Number(($event.target as HTMLInputElement).value) })" />
            </label>
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Cách dưới (px)' : 'Margin bottom' }}</span>
              <input type="number" min="0" max="64" :value="(activeBlock.node.attrs as ColumnsBlockAttrs).marginBottom ?? 16" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateActiveAttrs({ marginBottom: Number(($event.target as HTMLInputElement).value) })" />
            </label>
          </div>

          <label class="flex cursor-pointer items-center gap-2 rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-2 font-medium">
            <input
              type="checkbox"
              :checked="(activeBlock.node.attrs as ColumnsBlockAttrs).stackMobile !== false"
              class="rounded border-zinc-300 text-emerald-600"
              @change="updateActiveAttrs({ stackMobile: ($event.target as HTMLInputElement).checked })"
            />
            <span>{{ isVi ? 'Tự xếp dọc trên điện thoại' : 'Stack columns on mobile' }}</span>
          </label>

          <!-- Current column cell settings -->
          <div v-if="activeColumnCell" class="space-y-2.5 border-t border-zinc-100 pt-3">
            <span class="block font-bold text-blue-800">
              {{ isVi ? `Cột đang đặt con trỏ (Cột ${activeColumnCell.index + 1})` : `Focused Column (${activeColumnCell.index + 1})` }}
            </span>
            <div class="flex flex-wrap items-center gap-1.5">
              <button
                v-for="sw in BOX_BG_SWATCHES"
                :key="sw"
                type="button"
                class="h-5 w-5 rounded-full border"
                :class="(activeColumnCell.node.attrs as ColumnCellAttrs).bgColor === sw ? 'border-zinc-900 ring-2 ring-blue-400' : 'border-zinc-300'"
                :style="{ background: sw === 'transparent' ? 'repeating-conic-gradient(#e4e4e7 0% 25%, #fff 0% 50%) 50% / 8px 8px' : sw }"
                @click="updateCellAttrs(activeColumnCell.pos, { bgColor: sw })"
              />
              <input
                type="color"
                :value="(activeColumnCell.node.attrs as ColumnCellAttrs).bgColor?.startsWith('#') ? (activeColumnCell.node.attrs as ColumnCellAttrs).bgColor : '#ffffff'"
                class="h-5 w-6 cursor-pointer rounded border border-zinc-200 p-0"
                @input="updateCellAttrs(activeColumnCell.pos, { bgColor: ($event.target as HTMLInputElement).value })"
              />
            </div>

            <div class="grid grid-cols-2 gap-2">
              <label class="grid gap-1">
                <span>{{ isVi ? 'Màu chữ cột' : 'Cell text' }}</span>
                <div class="flex items-center gap-1">
                  <input
                    type="color"
                    :value="(activeColumnCell.node.attrs as ColumnCellAttrs).textColor || '#18181b'"
                    class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
                    @input="updateCellAttrs(activeColumnCell.pos, { textColor: ($event.target as HTMLInputElement).value })"
                  />
                  <button type="button" class="rounded border border-zinc-200 px-1.5 py-0.5 text-[10px]" @click="updateCellAttrs(activeColumnCell.pos, { textColor: '' })">Auto</button>
                </div>
              </label>
              <label class="grid gap-1">
                <span>{{ isVi ? 'Màu viền cột' : 'Cell border' }}</span>
                <div class="flex items-center gap-1">
                  <input
                    type="color"
                    :value="(activeColumnCell.node.attrs as ColumnCellAttrs).borderColor?.startsWith('#') ? (activeColumnCell.node.attrs as ColumnCellAttrs).borderColor : '#e4e4e7'"
                    class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
                    @input="updateCellAttrs(activeColumnCell.pos, { borderColor: ($event.target as HTMLInputElement).value })"
                  />
                  <button type="button" class="rounded border border-zinc-200 px-1.5 py-0.5 text-[10px]" @click="updateCellAttrs(activeColumnCell.pos, { borderColor: 'transparent' })">Không</button>
                </div>
              </label>
            </div>

            <div class="grid grid-cols-2 gap-2">
              <label class="grid gap-1">
                <span>{{ isVi ? 'Lề trong (px)' : 'Padding' }}</span>
                <input type="number" min="0" max="48" :value="(activeColumnCell.node.attrs as ColumnCellAttrs).paddingX ?? 12" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateCellAttrs(activeColumnCell.pos, { paddingX: Number(($event.target as HTMLInputElement).value), paddingY: Number(($event.target as HTMLInputElement).value) })" />
              </label>
              <label class="grid gap-1">
                <span>{{ isVi ? 'Bo góc (px)' : 'Radius' }}</span>
                <input type="number" min="0" max="32" :value="(activeColumnCell.node.attrs as ColumnCellAttrs).radius ?? 10" class="rounded-lg border border-zinc-200 px-2 py-1" @input="updateCellAttrs(activeColumnCell.pos, { radius: Number(($event.target as HTMLInputElement).value) })" />
              </label>
            </div>
          </div>
        </div>
      </template>

      <!-- SKILL BAR SETTINGS -->
      <template v-else-if="activeBlock.node.type.name === 'skillBarBlock'">
        <div class="space-y-3">
          <label class="grid gap-1 font-semibold">
            <span>{{ isVi ? 'Tên kỹ năng' : 'Skill label' }}</span>
            <input
              type="text"
              :value="(activeBlock.node.attrs as SkillBarAttrs).label"
              class="rounded-lg border border-zinc-200 px-2.5 py-1.5"
              @input="updateActiveAttrs({ label: ($event.target as HTMLInputElement).value })"
            />
          </label>
          <label class="grid gap-1 font-semibold">
            <span>{{ isVi ? `Mức độ (${(activeBlock.node.attrs as SkillBarAttrs).level}%)` : `Level (${(activeBlock.node.attrs as SkillBarAttrs).level}%)` }}</span>
            <input
              type="range"
              min="5"
              max="100"
              step="5"
              :value="(activeBlock.node.attrs as SkillBarAttrs).level"
              class="accent-emerald-600"
              @input="updateActiveAttrs({ level: Number(($event.target as HTMLInputElement).value) })"
            />
          </label>
          <label class="flex cursor-pointer items-center gap-2 font-medium">
            <input
              type="checkbox"
              :checked="(activeBlock.node.attrs as SkillBarAttrs).showPercent !== false"
              class="rounded border-zinc-300 text-emerald-600"
              @change="updateActiveAttrs({ showPercent: ($event.target as HTMLInputElement).checked })"
            />
            <span>{{ isVi ? 'Hiển thị số %' : 'Show %' }}</span>
          </label>
          <label class="grid gap-1 font-semibold">
            <span>{{ isVi ? 'Màu thanh (để trống = màu chủ đạo)' : 'Bar color' }}</span>
            <div class="flex items-center gap-1.5">
              <input
                type="color"
                :value="(activeBlock.node.attrs as SkillBarAttrs).color || '#1d4ed8'"
                class="h-7 w-9 cursor-pointer rounded border border-zinc-200 p-0"
                @input="updateActiveAttrs({ color: ($event.target as HTMLInputElement).value })"
              />
              <button type="button" class="rounded-lg border border-zinc-200 px-2 py-1 text-[11px]" @click="updateActiveAttrs({ color: '' })">
                {{ isVi ? 'Theo chủ đề' : 'Theme default' }}
              </button>
            </div>
          </label>
        </div>
      </template>

      <!-- BADGE LIST SETTINGS -->
      <template v-else-if="activeBlock.node.type.name === 'badgeListBlock'">
        <div class="space-y-3">
          <form class="flex gap-1" @submit.prevent="addBadgeToActive">
            <input v-model="newBadgeInput" type="text" placeholder="Thêm thẻ kỹ năng..." class="min-w-0 flex-1 rounded-lg border border-zinc-200 px-2.5 py-1.5" />
            <button type="submit" class="rounded-lg bg-emerald-600 px-2.5 py-1.5 font-semibold text-white">+</button>
          </form>
          <div class="flex flex-wrap gap-1">
            <span
              v-for="(tag, idx) in ((activeBlock.node.attrs as BadgeListAttrs).items || [])"
              :key="`${tag}-${idx}`"
              class="inline-flex items-center gap-1 rounded-full border border-zinc-200 bg-zinc-50 px-2 py-0.5 text-[11px]"
            >
              <span>{{ tag }}</span>
              <button type="button" class="text-zinc-400 hover:text-rose-600" @click="removeBadgeAt(idx)">
                <X class="size-3" />
              </button>
            </span>
          </div>
          <label class="grid gap-1 font-semibold">
            <span>{{ isVi ? 'Căn hàng' : 'Align' }}</span>
            <select
              :value="(activeBlock.node.attrs as BadgeListAttrs).align || 'left'"
              class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5"
              @change="updateActiveAttrs({ align: ($event.target as HTMLSelectElement).value })"
            >
              <option value="left">{{ isVi ? 'Trái' : 'Left' }}</option>
              <option value="center">{{ isVi ? 'Giữa' : 'Center' }}</option>
              <option value="right">{{ isVi ? 'Phải' : 'Right' }}</option>
            </select>
          </label>
        </div>
      </template>

      <!-- LINK BUTTON BLOCK SETTINGS -->
      <template v-else-if="activeBlock.node.type.name === 'linkButtonBlock'">
        <div class="space-y-2.5">
          <div
            v-for="(item, idx) in ((activeBlock.node.attrs as LinkButtonBlockAttrs).items || [])"
            :key="idx"
            class="space-y-1.5 rounded-xl border border-zinc-200 bg-zinc-50 p-2.5"
          >
            <div class="flex items-center justify-between gap-1">
              <span class="text-[11px] font-bold text-zinc-500">#{{ idx + 1 }}</span>
              <button type="button" class="text-rose-600 hover:underline" @click="removeLinkItem(idx)">Xóa</button>
            </div>
            <input
              type="text"
              :value="item.label"
              placeholder="Nhãn nút..."
              class="w-full rounded-lg border border-zinc-200 bg-white px-2 py-1"
              @input="updateLinkItem(idx, { label: ($event.target as HTMLInputElement).value })"
            />
            <input
              type="text"
              :value="item.href"
              placeholder="https://… hoặc mailto:…"
              class="w-full rounded-lg border border-zinc-200 bg-white px-2 py-1 font-mono text-[11px]"
              @input="updateLinkItem(idx, { href: ($event.target as HTMLInputElement).value })"
            />
            <select
              :value="item.variant || 'primary'"
              class="w-full rounded-lg border border-zinc-200 bg-white px-2 py-1"
              @change="updateLinkItem(idx, { variant: ($event.target as HTMLSelectElement).value as 'primary' | 'outline' })"
            >
              <option value="primary">{{ isVi ? 'Nút nổi (Màu chủ đạo)' : 'Solid Primary' }}</option>
              <option value="outline">{{ isVi ? 'Nút viền (Outline)' : 'Outline' }}</option>
            </select>
          </div>

          <button
            type="button"
            class="w-full rounded-xl border border-dashed border-emerald-400 py-1.5 font-semibold text-emerald-700 hover:bg-emerald-50"
            @click="addLinkItem"
          >
            + {{ isVi ? 'Thêm nút liên kết' : 'Add link button' }}
          </button>
        </div>
      </template>

      <!-- HORIZONTAL DIVIDER SETTINGS -->
      <template v-else-if="activeBlock.node.type.name === 'horizontalRule'">
        <div class="space-y-3">
          <div class="space-y-1.5">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Màu đường kẻ' : 'Divider color' }}</span>
            <div class="flex flex-wrap items-center gap-1.5">
              <button
                v-for="sw in DIVIDER_COLOR_SWATCHES"
                :key="sw"
                type="button"
                class="h-6 w-6 rounded-full border transition-transform hover:scale-110"
                :class="(activeBlock.node.attrs as DividerAttrs).color === sw ? 'border-zinc-900 ring-2 ring-emerald-400' : 'border-zinc-300'"
                :style="{ backgroundColor: sw }"
                :title="sw"
                @click="updateActiveAttrs({ color: sw })"
              />
              <input
                type="color"
                :value="(activeBlock.node.attrs as DividerAttrs).color?.startsWith('#') ? (activeBlock.node.attrs as DividerAttrs).color : '#18181b'"
                class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
                @input="updateActiveAttrs({ color: ($event.target as HTMLInputElement).value })"
              />
              <button
                type="button"
                class="rounded-md border border-zinc-200 px-2 py-1 text-[11px] hover:bg-zinc-50"
                :class="!(activeBlock.node.attrs as DividerAttrs).color ? 'border-emerald-400 bg-emerald-50 font-semibold text-emerald-800' : ''"
                @click="updateActiveAttrs({ color: '' })"
              >
                {{ isVi ? 'Theo chủ đề' : 'Theme' }}
              </button>
            </div>
          </div>

          <div class="space-y-1.5">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Độ dày (px)' : 'Thickness (px)' }}</span>
            <div class="grid grid-cols-5 gap-1">
              <button
                v-for="px in [1, 2, 3, 4]"
                :key="px"
                type="button"
                class="rounded-lg border py-1 font-mono text-[11px] font-semibold"
                :class="Number((activeBlock.node.attrs as DividerAttrs).thickness ?? 1) === px ? 'border-emerald-500 bg-emerald-50 text-emerald-800' : 'border-zinc-200 bg-white hover:bg-zinc-50'"
                @click="updateActiveAttrs({ thickness: px })"
              >
                {{ px }}px
              </button>
              <input
                type="number"
                min="1"
                max="8"
                :value="(activeBlock.node.attrs as DividerAttrs).thickness ?? 1"
                class="rounded-lg border border-zinc-200 px-1.5 py-1 text-center font-mono text-[11px]"
                @input="updateActiveAttrs({ thickness: Math.max(1, Math.min(8, Number(($event.target as HTMLInputElement).value) || 1)) })"
              />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Kiểu nét' : 'Line style' }}</span>
              <select
                :value="(activeBlock.node.attrs as DividerAttrs).lineStyle || 'solid'"
                class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5"
                @change="updateActiveAttrs({ lineStyle: ($event.target as HTMLSelectElement).value })"
              >
                <option value="solid">{{ isVi ? 'Nét liền' : 'Solid' }}</option>
                <option value="dashed">{{ isVi ? 'Nét đứt' : 'Dashed' }}</option>
                <option value="dotted">{{ isVi ? 'Nét chấm' : 'Dotted' }}</option>
              </select>
            </label>
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Căn hàng' : 'Align' }}</span>
              <select
                :value="(activeBlock.node.attrs as DividerAttrs).align || 'left'"
                class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5"
                @change="updateActiveAttrs({ align: ($event.target as HTMLSelectElement).value })"
              >
                <option value="left">{{ isVi ? 'Trái' : 'Left' }}</option>
                <option value="center">{{ isVi ? 'Giữa' : 'Center' }}</option>
                <option value="right">{{ isVi ? 'Phải' : 'Right' }}</option>
              </select>
            </label>
          </div>

          <label class="grid gap-1 font-semibold">
            <span>{{ isVi ? `Độ dài (${(activeBlock.node.attrs as DividerAttrs).widthPct ?? 100}%)` : `Width (${(activeBlock.node.attrs as DividerAttrs).widthPct ?? 100}%)` }}</span>
            <input
              type="range"
              min="15"
              max="100"
              step="5"
              :value="(activeBlock.node.attrs as DividerAttrs).widthPct ?? 100"
              class="accent-emerald-600"
              @input="updateActiveAttrs({ widthPct: Number(($event.target as HTMLInputElement).value) })"
            />
          </label>

          <div class="grid grid-cols-2 gap-2 border-t border-zinc-100 pt-2">
            <label class="grid gap-1">
              <span>{{ isVi ? 'Cách trên (px)' : 'Margin top' }}</span>
              <input
                type="number"
                min="0"
                max="40"
                :value="(activeBlock.node.attrs as DividerAttrs).marginTop ?? 2"
                class="rounded-lg border border-zinc-200 px-2 py-1"
                @input="updateActiveAttrs({ marginTop: Number(($event.target as HTMLInputElement).value) })"
              />
            </label>
            <label class="grid gap-1">
              <span>{{ isVi ? 'Cách dưới (px)' : 'Margin bottom' }}</span>
              <input
                type="number"
                min="0"
                max="40"
                :value="(activeBlock.node.attrs as DividerAttrs).marginBottom ?? 6"
                class="rounded-lg border border-zinc-200 px-2 py-1"
                @input="updateActiveAttrs({ marginBottom: Number(($event.target as HTMLInputElement).value) })"
              />
            </label>
          </div>
        </div>
      </template>

      <!-- DATA TABLE SETTINGS -->
      <template v-else-if="activeBlock.node.type.name === 'table'">
        <div class="space-y-3">
          <div class="grid grid-cols-2 gap-1.5">
            <button
              type="button"
              class="rounded-lg border border-emerald-300 bg-emerald-50 px-2.5 py-1.5 font-semibold text-emerald-800 hover:bg-emerald-100"
              @click="selectActiveTableColumn(editor)"
            >
              {{ isVi ? 'Chọn cả cột' : 'Select column' }}
            </button>
            <button
              type="button"
              class="rounded-lg border border-zinc-200 bg-white px-2.5 py-1.5 font-semibold hover:bg-zinc-50"
              @click="selectActiveTableRow(editor)"
            >
              {{ isVi ? 'Chọn cả hàng' : 'Select row' }}
            </button>
          </div>

          <label class="grid gap-1 font-semibold">
            <span>{{ isVi ? 'Kiểu đường viền bảng' : 'Table border style' }}</span>
            <select
              :value="(activeBlock.node.attrs as StyledTableAttrs).borderStyle || 'none'"
              class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5"
              @change="updateActiveAttrs({ borderStyle: ($event.target as HTMLSelectElement).value as TableBorderStyle })"
            >
              <option value="none">{{ isVi ? 'Không viền (CV gọn)' : 'Borderless (Clean CV)' }}</option>
              <option value="horizontal">{{ isVi ? 'Kẻ ngang mảnh từng dòng' : 'Horizontal dividers' }}</option>
              <option value="grid">{{ isVi ? 'Kẻ lưới đầy đủ (Full grid)' : 'Full grid borders' }}</option>
            </select>
          </label>

          <div class="grid grid-cols-2 gap-2">
            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Màu đường kẻ' : 'Border color' }}</span>
              <div class="flex items-center gap-1">
                <input
                  type="color"
                  :value="(activeBlock.node.attrs as StyledTableAttrs).borderColor?.startsWith('#') ? (activeBlock.node.attrs as StyledTableAttrs).borderColor : '#d4d4d8'"
                  class="h-7 w-8 cursor-pointer rounded border border-zinc-200 p-0"
                  @input="updateActiveAttrs({ borderColor: ($event.target as HTMLInputElement).value })"
                />
                <button
                  type="button"
                  class="rounded-md border border-zinc-200 px-2 py-1 text-[11px] hover:bg-zinc-50"
                  @click="updateActiveAttrs({ borderColor: '' })"
                >
                  Auto
                </button>
              </div>
            </label>

            <label class="grid gap-1 font-semibold">
              <span>{{ isVi ? 'Độ thoáng ô' : 'Cell padding' }}</span>
              <select
                :value="(activeBlock.node.attrs as StyledTableAttrs).cellPadding || 'normal'"
                class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5"
                @change="updateActiveAttrs({ cellPadding: ($event.target as HTMLSelectElement).value as TableCellPadding })"
              >
                <option value="compact">{{ isVi ? 'Gọn' : 'Compact' }}</option>
                <option value="normal">{{ isVi ? 'Vừa' : 'Normal' }}</option>
                <option value="relaxed">{{ isVi ? 'Thoáng' : 'Relaxed' }}</option>
              </select>
            </label>
          </div>

          <label class="flex cursor-pointer items-center gap-2 font-medium">
            <input
              type="checkbox"
              :checked="Boolean((activeBlock.node.attrs as StyledTableAttrs).striped)"
              class="rounded border-zinc-300 text-emerald-600"
              @change="updateActiveAttrs({ striped: ($event.target as HTMLInputElement).checked })"
            />
            <span>{{ isVi ? 'Tô nền xen kẽ từng dòng (Sọc)' : 'Zebra striped rows' }}</span>
          </label>

          <div class="space-y-1.5 border-t border-zinc-100 pt-2">
            <span class="block font-bold text-zinc-800">{{ isVi ? 'Màu nền ô / cột đang chọn' : 'Selected cell/col fill' }}</span>
            <div class="flex flex-wrap items-center gap-1.5">
              <button
                v-for="sw in TABLE_CELL_SWATCHES"
                :key="sw"
                type="button"
                class="h-6 w-6 rounded-full border transition-transform hover:scale-110"
                :class="activeTableCellBg === sw ? 'border-zinc-900 ring-2 ring-emerald-400' : 'border-zinc-300'"
                :style="{ backgroundColor: sw }"
                @click="editor?.chain().focus().setCellAttribute('backgroundColor', sw).run()"
              />
              <input
                type="color"
                :value="activeTableCellBg?.startsWith('#') ? activeTableCellBg : '#f8fafc'"
                class="h-6 w-7 cursor-pointer rounded border border-zinc-200 p-0"
                @input="editor?.chain().focus().setCellAttribute('backgroundColor', ($event.target as HTMLInputElement).value).run()"
              />
              <button
                type="button"
                class="rounded-md border border-zinc-200 px-2 py-1 text-[11px] hover:bg-zinc-50"
                @click="editor?.chain().focus().setCellAttribute('backgroundColor', null).run()"
              >
                {{ isVi ? 'Trong suốt' : 'Clear' }}
              </button>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-1.5 border-t border-zinc-100 pt-2">
            <button
              type="button"
              class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5 font-semibold hover:bg-zinc-50"
              @click="editor?.chain().focus().addRowAfter().run()"
            >
              + {{ isVi ? 'Thêm hàng' : 'Add row' }}
            </button>
            <button
              type="button"
              class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5 font-semibold hover:bg-zinc-50"
              @click="editor?.chain().focus().addColumnAfter().run()"
            >
              + {{ isVi ? 'Thêm cột' : 'Add col' }}
            </button>
            <button
              type="button"
              class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5 font-semibold hover:bg-zinc-50"
              @click="editor?.chain().focus().toggleHeaderRow().run()"
            >
              {{ isVi ? 'Bật/tắt tiêu đề' : 'Toggle header' }}
            </button>
            <button
              type="button"
              class="rounded-lg border border-zinc-200 bg-white px-2 py-1.5 font-semibold hover:bg-zinc-50"
              @click="resetActiveTableColumnWidths(editor)"
            >
              {{ isVi ? 'Tự căn cột' : 'Auto-fit cols' }}
            </button>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>
