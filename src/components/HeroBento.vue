<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import {
  ArrowUpRight,
  Camera,
  Check,
  Clock,
  Copy,
  FileText,
  Mail,
  MapPin,
  Pencil,
  Plus,
  SlidersHorizontal,
  Sparkles,
  Trash2,
  Upload,
  X,
} from '@lucide/vue'
import SocialIcon from '@/components/SocialIcon.vue'
import ProfileEditModal from '@/components/admin/ProfileEditModal.vue'
import { useI18n } from '@/composables/useI18n'
import { usePortfolio } from '@/composables/usePortfolio'
import { portfolioApi } from '@/services/api'
import type { HighlightMetric, LocalizedText, SocialLink } from '@/types/profile'

const emit = defineEmits<{
  openCv: []
}>()

const { t, isVi } = useI18n()
const { portfolio, editMode, updateProfile } = usePortfolio()
const copied = ref(false)
const editProfileOpen = ref(false)
const savingField = ref(false)
const uploadingAvatar = ref(false)
const uploadingCv = ref(false)
const inlineError = ref('')

const avatarInputRef = ref<HTMLInputElement | null>(null)
const cvInputRef = ref<HTMLInputElement | null>(null)

type InlineFieldKey =
  | 'availability'
  | 'handle'
  | 'name'
  | 'role'
  | 'headline'
  | 'bio'
  | 'location'
  | 'currentFocus'
  | 'email'
  | null

const editingField = ref<InlineFieldKey>(null)
const inlineDraft = reactive({
  vi: '',
  en: '',
  single: '',
  extra: '',
})

const editingMetricIdx = ref<number | null>(null)
const metricDraft = reactive({
  mode: 'custom' as 'custom' | '@projects' | '@skills' | '@experiences',
  value: '',
  labelVi: '',
  labelEn: '',
})

const editingSocialIdx = ref<number | null>(null)
const socialDraft = reactive<SocialLink>({
  id: 'github',
  label: 'GitHub',
  handle: '',
  url: '',
})

const resolvedMetrics = computed(() =>
  portfolio.value.metrics.map((m) => {
    let displayValue = m.value
    let isAuto = false
    if (m.value === '@projects') {
      displayValue = String(portfolio.value.projects.length)
      isAuto = true
    } else if (m.value === '@skills') {
      displayValue = String(portfolio.value.skills.length)
      isAuto = true
    } else if (m.value === '@experiences') {
      displayValue = String(portfolio.value.experiences.length)
      isAuto = true
    }
    return { ...m, displayValue, isAuto }
  }),
)

function buildProfilePayload(
  overrides: Partial<Omit<typeof portfolio.value, 'skills' | 'experiences' | 'education' | 'projects'>> = {},
) {
  const p = portfolio.value
  return {
    name: p.name,
    initials: p.initials,
    avatarUrl: p.avatarUrl,
    handle: p.handle,
    role: p.role,
    availability: p.availability,
    location: p.location,
    timezone: p.timezone,
    email: p.email,
    phone: p.phone,
    cvPdfUrl: p.cvPdfUrl,
    headline: p.headline,
    bio: p.bio,
    currentFocus: p.currentFocus,
    metrics: p.metrics,
    socials: p.socials,
    languages: p.languages,
    isDraft: Boolean(p.isDraft),
    ...overrides,
  }
}

function startInlineEdit(field: InlineFieldKey) {
  if (!editMode.value || !field) return
  inlineError.value = ''
  editingMetricIdx.value = null
  editingSocialIdx.value = null
  editingField.value = field
  const p = portfolio.value

  if (field === 'availability' || field === 'role' || field === 'headline' || field === 'bio' || field === 'currentFocus') {
    const loc: LocalizedText = p[field]
    inlineDraft.vi = loc.vi
    inlineDraft.en = loc.en
  } else if (field === 'location') {
    inlineDraft.vi = p.location.vi
    inlineDraft.en = p.location.en
    inlineDraft.extra = p.timezone
  } else if (field === 'handle') {
    inlineDraft.single = p.handle
  } else if (field === 'name') {
    inlineDraft.single = p.name
    inlineDraft.extra = p.initials
  } else if (field === 'email') {
    inlineDraft.single = p.email
  }
}

function cancelInlineEdit() {
  editingField.value = null
  inlineError.value = ''
}

