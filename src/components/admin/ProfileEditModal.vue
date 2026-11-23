<script setup lang="ts">
import { reactive, ref, toRef, watch } from 'vue'
import { Plus, Trash2, Upload, X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'
import { portfolioApi } from '@/services/api'
import type { HighlightMetric, SocialLink } from '@/types/profile'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const { isVi } = useI18n()
const { portfolio, updateProfile } = usePortfolio()

useModalEffects(toRef(props, 'open'), () => emit('close'))

type TabKey = 'basic' | 'content' | 'metrics'
const activeTab = ref<TabKey>('basic')

const saving = ref(false)
const uploadingAvatar = ref(false)
const uploadingCv = ref(false)
const errorText = ref('')

const avatarInputRef = ref<HTMLInputElement | null>(null)
const cvInputRef = ref<HTMLInputElement | null>(null)

const form = reactive({
  name: '',
  initials: '',
  avatarUrl: '',
  handle: '',
  roleVi: '',
  roleEn: '',
  availabilityVi: '',
  availabilityEn: '',
  locationVi: '',
  locationEn: '',
  timezone: 'UTC+7',
  email: '',
  phone: '',
  cvPdfUrl: '',
  headlineVi: '',
  headlineEn: '',
  bioVi: '',
  bioEn: '',
  currentFocusVi: '',
  currentFocusEn: '',
  languagesVi: '',
  languagesEn: '',
  isDraft: false,
  metrics: [] as HighlightMetric[],
  socials: [] as SocialLink[],
})

watch(
  () => props.open,
  (open) => {
    if (!open) return
    const p = portfolio.value
    activeTab.value = 'basic'
    errorText.value = ''
    form.name = p.name
    form.initials = p.initials
    form.avatarUrl = p.avatarUrl ?? ''
    form.handle = p.handle
    form.roleVi = p.role.vi
    form.roleEn = p.role.en
    form.availabilityVi = p.availability.vi
    form.availabilityEn = p.availability.en
    form.locationVi = p.location.vi
    form.locationEn = p.location.en
    form.timezone = p.timezone
    form.email = p.email
    form.phone = p.phone ?? ''
    form.cvPdfUrl = p.cvPdfUrl ?? ''
    form.headlineVi = p.headline.vi
    form.headlineEn = p.headline.en
    form.bioVi = p.bio.vi
    form.bioEn = p.bio.en
    form.currentFocusVi = p.currentFocus.vi
    form.currentFocusEn = p.currentFocus.en
    form.languagesVi = p.languages.vi.join('\n')
    form.languagesEn = p.languages.en.join('\n')
    form.isDraft = Boolean(p.isDraft)
    form.metrics = structuredClone(p.metrics)
    form.socials = structuredClone(p.socials)
  },
  { immediate: true },
)

function addMetric() {
  form.metrics.push({ value: '', label: { vi: '', en: '' } })
}

function addSocial() {
  form.socials.push({ id: 'linkedin', label: 'LinkedIn', handle: '', url: '' })
}

async function uploadAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingAvatar.value = true
  errorText.value = ''
  try {
    const res = await portfolioApi.uploadFile(file)
    form.avatarUrl = res.url
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : 'Upload avatar failed'
  } finally {
    uploadingAvatar.value = false
    input.value = ''
  }
}

async function uploadCv(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingCv.value = true
  errorText.value = ''
  try {
    const res = await portfolioApi.uploadFile(file)
    form.cvPdfUrl = res.url
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : 'Upload CV failed'
  } finally {
    uploadingCv.value = false
    input.value = ''
  }
}

