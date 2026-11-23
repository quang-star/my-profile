<script setup lang="ts">
import { reactive, ref, toRef, watch } from 'vue'
import { X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'
import type { SkillGroup } from '@/types/profile'

const props = defineProps<{
  open: boolean
  skill: SkillGroup | null
}>()

const emit = defineEmits<{
  close: []
}>()

const { isVi } = useI18n()
const { saveSkill } = usePortfolio()

useModalEffects(toRef(props, 'open'), () => emit('close'))

const saving = ref(false)
const errorText = ref('')

const form = reactive({
  id: '',
  titleVi: '',
  titleEn: '',
  descriptionVi: '',
  descriptionEn: '',
  accent: 'emerald' as SkillGroup['accent'],
  items: '',
  sortOrder: undefined as number | undefined,
})

watch(
  () => [props.open, props.skill] as const,
  ([open, skill]) => {
    if (!open) return
    errorText.value = ''
    if (skill) {
      form.id = skill.id
      form.titleVi = skill.title.vi
      form.titleEn = skill.title.en
      form.descriptionVi = skill.description.vi
      form.descriptionEn = skill.description.en
      form.accent = skill.accent
      form.items = skill.items.join(', ')
      form.sortOrder = skill.sortOrder
    } else {
      form.id = ''
      form.titleVi = ''
      form.titleEn = ''
      form.descriptionVi = ''
      form.descriptionEn = ''
      form.accent = 'emerald'
      form.items = ''
      form.sortOrder = undefined
    }
  },
  { immediate: true },
)

async function handleSave() {
  errorText.value = ''
  saving.value = true
  try {
    await saveSkill({
      id: form.id || undefined,
      title: { vi: form.titleVi, en: form.titleEn || form.titleVi },
      description: { vi: form.descriptionVi, en: form.descriptionEn || form.descriptionVi },
      accent: form.accent,
      items: form.items.split(',').map((s) => s.trim()).filter(Boolean),
      sortOrder: form.sortOrder,
    })
    emit('close')
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : 'Failed to save skill group'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="no-print fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-label="Edit Skill Group"
      @click.self="emit('close')"
    >
      <div class="w-full max-w-xl rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <h2 class="text-base font-bold text-slate-950">
            {{
              skill
                ? isVi
                  ? 'Sửa nhóm kỹ năng'
                  : 'Edit Skill Group'
                : isVi
                  ? 'Thêm nhóm kỹ năng'
                  : 'Add Skill Group'
            }}
          </h2>
          <button type="button" class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100" @click="emit('close')">
            <X class="h-4 w-4" />
          </button>
        </div>

        <form class="space-y-4 p-6 text-sm" @submit.prevent="handleSave">
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Tên nhóm (VI)</label>
              <input v-model="form.titleVi" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Title (EN)</label>
              <input v-model="form.titleEn" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Mô tả (VI)</label>
              <textarea v-model="form.descriptionVi" rows="2" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Description (EN)</label>
              <textarea v-model="form.descriptionEn" rows="2" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Màu nhấn</label>
              <select v-model="form.accent" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm">
                <option value="emerald">Emerald</option>
                <option value="indigo">Indigo</option>
                <option value="amber">Amber</option>
                <option value="sky">Sky</option>
              </select>
            </div>
            <div class="sm:col-span-2">
              <label class="mb-1 block text-xs font-semibold text-slate-700">Kỹ năng (cách nhau bằng dấu phẩy)</label>
              <input v-model="form.items" required placeholder="Java, Spring Boot, Docker" class="w-full rounded-xl border border-slate-200 px-3 py-2 font-mono text-xs" />
            </div>
          </div>

          <p v-if="errorText" class="rounded-xl bg-rose-50 px-3 py-2 text-xs font-medium text-rose-700">
            {{ errorText }}
          </p>

          <div class="flex justify-end gap-2 border-t border-slate-200 pt-4">
            <button type="button" class="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100" @click="emit('close')">
              {{ isVi ? 'Hủy' : 'Cancel' }}
            </button>
            <button type="submit" :disabled="saving" class="rounded-xl bg-slate-900 px-5 py-2 text-xs font-semibold text-white hover:bg-slate-800 disabled:opacity-60">
              {{ saving ? (isVi ? 'Đang lưu...' : 'Saving...') : (isVi ? 'Lưu nhóm' : 'Save group') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