async function saveInlineEdit() {
  const field = editingField.value
  if (!field) return
  savingField.value = true
  inlineError.value = ''
  try {
    if (field === 'availability' || field === 'role' || field === 'headline' || field === 'bio' || field === 'currentFocus') {
      await updateProfile(
        buildProfilePayload({
          [field]: {
            vi: inlineDraft.vi.trim(),
            en: (inlineDraft.en || inlineDraft.vi).trim(),
          },
        }),
      )
    } else if (field === 'location') {
      await updateProfile(
        buildProfilePayload({
          location: {
            vi: inlineDraft.vi.trim(),
            en: (inlineDraft.en || inlineDraft.vi).trim(),
          },
          timezone: inlineDraft.extra.trim() || 'UTC+7',
        }),
      )
    } else if (field === 'handle') {
      await updateProfile(buildProfilePayload({ handle: inlineDraft.single.trim() }))
    } else if (field === 'name') {
      await updateProfile(
        buildProfilePayload({
          name: inlineDraft.single.trim(),
          initials: inlineDraft.extra.trim() || portfolio.value.initials,
        }),
      )
    } else if (field === 'email') {
      await updateProfile(buildProfilePayload({ email: inlineDraft.single.trim() }))
    }
    editingField.value = null
  } catch (err) {
    inlineError.value = err instanceof Error ? err.message : 'Failed to save'
  } finally {
    savingField.value = false
  }
}

async function handleAvatarUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingAvatar.value = true
  inlineError.value = ''
  try {
    const uploaded = await portfolioApi.uploadFile(file)
    await updateProfile(buildProfilePayload({ avatarUrl: uploaded.url }))
  } catch (err) {
    inlineError.value = err instanceof Error ? err.message : 'Upload avatar failed'
  } finally {
    uploadingAvatar.value = false
    input.value = ''
  }
}

async function removeAvatar() {
  savingField.value = true
  try {
    await updateProfile(buildProfilePayload({ avatarUrl: undefined }))
  } finally {
    savingField.value = false
  }
}

async function handleCvUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingCv.value = true
  inlineError.value = ''
  try {
    const uploaded = await portfolioApi.uploadFile(file)
    await updateProfile(buildProfilePayload({ cvPdfUrl: uploaded.url }))
  } catch (err) {
    inlineError.value = err instanceof Error ? err.message : 'Upload CV failed'
  } finally {
    uploadingCv.value = false
    input.value = ''
  }
}

function startEditMetric(idx: number) {
  editingField.value = null
  editingSocialIdx.value = null
  editingMetricIdx.value = idx
  const existing = portfolio.value.metrics[idx]
  if (existing) {
    if (existing.value === '@projects' || existing.value === '@skills' || existing.value === '@experiences') {
      metricDraft.mode = existing.value
      metricDraft.value = ''
    } else {
      metricDraft.mode = 'custom'
      metricDraft.value = existing.value
    }
    metricDraft.labelVi = existing.label.vi
    metricDraft.labelEn = existing.label.en
  } else {
    metricDraft.mode = 'custom'
    metricDraft.value = ''
    metricDraft.labelVi = ''
    metricDraft.labelEn = ''
  }
}

async function saveMetric() {
  if (editingMetricIdx.value === null) return
  const finalValue = metricDraft.mode === 'custom' ? metricDraft.value.trim() : metricDraft.mode
  if (!finalValue || (!metricDraft.labelVi.trim() && !metricDraft.labelEn.trim())) return

  const nextMetrics: HighlightMetric[] = structuredClone(portfolio.value.metrics)
  const item: HighlightMetric = {
    value: finalValue,
    label: {
      vi: metricDraft.labelVi.trim() || metricDraft.labelEn.trim(),
      en: metricDraft.labelEn.trim() || metricDraft.labelVi.trim(),
    },
  }
  if (editingMetricIdx.value >= nextMetrics.length) {
    nextMetrics.push(item)
  } else {
    nextMetrics[editingMetricIdx.value] = item
  }

  savingField.value = true
  try {
    await updateProfile(buildProfilePayload({ metrics: nextMetrics }))
    editingMetricIdx.value = null
  } finally {
    savingField.value = false
  }
}

async function deleteMetric(idx: number) {
  const nextMetrics = portfolio.value.metrics.filter((_, i) => i !== idx)
  savingField.value = true
  try {
    await updateProfile(buildProfilePayload({ metrics: nextMetrics }))
    if (editingMetricIdx.value === idx) editingMetricIdx.value = null
  } finally {
    savingField.value = false
  }
}

function startEditSocial(idx: number) {
  editingField.value = null
  editingMetricIdx.value = null
  editingSocialIdx.value = idx
  const existing = portfolio.value.socials[idx]
  if (existing) {
    socialDraft.id = existing.id
    socialDraft.label = existing.label
    socialDraft.handle = existing.handle
    socialDraft.url = existing.url
  } else {
    socialDraft.id = 'github'
    socialDraft.label = 'GitHub'
    socialDraft.handle = ''
    socialDraft.url = ''
  }
}

