<script setup lang="ts">
import { computed, toRef } from 'vue'
import { Download, Globe, Mail, MapPin, Phone, Printer, X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'
import type { LocalizedText } from '@/types/profile'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const { t, tList, isVi } = useI18n()
const { portfolio } = usePortfolio()

useModalEffects(toRef(props, 'open'), () => emit('close'))

const hasExperiences = computed(() => portfolio.value.experiences.length > 0)
const languagesList = computed(() => tList(portfolio.value.languages))
const featuredProjects = computed(() =>
  portfolio.value.projects.slice(0, hasExperiences.value ? 3 : 4),
)

function formatPeriod(period: string | LocalizedText) {
  return typeof period === 'string' ? period : t(period)
}

function handlePrintPdf() {
  window.print()
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="print-only-sheet fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-950/60 p-4 backdrop-blur-xs sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-label="CV Preview"
      @click.self="emit('close')"
    >
      <div class="w-full max-w-3xl rounded-3xl border border-slate-200 bg-white shadow-2xl print:rounded-none print:border-none print:shadow-none">
        <!-- Modal Action Toolbar -->
        <div
          class="no-print sticky top-0 z-10 flex flex-wrap items-center justify-between gap-3 rounded-t-3xl border-b border-slate-200 bg-white/95 px-6 py-4 backdrop-blur-sm"
        >
          <div>
            <h2 class="text-base font-bold text-slate-950">
              {{ isVi ? 'Xem trước CV / Resume' : 'CV / Resume Preview' }}
            </h2>
            <p class="text-xs text-slate-500">
              {{
                isVi
                  ? 'Bấm "Lưu PDF" để xuất trực tiếp bản A4 sạch sẽ từ trình duyệt.'
                  : 'Click "Save PDF" to export a clean A4 PDF directly from your browser.'
              }}
            </p>
          </div>

          <div class="flex items-center gap-2">
            <a
              v-if="portfolio.cvPdfUrl"
              :href="portfolio.cvPdfUrl"
              download
              class="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100"
            >
              <Download class="h-3.5 w-3.5" />
              <span>{{ isVi ? 'Tải file PDF' : 'Download PDF' }}</span>
            </a>

            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-3.5 py-2 text-xs font-semibold text-white hover:bg-slate-800"
              @click="handlePrintPdf"
            >
              <Printer class="h-3.5 w-3.5" />
              <span>{{ isVi ? 'In / Lưu PDF' : 'Print / Save PDF' }}</span>
            </button>

            <button
              type="button"
              class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
              aria-label="Close CV preview"
              @click="emit('close')"
            >
              <X class="h-4 w-4" />
            </button>
          </div>
        </div>

        <!-- Printable A4 Resume Sheet -->
        <div class="p-6 text-slate-800 sm:p-10 print:p-0">
          <!-- Header -->
          <div class="border-b border-slate-200 pb-5">
            <h1 class="text-2xl font-extrabold tracking-tight text-slate-950 sm:text-3xl">
              {{ portfolio.name }}
            </h1>
            <p class="mt-1 text-base font-semibold text-emerald-700">
              {{ t(portfolio.role) }}
            </p>

            <div class="mt-3 flex flex-wrap gap-x-5 gap-y-1.5 text-xs text-slate-600">
              <span class="inline-flex items-center gap-1.5">
                <MapPin class="h-3.5 w-3.5 text-slate-400" />
                {{ t(portfolio.location) }}
              </span>
              <span v-if="portfolio.email" class="inline-flex items-center gap-1.5">
                <Mail class="h-3.5 w-3.5 text-slate-400" />
                {{ portfolio.email }}
              </span>
              <span v-if="portfolio.phone" class="inline-flex items-center gap-1.5">
                <Phone class="h-3.5 w-3.5 text-slate-400" />
                {{ portfolio.phone }}
              </span>
              <a
                v-for="social in portfolio.socials"
                :key="`${social.id}-${social.url}`"
                :href="social.url"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1.5 hover:text-slate-900"
              >
                <Globe class="h-3.5 w-3.5 text-slate-400" />
                {{ social.handle }}
              </a>
            </div>
          </div>

          <!-- Summary -->
          <div class="mt-5">
            <h3 class="font-mono text-xs font-bold uppercase tracking-wider text-slate-500">
              {{ isVi ? 'Tóm tắt' : 'Summary' }}
            </h3>
            <p class="mt-2 text-sm leading-relaxed text-slate-700">
              {{ t(portfolio.bio) }}
            </p>
          </div>

          <!-- Experience -->
          <div v-if="hasExperiences" class="mt-6">
            <h3 class="font-mono text-xs font-bold uppercase tracking-wider text-slate-500">
              {{ isVi ? 'Kinh nghiệm làm việc' : 'Work Experience' }}
            </h3>
            <div class="mt-3 space-y-5">
              <div v-for="exp in portfolio.experiences" :key="exp.id">
                <div class="flex flex-wrap items-baseline justify-between gap-2">
                  <h4 class="text-sm font-bold text-slate-950">
                    {{ t(exp.role) }} · <span class="text-emerald-700">{{ exp.company }}</span>
                  </h4>
                  <span class="font-mono text-xs text-slate-500">{{ t(exp.period) }}</span>
                </div>
                <p class="mt-1 text-xs text-slate-600">{{ t(exp.summary) }}</p>
                <ul class="mt-2 list-disc space-y-1 pl-4 text-xs leading-relaxed text-slate-700">
                  <li v-for="(item, idx) in tList(exp.achievements)" :key="idx">
                    {{ item }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <!-- Skills -->
          <div class="mt-6">
            <h3 class="font-mono text-xs font-bold uppercase tracking-wider text-slate-500">
              {{ isVi ? 'Kỹ năng chuyên môn' : 'Technical Skills' }}
            </h3>
            <div class="mt-2.5 grid grid-cols-1 gap-2 sm:grid-cols-2">
              <div
                v-for="group in portfolio.skills"
                :key="group.id"
                class="rounded-xl border border-slate-200/80 bg-slate-50/60 px-3.5 py-2.5 text-xs"
              >
                <span class="font-bold text-slate-900">{{ t(group.title) }}: </span>
                <span class="text-slate-700">{{ group.items.join(', ') }}</span>
              </div>
            </div>
          </div>

          <!-- Projects -->
          <div class="mt-6">
            <h3 class="font-mono text-xs font-bold uppercase tracking-wider text-slate-500">
              {{ isVi ? 'Dự án nổi bật' : 'Selected Projects' }}
            </h3>
            <div class="mt-2.5 space-y-2.5">
              <div
                v-for="proj in featuredProjects"
                :key="proj.id"
                class="text-xs leading-relaxed"
              >
                <span class="font-bold text-slate-950">{{ proj.title }}</span>
                <span class="text-slate-500"> ({{ proj.stack.join(', ') }})</span>
                — <span class="text-slate-700">{{ t(proj.tagline) }}</span>
              </div>
            </div>
          </div>

          <!-- Education & Languages -->
          <div
            class="mt-6 grid grid-cols-1 gap-4 border-t border-slate-200 pt-5"
            :class="{ 'sm:grid-cols-2': languagesList.length > 0 }"
          >
            <div>
              <h3 class="font-mono text-xs font-bold uppercase tracking-wider text-slate-500">
                {{ isVi ? 'Học vấn' : 'Education' }}
              </h3>
              <div v-for="edu in portfolio.education" :key="edu.id" class="mt-2 text-xs">
                <p class="font-bold text-slate-900">{{ edu.school }}</p>
                <p class="text-slate-700">
                  {{ t(edu.degree) }} ({{ formatPeriod(edu.period) }})
                  <span v-if="edu.gpa"> · GPA: {{ edu.gpa }}</span>
                </p>
                <p v-if="t(edu.details)" class="mt-0.5 text-slate-500">{{ t(edu.details) }}</p>
              </div>
            </div>

            <div v-if="languagesList.length > 0">
              <h3 class="font-mono text-xs font-bold uppercase tracking-wider text-slate-500">
                {{ isVi ? 'Ngoại ngữ' : 'Languages' }}
              </h3>
              <ul class="mt-2 space-y-1 text-xs text-slate-700">
                <li v-for="(lang, idx) in languagesList" :key="idx">
                  {{ lang }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
