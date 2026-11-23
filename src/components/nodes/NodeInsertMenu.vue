<script setup lang="ts">
import { computed, ref } from 'vue'
import { Plus } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useNodes } from '@/composables/useNodes'
import { nodeIcons } from './nodeIcons'
import type { NodeAttrs, NodeTag } from '@/types/node'

const props = defineProps<{
  parentId: string | null
  parentTag?: string
  beforeId?: string
  label: string
}>()

const { isVi } = useI18n()
const { insertCanvasNode, visualSaving } = useNodes()
const menu = ref<'blocks' | 'icons' | null>(null)
const alignLeft = ref(false)
const openBelow = ref(false)
const choices = [
  { tag: 'section', vi: 'Mục', en: 'Section' },
  { tag: 'grid', vi: 'Lưới', en: 'Grid' },
  { tag: 'heading', vi: 'Tiêu đề', en: 'Heading' },
  { tag: 'text', vi: 'Chữ', en: 'Text' },
  { tag: 'icon', vi: 'Icon', en: 'Icon' },
  { tag: 'card', vi: 'Card', en: 'Card' },
  { tag: 'badge', vi: 'Nhãn', en: 'Badge' },
  { tag: 'image', vi: 'Ảnh', en: 'Image' },
  { tag: 'link-button', vi: 'Nút liên kết', en: 'Link button' },
  { tag: 'list', vi: 'Danh sách', en: 'List' },
  { tag: 'divider', vi: 'Đường phân cách', en: 'Divider' },
  { tag: 'contact-form', vi: 'Form liên hệ', en: 'Contact form' },
] as const
const available = computed(() => choices.filter(({ tag }) => {
  if (props.parentTag === 'badge-group') return tag === 'badge'
  return tag !== 'section' || props.parentId === null
}))

function toggle(event: MouseEvent) {
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  alignLeft.value = bounds.right < 184
  openBelow.value = bounds.top < 196
  menu.value = menu.value ? null : 'blocks'
}

async function insert(tag: NodeTag, icon?: string) {
  if (tag === 'icon' && !icon) {
    menu.value = 'icons'
    return
  }
  menu.value = null
  const attrs: NodeAttrs = {}
  if (tag === 'card') Object.assign(attrs, { bgColor: '#ffffff', borderColor: '#e2e8f0', rounded: '3xl', padding: 'md' })
  if (tag === 'grid') Object.assign(attrs, { cols: 2, gap: 4 })
  if (tag === 'heading') attrs.level = 'h2'
  if (tag === 'badge') Object.assign(attrs, { bgColor: '#ecfdf5', textColor: '#047857', borderColor: '#a7f3d0' })
  if (icon) Object.assign(attrs, { icon, iconSize: 24, iconColor: '#047857' })
  await insertCanvasNode({
    tag, attrs, parentId: props.parentId, beforeId: props.beforeId, contentVi: '', contentEn: '',
  }, ['heading', 'text', 'list', 'badge', 'link-button'].includes(tag))
}
</script>

<template>
  <div class="relative" data-editor-control @click.stop @keydown.esc.stop="menu = null">
    <button
      type="button"
      class="flex items-center gap-1 whitespace-nowrap rounded-lg border border-emerald-200 bg-white px-2 py-1 text-xs font-semibold text-emerald-800 shadow-sm hover:bg-emerald-50 disabled:opacity-50"
      :disabled="visualSaving"
      :aria-expanded="!!menu"
      @click="toggle"
    >
      <Plus class="size-3.5" />
      {{ label }}
    </button>
    <div v-if="menu" class="absolute z-30 max-h-60 min-w-36 overflow-auto rounded-xl border border-slate-200 bg-white p-1.5 text-sm text-slate-800 shadow-lg" :class="[alignLeft ? 'left-0' : 'right-0', openBelow ? 'top-full mt-1' : 'bottom-full mb-1']">
      <template v-if="menu === 'blocks'">
        <button v-for="item in available" :key="item.tag" type="button" class="block w-full rounded-lg px-3 py-1.5 text-left hover:bg-emerald-50" @click="insert(item.tag)">
          {{ isVi ? item.vi : item.en }}
        </button>
      </template>
      <template v-else>
        <button type="button" class="mb-1 w-full rounded-lg p-1 text-left text-xs hover:bg-slate-100" @click="menu = 'blocks'">
          {{ isVi ? '← Chọn loại khối' : '← Block type' }}
        </button>
        <div class="grid w-40 grid-cols-4 gap-1">
          <button v-for="(icon, key) in nodeIcons" :key="key" type="button" class="grid size-9 place-items-center rounded-lg hover:bg-emerald-50" :title="icon.label" :aria-label="icon.label" @click="insert('icon', key)">
            <component :is="icon.component" :id="key === 'github' ? 'github' : undefined" class="size-4" aria-hidden="true" />
          </button>
        </div>
      </template>
    </div>
  </div>
</template>