async function saveSocial() {
  if (editingSocialIdx.value === null || !socialDraft.label.trim() || !socialDraft.url.trim()) return
  const nextSocials: SocialLink[] = structuredClone(portfolio.value.socials)
  const item: SocialLink = {
    id: socialDraft.id,
    label: socialDraft.label.trim(),
    handle: socialDraft.handle.trim() || socialDraft.url.trim(),
    url: socialDraft.url.trim(),
  }
  if (editingSocialIdx.value >= nextSocials.length) {
    nextSocials.push(item)
  } else {
    nextSocials[editingSocialIdx.value] = item
  }

  savingField.value = true
  try {
    await updateProfile(buildProfilePayload({ socials: nextSocials }))
    editingSocialIdx.value = null
  } finally {
    savingField.value = false
  }
}

async function deleteSocial(idx: number) {
  const nextSocials = portfolio.value.socials.filter((_, i) => i !== idx)
  savingField.value = true
  try {
    await updateProfile(buildProfilePayload({ socials: nextSocials }))
    if (editingSocialIdx.value === idx) editingSocialIdx.value = null
  } finally {
    savingField.value = false
  }
}

async function copyEmail() {
  if (!portfolio.value.email) return
  try {
    await navigator.clipboard.writeText(portfolio.value.email)
    copied.value = true
    window.setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    window.location.href = `mailto:${portfolio.value.email}`
  }
}
</script>

