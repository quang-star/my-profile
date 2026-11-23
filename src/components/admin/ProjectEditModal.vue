<script setup lang="ts">
import { reactive, ref, toRef, watch } from 'vue'
import { X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'
import type { ProjectItem } from '@/types/profile'

const props = defineProps<{
  open: boolean
  project: ProjectItem | null
}>()

const emit = defineEmits<{
  close: []
}>()

const { isVi } = useI18n()
const { saveProject } = usePortfolio()

useModalEffects(toRef(props, 'open'), () => emit('close'))

const saving = ref(false)
const errorText = ref('')

const form = reactive({
  id: '',
  title: '',
  category: 'fullstack' as ProjectItem['category'],
  featured: false,
  year: String(new Date().getFullYear()),
  taglineVi: '',
  taglineEn: '',
  descriptionVi: '',
  descriptionEn: '',
  highlightsVi: '',
  highlightsEn: '',
  stack: '',
  liveUrl: '',
  repoUrl: '',
  secondaryRepoUrl: '',
  sortOrder: undefined as number | undefined,
})

watch(
  () => [props.open, props.project] as const,
  ([open, proj]) => {
    if (!open) return
    errorText.value = ''
    if (proj) {
      form.id = proj.id
      form.title = proj.title
      form.category = proj.category
      form.featured = Boolean(proj.featured)
      form.year = proj.year
      form.taglineVi = proj.tagline.vi
      form.taglineEn = proj.tagline.en
      form.descriptionVi = proj.description.vi
      form.descriptionEn = proj.description.en
      form.highlightsVi = proj.highlights.vi.join('\n')
      form.highlightsEn = proj.highlights.en.join('\n')
      form.stack = proj.stack.join(', ')
      form.liveUrl = proj.liveUrl ?? ''
      form.repoUrl = proj.repoUrl ?? ''
      form.secondaryRepoUrl = proj.secondaryRepoUrl ?? ''
      form.sortOrder = proj.sortOrder
    } else {
      form.id = ''
      form.title = ''
      form.category = 'fullstack'
      form.featured = false
      form.year = String(new Date().getFullYear())
      form.taglineVi = ''
      form.taglineEn = ''
      form.descriptionVi = ''
      form.descriptionEn = ''
      form.highlightsVi = ''
      form.highlightsEn = ''
      form.stack = ''
      form.liveUrl = ''
      form.repoUrl = ''
      form.secondaryRepoUrl = ''
      form.sortOrder = undefined
    }
  },
  { immediate: true },
)

async function handleSave() {
  errorText.value = ''
  saving.value = true
  try {
    const highlightsVi = form.highlightsVi.split('\n').map((s) => s.trim()).filter(Boolean)
    const highlightsEn = form.highlightsEn.split('\n').map((s) => s.trim()).filter(Boolean)
    await saveProject({
      id: form.id || undefined,
      title: form.title,
      category: form.category,
      featured: form.featured,
      year: form.year,
      tagline: { vi: form.taglineVi, en: form.taglineEn || form.taglineVi },
      description: { vi: form.descriptionVi, en: form.descriptionEn || form.descriptionVi },
      highlights: {
        vi: highlightsVi,
        en: highlightsEn.length ? highlightsEn : highlightsVi,
      },
      stack: form.stack.split(',').map((s) => s.trim()).filter(Boolean),
      liveUrl: form.liveUrl || undefined,
      repoUrl: form.repoUrl || undefined,
      secondaryRepoUrl: form.secondaryRepoUrl || undefined,
      sortOrder: form.sortOrder,
    })
    emit('close')
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : 'Failed to save project'
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
      aria-label="Edit Project"
      @click.self="emit('close')"
    >
      <div class="flex max-h-[90vh] w-full max-w-2xl flex-col rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <h2 class="text-base font-bold text-slate-950">
            {{
              project
                ? isVi
                  ? 'Chỉnh sửa dự án'
                  : 'Edit Project'
                : isVi
                  ? 'Thêm dự án mới'
                  : 'Add New Project'
            }}
          </h2>
          <button type="button" class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100" @click="emit('close')">
            <X class="h-4 w-4" />
          </button>
        </div>

        <form class="flex-1 space-y-4 overflow-y-auto p-6 text-sm" @submit.prevent="handleSave">
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-12">
            <div class="sm:col-span-6">
              <label class="mb-1 block text-xs font-semibold text-slate-700">{{ isVi ? 'Tên dự án' : 'Project title' }}</label>
              <input v-model="form.title" required placeholder="Spring Boot E-Commerce API" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div class="sm:col-span-3">
              <label class="mb-1 block text-xs font-semibold text-slate-700">{{ isVi ? 'Nhóm' : 'Category' }}</label>
              <select v-model="form.category" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm">
                <option value="fullstack">Fullstack</option>
                <option value="frontend">Frontend</option>
                <option value="tooling">Tooling</option>
                <option value="work">{{ isVi ? 'Việc đã làm' : 'Work' }}</option>
              </select>
            </div>
            <div class="sm:col-span-3">
              <label class="mb-1 block text-xs font-semibold text-slate-700">{{ isVi ? 'Thời gian' : 'Timeframe' }}</label>
              <input
                v-model="form.year"
                required
                maxlength="64"
                :placeholder="isVi ? '03/2026 - 05/2026' : 'Mar - May 2026'"
                class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm"
              />
            </div>
          </div>

          <label class="flex items-center gap-2 text-xs font-semibold text-emerald-700">
            <input v-model="form.featured" type="checkbox" class="h-4 w-4 rounded border-slate-300" />
            <span>{{ isVi ? 'Đánh dấu là dự án Chủ lực (Featured)' : 'Mark as Featured project' }}</span>
          </label>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Tagline ngắn (VI)</label>
              <input v-model="form.taglineVi" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Tagline (EN)</label>
              <input v-model="form.taglineEn" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Mô tả chi tiết (VI)</label>
              <textarea v-model="form.descriptionVi" rows="3" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Description (EN)</label>
              <textarea v-model="form.descriptionEn" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Điểm nổi bật VI (mỗi dòng 1 ý)</label>
              <textarea v-model="form.highlightsVi" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Highlights EN (one per line)</label>
              <textarea v-model="form.highlightsEn" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
            </div>
          </div>

          <div>
            <label class="mb-1 block text-xs font-semibold text-slate-700">Tech stack (cách nhau bằng dấu phẩy)</label>
            <input v-model="form.stack" placeholder="Java, Spring Boot, PostgreSQL, Docker" class="w-full rounded-xl border border-slate-200 px-3 py-2 font-mono text-xs" />
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">GitHub URL</label>
              <input v-model="form.repoUrl" placeholder="https://github.com/..." class="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">GitHub Backend (nếu tách)</label>
              <input v-model="form.secondaryRepoUrl" placeholder="https://github.com/..." class="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-semibold text-slate-700">Live Demo URL</label>
              <input v-model="form.liveUrl" placeholder="https://..." class="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs" />
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
              {{ saving ? (isVi ? 'Đang lưu...' : 'Saving...') : (isVi ? 'Lưu dự án' : 'Save project') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