async function handleSave() {
  errorText.value = ''
  saving.value = true
  try {
    await updateProfile({
      name: form.name,
      initials: form.initials,
      avatarUrl: form.avatarUrl || undefined,
      handle: form.handle,
      role: { vi: form.roleVi, en: form.roleEn || form.roleVi },
      availability: { vi: form.availabilityVi, en: form.availabilityEn || form.availabilityVi },
      location: { vi: form.locationVi, en: form.locationEn || form.locationVi },
      timezone: form.timezone,
      email: form.email,
      phone: form.phone || undefined,
      cvPdfUrl: form.cvPdfUrl || undefined,
      headline: { vi: form.headlineVi, en: form.headlineEn || form.headlineVi },
      bio: { vi: form.bioVi, en: form.bioEn || form.bioVi },
      currentFocus: { vi: form.currentFocusVi, en: form.currentFocusEn || form.currentFocusVi },
      metrics: form.metrics.filter((m) => m.value.trim() && (m.label.vi.trim() || m.label.en.trim())),
      socials: form.socials.filter((s) => s.label.trim() && s.url.trim()),
      languages: {
        vi: form.languagesVi.split('\n').map((s) => s.trim()).filter(Boolean),
        en: form.languagesEn.split('\n').map((s) => s.trim()).filter(Boolean),
      },
      isDraft: form.isDraft,
    })
    emit('close')
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : 'Failed to update profile'
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
      aria-label="Edit Profile"
      @click.self="emit('close')"
    >
      <div class="flex max-h-[90vh] w-full max-w-3xl flex-col rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <h2 class="text-base font-bold text-slate-950">
            {{ isVi ? 'Tùy chỉnh hồ sơ & Hero' : 'Customize Profile & Hero' }}
          </h2>
          <button
            type="button"
            class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100"
            @click="emit('close')"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <!-- Tabs -->
        <div class="flex gap-1.5 border-b border-slate-200 bg-slate-50/80 px-6 py-2.5 text-xs font-semibold">
          <button
            type="button"
            class="rounded-xl px-3.5 py-1.5 transition-colors"
            :class="activeTab === 'basic' ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-200/70'"
            @click="activeTab = 'basic'"
          >
            {{ isVi ? '1. Cơ bản & Media' : '1. Basic & Media' }}
          </button>
          <button
            type="button"
            class="rounded-xl px-3.5 py-1.5 transition-colors"
            :class="activeTab === 'content' ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-200/70'"
            @click="activeTab = 'content'"
          >
            {{ isVi ? '2. Nội dung (VI / EN)' : '2. Content (VI / EN)' }}
          </button>
          <button
            type="button"
            class="rounded-xl px-3.5 py-1.5 transition-colors"
            :class="activeTab === 'metrics' ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-200/70'"
            @click="activeTab = 'metrics'"
          >
            {{ isVi ? '3. Chỉ số & Mạng xã hội' : '3. Metrics & Socials' }}
          </button>
        </div>

        <form class="flex-1 space-y-4 overflow-y-auto p-6 text-sm" @submit.prevent="handleSave">
          <!-- TAB 1: Basic & Media -->
          <div v-show="activeTab === 'basic'" class="space-y-4">
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">{{ isVi ? 'Họ tên' : 'Full name' }}</label>
                <input v-model="form.name" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Initials</label>
                <input v-model="form.initials" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Handle</label>
                <input v-model="form.handle" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
            </div>

            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Email</label>
                <input v-model="form.email" type="email" placeholder="you@gmail.com" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">{{ isVi ? 'Số điện thoại' : 'Phone' }}</label>
                <input v-model="form.phone" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
            </div>

            <!-- Avatar Upload + URL -->
            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <label class="mb-1.5 block text-xs font-semibold text-slate-700">
                {{ isVi ? 'Ảnh đại diện (Avatar)' : 'Avatar Image' }}
              </label>
              <div class="flex flex-wrap items-center gap-2">
                <input v-model="form.avatarUrl" placeholder="/api/files/... hoặc https://..." class="min-w-[200px] flex-1 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs" />
                <input ref="avatarInputRef" type="file" accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml" class="hidden" @change="uploadAvatar" />
                <button
                  type="button"
                  :disabled="uploadingAvatar"
                  class="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-60"
                  @click="avatarInputRef?.click()"
                >
                  <Upload class="h-3.5 w-3.5" />
                  <span>{{ uploadingAvatar ? (isVi ? 'Đang tải...' : 'Uploading...') : (isVi ? 'Chọn ảnh từ máy' : 'Upload image') }}</span>
                </button>
              </div>
            </div>

            <!-- CV PDF Upload + URL -->
            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <label class="mb-1.5 block text-xs font-semibold text-slate-700">
                {{ isVi ? 'File CV (PDF)' : 'CV File (PDF)' }}
              </label>
              <div class="flex flex-wrap items-center gap-2">
                <input v-model="form.cvPdfUrl" placeholder="/api/files/... hoặc https://..." class="min-w-[200px] flex-1 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs" />
                <input ref="cvInputRef" type="file" accept="application/pdf" class="hidden" @change="uploadCv" />
                <button
                  type="button"
                  :disabled="uploadingCv"
                  class="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-3.5 py-2 text-xs font-semibold text-white hover:bg-slate-800 disabled:opacity-60"
                  @click="cvInputRef?.click()"
                >
                  <Upload class="h-3.5 w-3.5" />
                  <span>{{ uploadingCv ? (isVi ? 'Đang tải...' : 'Uploading...') : (isVi ? 'Chọn PDF từ máy' : 'Upload PDF') }}</span>
                </button>
              </div>
            </div>

            <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Địa điểm (VI)</label>
                <input v-model="form.locationVi" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Location (EN)</label>
                <input v-model="form.locationEn" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Timezone</label>
                <input v-model="form.timezone" required class="w-full rounded-xl border border-slate-200 px-3 py-2 font-mono text-sm" />
              </div>
            </div>

            <label class="flex items-center gap-2 text-xs font-medium text-slate-700">
              <input v-model="form.isDraft" type="checkbox" class="h-4 w-4 rounded border-slate-300" />
              <span>{{ isVi ? 'Hiển thị thanh nhắc bản nháp ở đầu trang' : 'Show draft notice banner at top' }}</span>
            </label>
          </div>

          <!-- TAB 2: Content VI/EN -->
          <div v-show="activeTab === 'content'" class="space-y-4">
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Chức danh (VI)</label>
                <input v-model="form.roleVi" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Role (EN)</label>
                <input v-model="form.roleEn" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Trạng thái (VI)</label>
                <input v-model="form.availabilityVi" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Availability (EN)</label>
                <input v-model="form.availabilityEn" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Headline (VI)</label>
                <textarea v-model="form.headlineVi" rows="2" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Headline (EN)</label>
                <textarea v-model="form.headlineEn" rows="2" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Bio (VI)</label>
                <textarea v-model="form.bioVi" rows="3" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Bio (EN)</label>
                <textarea v-model="form.bioEn" rows="3" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Đang tập trung (VI)</label>
                <textarea v-model="form.currentFocusVi" rows="2" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Current Focus (EN)</label>
                <textarea v-model="form.currentFocusEn" rows="2" required class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Ngoại ngữ (VI — mỗi dòng 1 mục)</label>
                <textarea v-model="form.languagesVi" rows="2" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-xs font-semibold text-slate-700">Languages (EN — one per line)</label>
                <textarea v-model="form.languagesEn" rows="2" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
              </div>
            </div>
          </div>

          <!-- TAB 3: Metrics & Socials -->
          <div v-show="activeTab === 'metrics'" class="space-y-4">
            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div class="flex items-center justify-between">
                <div>
                  <span class="text-xs font-bold uppercase tracking-wider text-slate-500">
                    {{ isVi ? 'Thẻ số liệu nổi bật' : 'Highlight Metrics' }}
                  </span>
                  <p class="mt-0.5 text-[11px] text-slate-500">
                    {{ isVi ? 'Gõ @projects, @skills, @experiences để tự động đếm theo dữ liệu thực tế.' : 'Use @projects, @skills, or @experiences to auto-count.' }}
                  </p>
                </div>
                <button type="button" class="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700" @click="addMetric">
                  <Plus class="h-3.5 w-3.5" /> {{ isVi ? 'Thêm' : 'Add' }}
                </button>
              </div>
              <div class="mt-3 space-y-2">
                <div v-for="(metric, idx) in form.metrics" :key="idx" class="grid grid-cols-12 gap-2">
                  <input v-model="metric.value" placeholder="@projects hoặc 3.61" class="col-span-3 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 font-mono text-xs" />
                  <input v-model="metric.label.vi" placeholder="Nhãn VI" class="col-span-4 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs" />
                  <input v-model="metric.label.en" placeholder="Label EN" class="col-span-4 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs" />
                  <button type="button" class="col-span-1 flex items-center justify-center rounded-lg text-rose-500 hover:bg-rose-50" @click="form.metrics.splice(idx, 1)">
                    <Trash2 class="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div class="flex items-center justify-between">
                <span class="text-xs font-bold uppercase tracking-wider text-slate-500">
                  {{ isVi ? 'Liên kết mạng xã hội' : 'Social Links' }}
                </span>
                <button type="button" class="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700" @click="addSocial">
                  <Plus class="h-3.5 w-3.5" /> {{ isVi ? 'Thêm' : 'Add' }}
                </button>
              </div>
              <div class="mt-3 space-y-2">
                <div v-for="(social, idx) in form.socials" :key="idx" class="grid grid-cols-12 gap-2">
                  <select v-model="social.id" class="col-span-2 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs">
                    <option value="github">GitHub</option>
                    <option value="linkedin">LinkedIn</option>
                    <option value="facebook">Facebook</option>
                    <option value="x">X</option>
                    <option value="website">Website</option>
                  </select>
                  <input v-model="social.label" placeholder="Label" class="col-span-2 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs" />
                  <input v-model="social.handle" placeholder="github.com/..." class="col-span-3 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs" />
                  <input v-model="social.url" placeholder="https://..." class="col-span-4 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs" />
                  <button type="button" class="col-span-1 flex items-center justify-center rounded-lg text-rose-500 hover:bg-rose-50" @click="form.socials.splice(idx, 1)">
                    <Trash2 class="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
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
              {{ saving ? (isVi ? 'Đang lưu...' : 'Saving...') : (isVi ? 'Lưu thay đổi' : 'Save changes') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
