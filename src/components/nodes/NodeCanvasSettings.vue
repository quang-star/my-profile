<script setup lang="ts">
import { computed } from 'vue'
import { Layers } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { nodeIcons } from './nodeIcons'
import type { NodeAttrs } from '@/types/node'

const props = defineProps<{ tag: string; saving: boolean; gridColumns?: number; dirty: boolean; variant: boolean; hasOverride: boolean }>()
const attrs = defineModel<NodeAttrs>({ required: true })
defineEmits<{ save: []; cancel: []; advanced: []; inherit: [] }>()
const { isVi } = useI18n()
const canSetIcon = computed(() => ['icon', 'text', 'heading', 'badge', 'link-button'].includes(props.tag))
const colors = [
  { name: 'bgColor', vi: 'Nền', en: 'Background' },
  { name: 'textColor', vi: 'Chữ', en: 'Text' },
  { name: 'borderColor', vi: 'Viền', en: 'Border' },
] as const

function setNumber(key: 'colSpan' | 'blockWidth' | 'minHeight', event: Event) {
  const value = (event.target as HTMLInputElement).valueAsNumber
  if (Number.isFinite(value)) attrs.value[key] = value
  else delete attrs.value[key]
  if (key === 'blockWidth') delete attrs.value.blockOffset
  if (key === 'minHeight') delete attrs.value.blockTop
}
</script>

