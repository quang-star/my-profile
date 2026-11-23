<script setup lang="ts">
import { reactive, ref, toRef, watch } from 'vue'
import { X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'
import type { ExperienceItem } from '@/types/profile'

const props = defineProps<{
  open: boolean
  item: ExperienceItem | null
}>()

const emit = defineEmits<{
  close: []
}>()

const { isVi } = useI18n()
const { saveExperience } = usePortfolio()

useModalEffects(toRef(props, 'open'), () => emit('close'))

const saving = ref(false)
const errorText = ref('')

const form = reactive({
  id: '',
  company: '',
  roleVi: '',
  roleEn: '',
  periodVi: '',
  periodEn: '',
  locationVi: '',
  locationEn: '',
  typeVi: 'Thực tập / Toàn thời gian',
  typeEn: 'Internship / Full-time',
  summaryVi: '',
  summaryEn: '',
  achievementsVi: '',
  achievementsEn: '',
  stack: '',
  sortOrder: undefined as number | undefined,
})

watch(
  () => [props.open, props.item] as const,
  ([open, exp]) => {
    if (!open) return
    errorText.value = ''
    if (exp) {
      form.id = exp.id
      form.company = exp.company
      form.roleVi = exp.role.vi
      form.roleEn = exp.role.en
      form.periodVi = exp.period.vi
      form.periodEn = exp.period.en
      form.locationVi = exp.location.vi
      form.locationEn = exp.location.en
      form.typeVi = exp.type.vi
      form.typeEn = exp.type.en
      form.summaryVi = exp.summary.vi
      form.summaryEn = exp.summary.en
      form.achievementsVi = exp.achievements.vi.join('\n')
      form.achievementsEn = exp.achievements.en.join('\n')
      form.stack = exp.stack.join(', ')
      form.sortOrder = exp.sortOrder
    } else {
      form.id = ''
      form.company = ''
      form.roleVi = ''
      form.roleEn = ''
      form.periodVi = ''
      form.periodEn = ''
      form.locationVi = 'Hà Nội'
      form.locationEn = 'Hanoi'
      form.typeVi = 'Toàn thời gian'
      form.typeEn = 'Full-time'
      form.summaryVi = ''
      form.summaryEn = ''
      form.achievementsVi = ''
      form.achievementsEn = ''
      form.stack = ''
      form.sortOrder = undefined
    }
  },
  { immediate: true },
)

async function handleSave() {
  errorText.value = ''
  saving.value = true
  try {
    const achVi = form.achievementsVi.split('\n').map((s) => s.trim()).filter(Boolean)
    const achEn = form.achievementsEn.split('\n').map((s) => s.trim()).filter(Boolean)
    await saveExperience({
      id: form.id || undefined,
      company: form.company,
      role: { vi: form.roleVi, en: form.roleEn || form.roleVi },
      period: { vi: form.periodVi, en: form.periodEn || form.periodVi },
      location: { vi: form.locationVi, en: form.locationEn || form.locationVi },
      type: { vi: form.typeVi, en: form.typeEn || form.typeVi },
      summary: { vi: form.summaryVi, en: form.summaryEn || form.summaryVi },
      achievements: { vi: achVi, en: achEn.length ? achEn : achVi },
      stack: form.stack.split(',').map((s) => s.trim()).filter(Boolean),
      sortOrder: form.sortOrder,
    })
    emit('close')
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : 'Failed to save experience'
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
      aria-label="Edit Experience"
      @click.self="emit('close')"
    >
      <div class="flex max-h-[90vh] w-full max-w-2xl flex-col rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <h2 class="text-base font-bold text-slate-950">
            {{ item ? (isVi ? 'Sửa kinh nghiệm' : 'Edit Experience') : (isVi ? 'Thêm kinh nghiệm làm việc' : 'Add Experience') }}
          </h2>
          <button type="button" class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100" @click="emit('close')">
            <X class="h-4 w-4" />
          </button>
        </div>

        <form class="flex-1 space-y-4 overflow-y-auto p-6 text-sm" @submit.prevent="handleSave">
          <div>
            <label class="mb-1 block text-xs font-semibold text-slate-700">{{ isVi ? 'Công ty / Tổ chức' : 'Company' }}</label>
            <input v-model="form.company" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Vị trí (VI)</label>
              <input v-model="form.roleVi" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Role (EN)</label>
              <input v-model="form.roleEn" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Thời gian (VI)</label>
              <input v-model="form.periodVi" required placeholder="06/2025 — Hiện tại" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Period (EN)</label>
              <input v-model="form.periodEn" placeholder="Jun 2025 — Present" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Địa điểm (VI)</label>
              <input v-model="form.locationVi" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Hình thức (VI)</label>
              <input v-model="form.typeVi" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Tóm tắt (VI)</label>
              <textarea v-model="form.summaryVi" rows="2" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Summary (EN)</label>
              <textarea v-model="form.summaryEn" rows="2" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Thành tựu VI (mỗi dòng 1 ý)</label>
              <textarea v-model="form.achievementsVi" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Achievements EN (one per line)</label>
              <textarea v-model="form.achievementsEn" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
          </div>

          <div>
            <label class="mb-1 block text-xs font-semibold text-slate-700">Tech stack (cách nhau bằng dấu phẩy)</label>
            <input v-model="form.stack" placeholder="Java, Spring Boot, PostgreSQL" class="w-full rounded-xl border border-slate-200 px-3 py-2 font-mono text-xs" />
          </div>

          <p v-if="errorText" class="rounded-xl bg-rose-50 px-3 py-2 text-xs font-medium text-rose-700">
            {{ errorText }}
          </p>

          <div class="flex justify-end gap-2 border-t border-slate-200 pt-4">
            <button type="button" class="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100" @click="emit('close')">
              {{ isVi ? 'Hủy' : 'Cancel' }}
            </button>
            <button type="submit" :disabled="saving" class="rounded-xl bg-slate-900 px-5 py-2 text-xs font-semibold text-white hover:bg-slate-800 disabled:opacity-60">
              {{ saving ? (isVi ? 'Đang lưu...' : 'Saving...') : (isVi ? 'Lưu kinh nghiệm' : 'Save experience') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