<template>
  <section id="about" class="pt-8 pb-6 sm:pt-12 sm:pb-8">
    <div v-if="inlineError" class="mb-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-xs font-medium text-rose-700">
      {{ inlineError }}
    </div>

    <div class="grid grid-cols-1 gap-4 lg:grid-cols-12">
      <!-- Main Intro Bento Card -->
      <div class="bento-card relative flex flex-col justify-between overflow-hidden lg:col-span-8 lg:p-8">
        <!-- Subtle ambient accent -->
        <div
          class="pointer-events-none absolute -right-24 -top-24 h-64 w-64 rounded-full bg-emerald-400/10 blur-3xl"
          aria-hidden="true"
        />

        <div class="relative">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <!-- Availability Badge (Inline Editable) -->
            <div v-if="editingField === 'availability'" class="flex flex-wrap items-center gap-1.5 rounded-2xl border border-emerald-300 bg-emerald-50/80 p-1.5">
              <input
                v-model="inlineDraft.vi"
                placeholder="Trạng thái (VI)"
                class="w-40 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs"
                @keydown.enter="saveInlineEdit"
                @keydown.escape="cancelInlineEdit"
              />
              <input
                v-model="inlineDraft.en"
                placeholder="Status (EN)"
                class="w-40 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs"
                @keydown.enter="saveInlineEdit"
                @keydown.escape="cancelInlineEdit"
              />
              <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 p-1.5 text-white hover:bg-emerald-700" @click="saveInlineEdit">
                <Check class="h-3.5 w-3.5" />
              </button>
              <button type="button" class="rounded-lg bg-white p-1.5 text-slate-500 hover:bg-slate-100" @click="cancelInlineEdit">
                <X class="h-3.5 w-3.5" />
              </button>
            </div>
            <button
              v-else
              type="button"
              class="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700 transition-all"
              :class="editMode ? 'cursor-pointer ring-1 ring-transparent hover:border-emerald-400 hover:ring-emerald-300' : 'cursor-default'"
              :title="editMode ? (isVi ? 'Bấm để sửa trạng thái' : 'Click to edit status') : undefined"
              @click="startInlineEdit('availability')"
            >
              <span class="relative flex h-2 w-2">
                <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                <span class="relative inline-flex h-2 w-2 rounded-full bg-emerald-500" />
              </span>
              <span>{{ t(portfolio.availability) }}</span>
              <Pencil v-if="editMode" class="h-3 w-3 opacity-60" />
            </button>

            <div class="flex items-center gap-2">
              <!-- Handle (Inline Editable) -->
              <div v-if="editingField === 'handle'" class="flex items-center gap-1 rounded-xl border border-emerald-300 bg-white p-1">
                <input
                  v-model="inlineDraft.single"
                  class="w-28 rounded-lg border border-slate-200 px-2 py-0.5 font-mono text-xs"
                  @keydown.enter="saveInlineEdit"
                  @keydown.escape="cancelInlineEdit"
                />
                <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 p-1 text-white" @click="saveInlineEdit">
                  <Check class="h-3 w-3" />
                </button>
                <button type="button" class="rounded-lg p-1 text-slate-500 hover:bg-slate-100" @click="cancelInlineEdit">
                  <X class="h-3 w-3" />
                </button>
              </div>
              <button
                v-else
                type="button"
                class="inline-flex items-center gap-1 rounded-lg px-1.5 py-0.5 font-mono text-xs text-slate-400 transition-colors"
                :class="editMode ? 'cursor-pointer hover:bg-emerald-50 hover:text-emerald-700' : 'cursor-default'"
                @click="startInlineEdit('handle')"
              >
                <span>{{ portfolio.handle }}</span>
                <Pencil v-if="editMode" class="h-2.5 w-2.5 opacity-60" />
              </button>

              <button
                v-if="editMode"
                type="button"
                class="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 shadow-2xs hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-800"
                @click="editProfileOpen = true"
              >
                <SlidersHorizontal class="h-3.5 w-3.5" />
                <span>{{ isVi ? 'Tùy chỉnh đầy đủ' : 'Full settings' }}</span>
              </button>
            </div>
          </div>

          <!-- Avatar + Name + Role -->
          <div class="mt-6 flex items-center gap-4 sm:gap-5">
            <div class="relative shrink-0">
              <div
                class="flex h-18 w-18 items-center justify-center overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-700 font-mono text-xl font-bold text-white shadow-md ring-2 ring-emerald-500/20 sm:h-22 sm:w-22 sm:text-2xl"
              >
                <img
                  v-if="portfolio.avatarUrl"
                  :src="portfolio.avatarUrl"
                  :alt="portfolio.name"
                  class="h-full w-full object-cover"
                />
                <template v-else>{{ portfolio.initials }}</template>
              </div>

              <input
                ref="avatarInputRef"
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
                class="hidden"
                @change="handleAvatarUpload"
              />

              <button
                v-if="editMode"
                type="button"
                :disabled="uploadingAvatar"
                class="absolute -bottom-1.5 -right-1.5 flex h-7 w-7 items-center justify-center rounded-full border border-white bg-emerald-600 text-white shadow-md transition-transform hover:scale-105 hover:bg-emerald-700 disabled:opacity-60"
                :title="isVi ? 'Tải ảnh đại diện từ máy' : 'Upload avatar from device'"
                @click="avatarInputRef?.click()"
              >
                <Camera class="h-3.5 w-3.5" />
              </button>
              <button
                v-if="editMode && portfolio.avatarUrl"
                type="button"
                class="absolute -top-1.5 -right-1.5 flex h-5 w-5 items-center justify-center rounded-full border border-white bg-rose-500 text-white shadow-sm hover:bg-rose-600"
                :title="isVi ? 'Xóa ảnh đại diện' : 'Remove avatar'"
                @click="removeAvatar"
              >
                <X class="h-3 w-3" />
              </button>
            </div>

            <div class="min-w-0 flex-1">
              <!-- Name (Inline Editable) -->
              <div v-if="editingField === 'name'" class="flex flex-wrap items-center gap-1.5">
                <input
                  v-model="inlineDraft.single"
                  placeholder="Họ tên"
                  class="min-w-[200px] flex-1 rounded-xl border border-emerald-300 bg-white px-3 py-1.5 text-lg font-bold text-slate-950"
                  @keydown.enter="saveInlineEdit"
                  @keydown.escape="cancelInlineEdit"
                />
                <input
                  v-model="inlineDraft.extra"
                  placeholder="Initials"
                  class="w-16 rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-center font-mono text-sm"
                  @keydown.enter="saveInlineEdit"
                  @keydown.escape="cancelInlineEdit"
                />
                <button type="button" :disabled="savingField" class="rounded-xl bg-emerald-600 p-2 text-white" @click="saveInlineEdit">
                  <Check class="h-4 w-4" />
                </button>
                <button type="button" class="rounded-xl border border-slate-200 bg-white p-2 text-slate-500" @click="cancelInlineEdit">
                  <X class="h-4 w-4" />
                </button>
              </div>
              <h1
                v-else
                class="inline-flex items-center gap-2 rounded-xl text-2xl font-extrabold tracking-tight text-slate-950 sm:text-4xl"
                :class="editMode ? 'cursor-pointer px-1.5 -mx-1.5 hover:bg-emerald-50/70' : ''"
                @click="startInlineEdit('name')"
              >
                <span>{{ portfolio.name }}</span>
                <Pencil v-if="editMode" class="h-4 w-4 text-emerald-600 opacity-60" />
              </h1>

              <!-- Role (Inline Editable) -->
              <div v-if="editingField === 'role'" class="mt-1.5 flex flex-wrap items-center gap-1.5">
                <input
                  v-model="inlineDraft.vi"
                  placeholder="Chức danh (VI)"
                  class="w-48 rounded-xl border border-emerald-300 bg-white px-2.5 py-1 text-sm"
                  @keydown.enter="saveInlineEdit"
                  @keydown.escape="cancelInlineEdit"
                />
                <input
                  v-model="inlineDraft.en"
                  placeholder="Role (EN)"
                  class="w-48 rounded-xl border border-slate-200 bg-white px-2.5 py-1 text-sm"
                  @keydown.enter="saveInlineEdit"
                  @keydown.escape="cancelInlineEdit"
                />
                <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 p-1.5 text-white" @click="saveInlineEdit">
                  <Check class="h-3.5 w-3.5" />
                </button>
                <button type="button" class="rounded-lg border border-slate-200 bg-white p-1.5 text-slate-500" @click="cancelInlineEdit">
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
              <p
                v-else
                class="mt-1 inline-flex items-center gap-1.5 rounded-lg text-sm font-semibold text-emerald-700 sm:text-base"
                :class="editMode ? 'cursor-pointer px-1.5 -mx-1.5 hover:bg-emerald-50/70' : ''"
                @click="startInlineEdit('role')"
              >
                <span>{{ t(portfolio.role) }}</span>
                <Pencil v-if="editMode" class="h-3.5 w-3.5 opacity-60" />
              </p>
            </div>
          </div>

          <!-- Headline (Inline Editable) -->
          <div v-if="editingField === 'headline'" class="mt-5 space-y-2 rounded-2xl border border-emerald-200 bg-emerald-50/40 p-3">
            <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
              <input v-model="inlineDraft.vi" placeholder="Headline (VI)" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold" />
              <input v-model="inlineDraft.en" placeholder="Headline (EN)" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold" />
            </div>
            <div class="flex justify-end gap-1.5">
              <button type="button" class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-slate-600" @click="cancelInlineEdit">
                {{ isVi ? 'Hủy' : 'Cancel' }}
              </button>
              <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 px-3 py-1 text-xs font-semibold text-white" @click="saveInlineEdit">
                {{ isVi ? 'Lưu' : 'Save' }}
              </button>
            </div>
          </div>
          <p
            v-else
            class="mt-6 rounded-xl text-lg font-semibold leading-snug text-slate-900 sm:text-xl"
            :class="editMode ? 'cursor-pointer p-2 -mx-2 hover:bg-emerald-50/60' : ''"
            @click="startInlineEdit('headline')"
          >
            {{ t(portfolio.headline) }}
            <Pencil v-if="editMode" class="ml-1.5 inline h-3.5 w-3.5 text-emerald-600 opacity-60" />
          </p>

          <!-- Bio (Inline Editable) -->
          <div v-if="editingField === 'bio'" class="mt-2 space-y-2 rounded-2xl border border-emerald-200 bg-emerald-50/40 p-3">
            <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
              <textarea v-model="inlineDraft.vi" rows="3" placeholder="Giới thiệu (VI)" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" />
              <textarea v-model="inlineDraft.en" rows="3" placeholder="Bio (EN)" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" />
            </div>
            <div class="flex justify-end gap-1.5">
              <button type="button" class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-slate-600" @click="cancelInlineEdit">
                {{ isVi ? 'Hủy' : 'Cancel' }}
              </button>
              <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 px-3 py-1 text-xs font-semibold text-white" @click="saveInlineEdit">
                {{ isVi ? 'Lưu' : 'Save' }}
              </button>
            </div>
          </div>
          <p
            v-else
            class="mt-3 max-w-2xl rounded-xl text-sm leading-relaxed text-slate-600 sm:text-base"
            :class="editMode ? 'cursor-pointer p-2 -mx-2 hover:bg-emerald-50/60' : ''"
            @click="startInlineEdit('bio')"
          >
            {{ t(portfolio.bio) }}
            <Pencil v-if="editMode" class="ml-1.5 inline h-3.5 w-3.5 text-emerald-600 opacity-60" />
          </p>
        </div>

        <!-- CTA Row -->
        <div class="relative mt-8 flex flex-wrap items-center gap-3">
          <div class="inline-flex items-center gap-1.5">
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-slate-800"
              @click="emit('openCv')"
            >
              <FileText class="h-4 w-4" />
              <span>{{ isVi ? 'Xem & tải CV' : 'View & Download CV' }}</span>
            </button>

            <input
              ref="cvInputRef"
              type="file"
              accept="application/pdf"
              class="hidden"
              @change="handleCvUpload"
            />
            <button
              v-if="editMode"
              type="button"
              :disabled="uploadingCv"
              class="inline-flex items-center gap-1.5 rounded-2xl border border-emerald-200 bg-emerald-50 px-3.5 py-3 text-xs font-semibold text-emerald-800 hover:bg-emerald-100 disabled:opacity-60"
              :title="isVi ? 'Tải file CV PDF từ máy' : 'Upload CV PDF from device'"
              @click="cvInputRef?.click()"
            >
              <Upload class="h-3.5 w-3.5" />
              <span>{{ uploadingCv ? (isVi ? 'Đang tải...' : 'Uploading...') : (isVi ? 'Tải CV (.pdf)' : 'Upload CV') }}</span>
            </button>
          </div>

          <a
            href="#contact"
            class="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-5 py-3 text-sm font-semibold text-slate-800 transition-colors hover:border-slate-300 hover:bg-slate-100"
          >
            <Mail class="h-4 w-4" />
            <span>{{ isVi ? 'Liên hệ ngay' : 'Get in touch' }}</span>
          </a>

          <!-- Email Copy / Inline Edit -->
          <div v-if="editingField === 'email'" class="flex items-center gap-1 rounded-2xl border border-emerald-300 bg-white p-1.5">
            <input
              v-model="inlineDraft.single"
              type="email"
              placeholder="you@gmail.com"
              class="w-48 rounded-lg border border-slate-200 px-2.5 py-1 font-mono text-xs"
              @keydown.enter="saveInlineEdit"
              @keydown.escape="cancelInlineEdit"
            />
            <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 p-1.5 text-white" @click="saveInlineEdit">
              <Check class="h-3.5 w-3.5" />
            </button>
            <button type="button" class="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100" @click="cancelInlineEdit">
              <X class="h-3.5 w-3.5" />
            </button>
          </div>
          <button
            v-else-if="portfolio.email || editMode"
            type="button"
            class="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-600 transition-colors hover:border-slate-300 hover:text-slate-900"
            @click="editMode ? startInlineEdit('email') : copyEmail()"
          >
            <Pencil v-if="editMode" class="h-3.5 w-3.5 text-emerald-600" />
            <Check v-else-if="copied" class="h-4 w-4 text-emerald-600" />
            <Copy v-else class="h-4 w-4" />
            <span class="font-mono text-xs">
              {{
                editMode
                  ? portfolio.email || (isVi ? 'Thêm email...' : 'Add email...')
                  : copied
                    ? isVi
                      ? 'Đã sao chép email!'
                      : 'Email copied!'
                    : portfolio.email
              }}
            </span>
          </button>
        </div>
      </div>

      <!-- Right Side Bento Stack -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:col-span-4 lg:grid-cols-1">
        <!-- Metrics Bento Card -->
        <div
          v-if="portfolio.metrics.length || editMode"
          class="bento-card relative flex flex-col justify-between overflow-hidden border-slate-800 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 text-white"
        >
          <div
            class="pointer-events-none absolute -bottom-16 -left-16 h-44 w-44 rounded-full bg-emerald-500/15 blur-2xl"
            aria-hidden="true"
          />

          <div class="relative">
            <!-- Location & Timezone (Inline Editable) -->
            <div v-if="editingField === 'location'" class="space-y-2 rounded-2xl border border-slate-700 bg-slate-800/80 p-2.5 text-xs">
              <div class="grid grid-cols-2 gap-1.5">
                <input v-model="inlineDraft.vi" placeholder="Địa điểm (VI)" class="rounded-lg border border-slate-600 bg-slate-900 px-2 py-1 text-white" />
                <input v-model="inlineDraft.en" placeholder="Location (EN)" class="rounded-lg border border-slate-600 bg-slate-900 px-2 py-1 text-white" />
              </div>
              <div class="flex items-center gap-1.5">
                <input v-model="inlineDraft.extra" placeholder="UTC+7" class="w-24 rounded-lg border border-slate-600 bg-slate-900 px-2 py-1 font-mono text-white" />
                <button type="button" :disabled="savingField" class="ml-auto rounded-lg bg-emerald-500 px-2.5 py-1 font-semibold text-slate-950" @click="saveInlineEdit">
                  {{ isVi ? 'Lưu' : 'Save' }}
                </button>
                <button type="button" class="rounded-lg bg-slate-700 px-2.5 py-1 text-slate-200" @click="cancelInlineEdit">
                  {{ isVi ? 'Hủy' : 'Cancel' }}
                </button>
              </div>
            </div>
            <div v-else class="flex items-center justify-between text-xs text-slate-400">
              <button
                type="button"
                class="inline-flex items-center gap-1.5 rounded-lg px-1 -mx-1 py-0.5 transition-colors"
                :class="editMode ? 'cursor-pointer hover:bg-slate-800 hover:text-white' : 'cursor-default'"
                @click="startInlineEdit('location')"
              >
                <MapPin class="h-3.5 w-3.5 text-emerald-400" />
                <span>{{ t(portfolio.location) }}</span>
                <Pencil v-if="editMode" class="h-3 w-3 opacity-60" />
              </button>
              <span class="inline-flex items-center gap-1 font-mono">
                <Clock class="h-3.5 w-3.5" />
                {{ portfolio.timezone }}
              </span>
            </div>

            <!-- Metric Grid -->
            <div class="mt-4 grid grid-cols-2 gap-2.5">
              <div
                v-for="(metric, idx) in resolvedMetrics"
                :key="`${metric.value}-${idx}`"
                class="group relative rounded-2xl border border-slate-800/90 bg-slate-800/60 p-3.5 transition-colors hover:border-slate-700"
              >
                <div class="flex items-baseline gap-1.5">
                  <div class="text-2xl font-extrabold tracking-tight text-white">
                    {{ metric.displayValue }}
                  </div>
                  <span
                    v-if="editMode && metric.isAuto"
                    class="rounded-full bg-emerald-500/20 px-1.5 py-0.5 font-mono text-[9px] font-semibold text-emerald-300"
                  >
                    AUTO
                  </span>
                </div>
                <div class="mt-0.5 text-xs text-slate-400">
                  {{ t(metric.label) }}
                </div>

                <div v-if="editMode" class="absolute top-2 right-2 flex items-center gap-1 opacity-90">
                  <button
                    type="button"
                    class="rounded-lg bg-slate-900/80 p-1 text-slate-300 hover:text-white"
                    :title="isVi ? 'Sửa chỉ số' : 'Edit metric'"
                    @click="startEditMetric(idx)"
                  >
                    <Pencil class="h-3 w-3" />
                  </button>
                  <button
                    type="button"
                    class="rounded-lg bg-slate-900/80 p-1 text-rose-400 hover:text-rose-300"
                    :title="isVi ? 'Xóa' : 'Delete'"
                    @click="deleteMetric(idx)"
                  >
                    <Trash2 class="h-3 w-3" />
                  </button>
                </div>
              </div>

              <button
                v-if="editMode"
                type="button"
                class="flex min-h-[74px] flex-col items-center justify-center gap-1 rounded-2xl border border-dashed border-slate-700 bg-slate-900/40 p-3 text-xs font-semibold text-slate-400 transition-colors hover:border-emerald-500/60 hover:text-emerald-300"
                @click="startEditMetric(portfolio.metrics.length)"
              >
                <Plus class="h-4 w-4" />
                <span>{{ isVi ? 'Thêm chỉ số' : 'Add metric' }}</span>
              </button>
            </div>

            <!-- Inline Metric Editor -->
            <div v-if="editingMetricIdx !== null" class="mt-3 space-y-2 rounded-2xl border border-emerald-500/40 bg-slate-900/95 p-3 text-xs">
              <div class="flex items-center justify-between">
                <span class="font-semibold text-emerald-300">{{ isVi ? 'Chỉnh sửa chỉ số' : 'Edit Metric' }}</span>
                <button type="button" class="text-slate-400 hover:text-white" @click="editingMetricIdx = null">
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
              <select v-model="metricDraft.mode" class="w-full rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-1.5 text-white">
                <option value="custom">{{ isVi ? 'Nhập giá trị thủ công' : 'Custom value' }}</option>
                <option value="@projects">{{ isVi ? `Tự động đếm Dự án (${portfolio.projects.length})` : `Auto-count Projects (${portfolio.projects.length})` }}</option>
                <option value="@skills">{{ isVi ? `Tự động đếm Nhóm kỹ năng (${portfolio.skills.length})` : `Auto-count Skill Groups (${portfolio.skills.length})` }}</option>
                <option value="@experiences">{{ isVi ? `Tự động đếm Kinh nghiệm (${portfolio.experiences.length})` : `Auto-count Experiences (${portfolio.experiences.length})` }}</option>
              </select>
              <input
                v-if="metricDraft.mode === 'custom'"
                v-model="metricDraft.value"
                :placeholder="isVi ? 'Giá trị (VD: 3.61, Java)' : 'Value (e.g. 3.61, Java)'"
                class="w-full rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-1.5 text-white"
              />
              <div class="grid grid-cols-2 gap-1.5">
                <input v-model="metricDraft.labelVi" placeholder="Nhãn (VI)" class="rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-1.5 text-white" />
                <input v-model="metricDraft.labelEn" placeholder="Label (EN)" class="rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-1.5 text-white" />
              </div>
              <div class="flex justify-end gap-1.5">
                <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-500 px-3 py-1 font-semibold text-slate-950 hover:bg-emerald-400" @click="saveMetric">
                  {{ isVi ? 'Lưu chỉ số' : 'Save' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Focus & Socials Bento Card -->
        <div class="bento-card flex flex-col justify-between">
          <div>
            <div class="flex items-center justify-between">
              <div class="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-slate-400">
                <Sparkles class="h-3.5 w-3.5 text-amber-500" />
                <span>{{ isVi ? 'Đang tập trung' : 'Current focus' }}</span>
              </div>
            </div>

            <div v-if="editingField === 'currentFocus'" class="mt-2 space-y-2 rounded-2xl border border-emerald-200 bg-emerald-50/40 p-2.5">
              <textarea v-model="inlineDraft.vi" rows="2" placeholder="Đang tập trung (VI)" class="w-full rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs" />
              <textarea v-model="inlineDraft.en" rows="2" placeholder="Current focus (EN)" class="w-full rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs" />
              <div class="flex justify-end gap-1.5">
                <button type="button" class="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs text-slate-600" @click="cancelInlineEdit">
                  {{ isVi ? 'Hủy' : 'Cancel' }}
                </button>
                <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 px-2.5 py-1 text-xs font-semibold text-white" @click="saveInlineEdit">
                  {{ isVi ? 'Lưu' : 'Save' }}
                </button>
              </div>
            </div>
            <p
              v-else
              class="mt-2 rounded-xl text-sm leading-relaxed text-slate-700"
              :class="editMode ? 'cursor-pointer p-1.5 -mx-1.5 hover:bg-emerald-50/60' : ''"
              @click="startInlineEdit('currentFocus')"
            >
              {{ t(portfolio.currentFocus) }}
              <Pencil v-if="editMode" class="ml-1 inline h-3 w-3 text-emerald-600 opacity-60" />
            </p>
          </div>

          <div class="mt-5 flex flex-col gap-2">
            <div
              v-for="(social, idx) in portfolio.socials"
              :key="`${social.id}-${social.url}-${idx}`"
              class="flex items-center gap-1.5"
            >
              <a
                :href="social.url"
                target="_blank"
                rel="noopener noreferrer"
                class="group flex min-w-0 flex-1 items-center justify-between gap-3 rounded-xl border border-slate-200/80 bg-slate-50/70 px-3.5 py-2.5 text-sm transition-colors hover:border-slate-300 hover:bg-slate-100"
              >
                <span class="flex shrink-0 items-center gap-2.5 font-medium text-slate-800">
                  <SocialIcon :id="social.id" class="h-4 w-4 text-slate-500" />
                  {{ social.label }}
                </span>
                <span class="flex min-w-0 items-center gap-1 font-mono text-xs text-slate-500 group-hover:text-slate-800">
                  <span class="truncate">{{ social.handle }}</span>
                  <ArrowUpRight class="h-3.5 w-3.5 shrink-0 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
                </span>
              </a>

              <div v-if="editMode" class="flex shrink-0 items-center gap-1">
                <button type="button" class="rounded-lg border border-slate-200 bg-white p-2 text-slate-600 hover:bg-slate-100" @click="startEditSocial(idx)">
                  <Pencil class="h-3.5 w-3.5" />
                </button>
                <button type="button" class="rounded-lg border border-rose-200 bg-rose-50 p-2 text-rose-600 hover:bg-rose-100" @click="deleteSocial(idx)">
                  <Trash2 class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>

            <button
              v-if="editMode"
              type="button"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl border border-dashed border-slate-300 py-2 text-xs font-semibold text-slate-600 hover:border-emerald-400 hover:bg-emerald-50/50 hover:text-emerald-700"
              @click="startEditSocial(portfolio.socials.length)"
            >
              <Plus class="h-3.5 w-3.5" />
              <span>{{ isVi ? 'Thêm liên kết' : 'Add social link' }}</span>
            </button>

            <!-- Inline Social Editor -->
            <div v-if="editingSocialIdx !== null" class="mt-1 space-y-2 rounded-2xl border border-emerald-200 bg-emerald-50/40 p-3 text-xs">
              <div class="flex items-center justify-between">
                <span class="font-semibold text-emerald-800">{{ isVi ? 'Liên kết mạng xã hội' : 'Social Link' }}</span>
                <button type="button" class="text-slate-500" @click="editingSocialIdx = null">
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
              <div class="grid grid-cols-2 gap-1.5">
                <select v-model="socialDraft.id" class="rounded-lg border border-slate-200 bg-white px-2 py-1.5">
                  <option value="github">GitHub</option>
                  <option value="linkedin">LinkedIn</option>
                  <option value="facebook">Facebook</option>
                  <option value="x">X</option>
                  <option value="website">Website</option>
                </select>
                <input v-model="socialDraft.label" placeholder="Tên hiển thị" class="rounded-lg border border-slate-200 bg-white px-2.5 py-1.5" />
              </div>
              <input v-model="socialDraft.handle" placeholder="Handle (VD: github.com/quang-star)" class="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5" />
              <input v-model="socialDraft.url" placeholder="https://..." class="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5" />
              <div class="flex justify-end">
                <button type="button" :disabled="savingField" class="rounded-lg bg-emerald-600 px-3 py-1 font-semibold text-white" @click="saveSocial">
                  {{ isVi ? 'Lưu' : 'Save' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ProfileEditModal :open="editProfileOpen" @close="editProfileOpen = false" />
  </section>
</template>
