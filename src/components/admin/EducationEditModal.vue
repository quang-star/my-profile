<script setup lang="ts">
import { reactive, ref, toRef, watch } from 'vue'
import { X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'
import type { EducationItem } from '@/types/profile'

const props = defineProps<{
  open: boolean
  item: EducationItem | null
}>()

const emit = defineEmits<{
  close: []
}>()

const { isVi } = useI18n()
const { saveEducation } = usePortfolio()

useModalEffects(toRef(props, 'open'), () => emit('close'))

const saving = ref(false)
const errorText = ref('')

const form = reactive({
  id: '',
  school: '',
  degreeVi: '',
  degreeEn: '',
  periodVi: '',
  periodEn: '',
  gpa: '',
  detailsVi: '',
  detailsEn: '',
  sortOrder: undefined as number | undefined,
})

watch(
  () => [props.open, props.item] as const,
  ([open, edu]) => {
    if (!open) return
    errorText.value = ''
    if (edu) {
      form.id = edu.id
      form.school = edu.school
      form.degreeVi = edu.degree.vi
      form.degreeEn = edu.degree.en
      form.periodVi = typeof edu.period === 'string' ? edu.period : edu.period.vi
      form.periodEn = typeof edu.period === 'string' ? edu.period : edu.period.en
      form.gpa = edu.gpa ?? ''
      form.detailsVi = edu.details.vi
      form.detailsEn = edu.details.en
      form.sortOrder = edu.sortOrder
    } else {
      form.id = ''
      form.school = ''
      form.degreeVi = ''
      form.degreeEn = ''
      form.periodVi = ''
      form.periodEn = ''
      form.gpa = ''
      form.detailsVi = ''
      form.detailsEn = ''
      form.sortOrder = undefined
    }
  },
  { immediate: true },
)

async function handleSave() {
  errorText.value = ''
  saving.value = true
  try {
    await saveEducation({
      id: form.id || undefined,
      school: form.school,
      degree: { vi: form.degreeVi, en: form.degreeEn || form.degreeVi },
      period: { vi: form.periodVi, en: form.periodEn || form.periodVi },
      gpa: form.gpa || undefined,
      details: { vi: form.detailsVi, en: form.detailsEn || form.detailsVi },
      sortOrder: form.sortOrder,
    })
    emit('close')
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : 'Failed to save education'
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
      aria-label="Edit Education"
      @click.self="emit('close')"
    >
      <div class="w-full max-w-xl rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <h2 class="text-base font-bold text-slate-950">
            {{ item ? (isVi ? 'Sửa học vấn' : 'Edit Education') : (isVi ? 'Thêm học vấn' : 'Add Education') }}
          </h2>
          <button type="button" class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100" @click="emit('close')">
            <X class="h-4 w-4" />
          </button>
        </div>

        <form class="space-y-4 p-6 text-sm" @submit.prevent="handleSave">
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div class="sm:col-span-2">
              <label class="mb-1 block text-xs font-semibold text-slate-700">{{ isVi ? 'Trường' : 'School' }}</label>
              <input v-model="form.school" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">GPA</label>
              <input v-model="form.gpa" placeholder="3.61 / 4.0" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Ngành học (VI)</label>
              <input v-model="form.degreeVi" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Degree (EN)</label>
              <input v-model="form.degreeEn" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Thời gian (VI)</label>
              <input v-model="form.periodVi" required placeholder="2022 — Hiện tại" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Period (EN)</label>
              <input v-model="form.periodEn" placeholder="2022 — Present" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Chi tiết (VI)</label>
              <textarea v-model="form.detailsVi" rows="2" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Details (EN)</label>
              <textarea v-model="form.detailsEn" rows="2" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
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
              {{ saving ? (isVi ? 'Đang lưu...' : 'Saving...') : (isVi ? 'Lưu học vấn' : 'Save education') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