<template>
  <form
    class="text-xs text-slate-800"
    data-editor-control
    @click.stop
    @keydown.esc.stop="$emit('cancel')"
    @submit.prevent="$emit('save')"
  >
    <fieldset :disabled="saving" class="grid gap-3">
      <label v-if="!variant" class="grid gap-1">
        {{ isVi ? 'Tên trong cây khối' : 'Layer name' }}
        <input v-model.trim="attrs.canvasLabel" type="text" maxlength="80" class="rounded border border-slate-300 px-2 py-1.5" />
      </label>
      <label class="flex items-center gap-2">
        <input v-model="attrs.hidden" type="checkbox" />
        {{ variant ? (isVi ? 'Ẩn trên màn hình này' : 'Hide on this device') : (isVi ? 'Ẩn khối' : 'Hide block') }}
      </label>
      <div v-if="tag === 'grid'" class="grid grid-cols-2 gap-2">
        <label class="grid gap-1">
          {{ isVi ? 'Số cột' : 'Columns' }}
          <select v-model.number="attrs.cols" class="rounded border border-slate-300 p-1">
            <option v-for="count in [1, 2, 3, 4, 12]" :key="count" :value="count">{{ count }}</option>
          </select>
        </label>
        <label class="grid gap-1">
          {{ isVi ? 'Khoảng cách' : 'Gap' }}
          <select v-model.number="attrs.gap" class="rounded border border-slate-300 p-1">
            <option v-for="gap in [2, 3, 4, 5, 6]" :key="gap" :value="gap">{{ gap * 4 }} px</option>
          </select>
        </label>
      </div>
      <label v-if="tag === 'card' || tag === 'section'" class="grid gap-1">
        {{ isVi ? 'Lề trong' : 'Padding' }}
        <select v-model="attrs.padding" class="rounded border border-slate-300 p-1">
          <option value="none">{{ isVi ? 'Không' : 'None' }}</option>
          <option value="sm">{{ isVi ? 'Nhỏ' : 'Small' }}</option>
          <option value="md">{{ isVi ? 'Vừa' : 'Medium' }}</option>
          <option value="lg">{{ isVi ? 'Lớn' : 'Large' }}</option>
        </select>
      </label>
      <template v-if="tag === 'card' || tag === 'section'">
        <div class="grid grid-cols-2 gap-2">
          <label class="grid gap-1">
            {{ isVi ? 'Xếp nội dung' : 'Stack direction' }}
            <select v-model="attrs.stackDirection" class="rounded border border-slate-300 p-1">
              <option :value="undefined">{{ isVi ? 'Dọc (mặc định)' : 'Vertical (default)' }}</option>
              <option value="row">{{ isVi ? 'Ngang' : 'Horizontal' }}</option>
              <option value="column">{{ isVi ? 'Dọc' : 'Vertical' }}</option>
            </select>
          </label>
          <label class="grid gap-1">
            {{ isVi ? 'Khoảng cách' : 'Gap' }}
            <select v-model.number="attrs.gap" class="rounded border border-slate-300 p-1">
              <option :value="undefined">{{ isVi ? 'Mặc định' : 'Default' }}</option>
              <option v-for="gap in [2, 3, 4, 5, 6, 8, 10]" :key="gap" :value="gap">{{ gap * 4 }} px</option>
            </select>
          </label>
        </div>
        <label v-if="attrs.stackDirection === 'row'" class="flex items-center gap-2">
          <input v-model="attrs.stackWrap" type="checkbox" />
          {{ isVi ? 'Tự xuống hàng' : 'Wrap to next line' }}
        </label>
        <label v-if="attrs.stackDirection === 'row'" class="grid gap-1">
          {{ isVi ? 'Phân bố ngang' : 'Horizontal distribution' }}
          <select v-model="attrs.stackJustify" class="rounded border border-slate-300 p-1">
            <option value="start">{{ isVi ? 'Đầu hàng' : 'Start' }}</option>
            <option value="center">{{ isVi ? 'Giữa' : 'Center' }}</option>
            <option value="end">{{ isVi ? 'Cuối hàng' : 'End' }}</option>
            <option value="space-between">{{ isVi ? 'Cách đều' : 'Space between' }}</option>
          </select>
        </label>
      </template>
      <div class="grid grid-cols-2 gap-2">
        <label v-if="gridColumns" class="grid gap-1">
          {{ isVi ? 'Số cột chiếm' : 'Column span' }}
          <input :value="attrs.colSpan" type="number" min="1" :max="gridColumns" placeholder="Auto" class="rounded border border-slate-300 p-1" @input="setNumber('colSpan', $event)" />
        </label>
        <label v-else class="grid gap-1">
          {{ isVi ? 'Chiều rộng (%)' : 'Width (%)' }}
          <input :value="attrs.blockWidth" type="number" min="1" max="100" placeholder="Auto" class="rounded border border-slate-300 p-1" @input="setNumber('blockWidth', $event)" />
        </label>
        <label class="grid gap-1">
          {{ isVi ? 'Chiều cao tối thiểu' : 'Min height (px)' }}
          <input :value="attrs.minHeight" type="number" min="0" max="1200" placeholder="Auto" class="rounded border border-slate-300 p-1" @input="setNumber('minHeight', $event)" />
        </label>
      </div>
      <div class="grid grid-cols-3 gap-2">
        <label v-for="item in colors" :key="item.name" class="grid gap-1">
          {{ isVi ? item.vi : item.en }}
          <input type="color" class="h-7 w-full rounded border border-slate-300" :value="attrs[item.name]" @input="attrs[item.name] = ($event.target as HTMLInputElement).value" />
        </label>
      </div>
      <div v-if="['heading', 'text', 'list'].includes(tag)" class="grid grid-cols-2 gap-2">
        <label class="grid gap-1">
          {{ isVi ? 'Cỡ chữ' : 'Font size' }}
          <select v-model="attrs.fontSize" class="rounded border border-slate-300 p-1">
            <option :value="undefined">{{ isVi ? 'Mặc định' : 'Default' }}</option>
            <option v-for="size in ['xs', 'sm', 'base', 'lg', 'xl', '2xl', '3xl', '4xl']" :key="size">{{ size }}</option>
          </select>
        </label>
        <label class="grid gap-1">
          {{ isVi ? 'Căn lề' : 'Alignment' }}
          <select v-model="attrs.align" class="rounded border border-slate-300 p-1">
            <option value="left">{{ isVi ? 'Trái' : 'Left' }}</option>
            <option value="center">{{ isVi ? 'Giữa' : 'Center' }}</option>
            <option value="right">{{ isVi ? 'Phải' : 'Right' }}</option>
          </select>
        </label>
      </div>
      <template v-if="tag === 'image'">
        <label class="grid gap-1">
          {{ isVi ? 'Đường dẫn ảnh' : 'Image URL' }}
          <input v-model.trim="attrs.src" class="rounded border border-slate-300 p-1" placeholder="https://…" />
        </label>
        <label class="grid gap-1">
          {{ isVi ? 'Mô tả ảnh' : 'Alt text' }}
          <input v-model="attrs.alt" class="rounded border border-slate-300 p-1" />
        </label>
      </template>
      <label v-if="tag === 'link-button'" class="grid gap-1">
        {{ isVi ? 'Đường dẫn' : 'Link destination' }}
        <input v-model.trim="attrs.href" class="rounded border border-slate-300 p-1" placeholder="https://…" />
      </label>
      <template v-if="canSetIcon">
        <label class="grid gap-1">
          Icon
          <select v-model="attrs.icon" aria-label="Icon" class="rounded border border-slate-300 p-1">
            <option v-if="tag !== 'icon'" value="">{{ isVi ? 'Không gắn icon' : 'No icon' }}</option>
            <option v-for="(icon, key) in nodeIcons" :key="key" :value="key">{{ icon.label }}</option>
          </select>
        </label>
        <div v-if="attrs.icon || tag === 'icon'" class="grid grid-cols-2 gap-2">
          <label class="grid gap-1">
            {{ isVi ? 'Cỡ icon (px)' : 'Icon size (px)' }}
            <input v-model.number="attrs.iconSize" type="number" min="12" max="128" class="h-7 w-full rounded border border-slate-300 px-1" />
          </label>
          <label class="grid gap-1">
            {{ isVi ? 'Màu icon' : 'Icon color' }}
            <input v-model="attrs.iconColor" type="color" class="h-7 w-full rounded border border-slate-300" />
          </label>
        </div>
      </template>
      <div class="flex flex-wrap items-center gap-1.5">
        <button type="submit" :disabled="!dirty" class="rounded-md bg-emerald-700 px-2.5 py-1.5 font-semibold text-white disabled:opacity-40">{{ isVi ? 'Lưu thiết lập' : 'Save settings' }}</button>
        <button v-if="dirty" type="button" class="rounded-md px-2 py-1 hover:bg-slate-100" @click="$emit('cancel')">{{ isVi ? 'Hủy' : 'Cancel' }}</button>
        <button v-if="variant && hasOverride" type="button" class="rounded-md px-2 py-1 hover:bg-slate-100" @click="$emit('inherit')">{{ isVi ? 'Dùng thiết lập gốc' : 'Use base settings' }}</button>
        <button v-if="!dirty && !variant" type="button" class="ml-auto inline-flex items-center gap-1 rounded-md px-2 py-1 hover:bg-slate-100" @click="$emit('advanced')">
          <Layers class="size-3" /> {{ isVi ? 'Nâng cao' : 'Advanced' }}
        </button>
      </div>
    </fieldset>
  </form>
</template>
